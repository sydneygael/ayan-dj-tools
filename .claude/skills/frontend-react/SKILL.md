# Frontend React — Patterns du Projet

Stack : React 19, Vite, MUI v6, Zustand, React Router v7, Electron 40, i18next, Recharts, Vitest + RTL.
Répertoire : `music-tagger-ui/`

## Structure des fichiers

```
src/
├── App.tsx                   # Routes React Router v7
├── config/environment.ts     # apiUrl, wsUrl
├── types/types.ts            # Interfaces TS mirroring Java records
├── store/useAppStore.ts      # Zustand global state
├── api/                      # Fetch clients par domaine
├── hooks/                    # Custom hooks (usePlanProgress, ...)
├── utils/helpers.ts          # formatDate, extractFilename, ...
├── i18n/locales/             # en.json, fr.json
└── components/               # Un dossier par feature
electron/                     # main.ts + preload.ts (process séparé)
```

## Composant fonctionnel standard

```tsx
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import { useTranslation } from 'react-i18next';

interface MyComponentProps {
  value: string;
  label?: string;
}

export default function MyComponent({ value, label }: MyComponentProps) {
  const { t } = useTranslation();
  return (
    <Box sx={{ p: 2 }}>
      <Typography variant="subtitle2">{label ?? t('my.key')}</Typography>
      <Typography>{value}</Typography>
    </Box>
  );
}
```

## Zustand store (useAppStore)

```tsx
import { create } from 'zustand';

interface AppState {
  selectedFiles: string[];
  mode: 'PLAN' | 'MANUAL' | 'APPLY';
  setSelectedFiles: (files: string[]) => void;
  setMode: (mode: AppState['mode']) => void;
}

export const useAppStore = create<AppState>((set) => ({
  selectedFiles: [],
  mode: 'PLAN',
  setSelectedFiles: (files) => set({ selectedFiles: files }),
  setMode: (mode) => set({ mode }),
}));

// Usage dans un composant
const { selectedFiles, mode, setMode } = useAppStore();
```

## Pattern API fetch (api/*.ts)

```ts
import { environment } from '../config/environment';
import type { MyType } from '../types/types';

export async function getMyData(): Promise<MyType> {
  const res = await fetch(`${environment.apiUrl}/api/my-endpoint`);
  if (!res.ok) throw new Error(`Failed: ${res.status}`);
  return res.json();
}

export async function postMyData(body: unknown): Promise<MyType> {
  const res = await fetch(`${environment.apiUrl}/api/my-endpoint`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  if (!res.ok) throw new Error(`Failed: ${res.status}`);
  return res.json();
}
```

## Hook STOMP WebSocket (usePlanProgress)

```tsx
import { useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { environment } from '../config/environment';

export function usePlanProgress(planId: string, onEvent: (event: TagProgressEvent) => void) {
  const clientRef = useRef<Client | null>(null);

  useEffect(() => {
    if (!planId) return;
    const client = new Client({
      webSocketFactory: () => new SockJS(environment.wsUrl),
      onConnect: () => {
        client.subscribe(`/topic/plan/${planId}/progress`, (msg) => {
          onEvent(JSON.parse(msg.body));
        });
      },
    });
    client.activate();
    clientRef.current = client;
    return () => { client.deactivate(); };
  }, [planId, onEvent]);
}
```

## Electron IPC — file picker

```ts
// preload.ts — expose via contextBridge
contextBridge.exposeInMainWorld('electronAPI', {
  openFileDialog: () => ipcRenderer.invoke('open-file-dialog'),
});

// main.ts — handler
ipcMain.handle('open-file-dialog', async () => {
  const { filePaths } = await dialog.showOpenDialog({
    properties: ['openFile', 'multiSelections'],
    filters: [{ name: 'Audio', extensions: ['mp3','flac','wav','aiff','m4a','ogg'] }],
  });
  return filePaths;
});

// composant React
const files = await (window as any).electronAPI.openFileDialog();
```

## Theming MUI v6 dark mode

```tsx
// App.tsx
import { ThemeProvider, createTheme } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';

const theme = createTheme({
  palette: {
    mode: 'dark',
    primary: { main: '#00bcd4' },
    secondary: { main: '#7c4dff' },
  },
});

export default function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      {/* routes */}
    </ThemeProvider>
  );
}
```

## i18next (useTranslation)

```tsx
import { useTranslation } from 'react-i18next';

function MyComponent() {
  const { t } = useTranslation();
  return <Typography>{t('my.translation.key')}</Typography>;
}
```

Fichiers de traduction : `src/i18n/locales/en.json` et `fr.json`.
Clés imbriquées : `{ "stats": { "title": "Statistics" } }` → `t('stats.title')`.

## Recharts (charts)

```tsx
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip, Legend } from 'recharts';
import { useTheme } from '@mui/material/styles';

const COLORS = ['#00bcd4', '#7c4dff', '#ff5722'];

function MyChart({ data }: { data: { name: string; value: number }[] }) {
  const theme = useTheme();
  return (
    <ResponsiveContainer width="100%" height={280}>
      <PieChart>
        <Pie data={data} dataKey="value" nameKey="name" innerRadius={50} outerRadius={100}>
          {data.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
        </Pie>
        <Tooltip />
        <Legend wrapperStyle={{ color: theme.palette.text.primary }} />
      </PieChart>
    </ResponsiveContainer>
  );
}
```

## Tests Vitest + React Testing Library

### Configuration (vite.config.ts)

```ts
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/setupTests.ts'],
  },
});
```

### Setup (src/setupTests.ts)

```ts
import '@testing-library/jest-dom';
```

### Mocks standards

```tsx
// Mock i18next
vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}));

// Mock fetch
vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
  ok: true,
  json: async () => mockData,
}));

// Mock MUI useTheme
vi.mock('@mui/material/styles', () => ({
  useTheme: () => ({
    palette: {
      primary: { main: '#00bcd4' },
      secondary: { main: '#7c4dff' },
      text: { primary: '#000' },
      divider: '#ccc',
    },
  }),
}));

// Mock Recharts (évite les erreurs SVG/canvas en jsdom)
vi.mock('recharts', () => ({
  ResponsiveContainer: ({ children }: any) => children,
  PieChart: ({ children }: any) => <div data-testid="pie-chart">{children}</div>,
  Pie: () => null,
  Cell: () => null,
  BarChart: ({ children }: any) => <div data-testid="bar-chart">{children}</div>,
  Bar: () => null,
  XAxis: () => null,
  YAxis: () => null,
  Tooltip: () => null,
  Legend: () => null,
  RadarChart: ({ children }: any) => <div>{children}</div>,
  PolarGrid: () => null,
  PolarAngleAxis: () => null,
  PolarRadiusAxis: () => null,
  Radar: () => null,
  AreaChart: ({ children }: any) => <div>{children}</div>,
  Area: () => null,
}));
```

### Exemple de test composant

```tsx
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import MyComponent from './MyComponent';

describe('MyComponent', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ value: 42 }),
    }));
  });

  it('affiche les données après chargement', async () => {
    render(<MyComponent />);
    await waitFor(() => {
      expect(screen.getByText('42')).toBeInTheDocument();
    });
  });
});
```

## Règles du projet

- Un fichier par composant, export default.
- Props typées via `interface`, jamais `any`.
- Les appels API dans `api/`, jamais directement dans les composants.
- `useEffect` avec dépendances explicites + commentaire expliquant pourquoi.
- `useMemo` pour les transformations coûteuses dérivées du state.
- Pas de Redux — Zustand uniquement pour state global partagé.
- Les composants page (route) sont dans `components/<feature>/`, pas dans un dossier `pages/`.
