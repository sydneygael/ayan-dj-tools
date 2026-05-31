import { computed, effect, Injectable, signal } from '@angular/core';
import { OperatingMode } from './models';

type ThemeMode = 'light' | 'dark';
type Lang = 'fr' | 'en';

interface PreferencesState {
  apiBaseUrl: string;
  defaultMode: OperatingMode;
  theme: ThemeMode;
  language: Lang;
  defaultMusicDir: string;
  currentDir: string;
  selectedFiles: string[];
}

const STORAGE_KEY = 'ayan.dj.preferences';

const DEFAULT_PREFERENCES: PreferencesState = {
  apiBaseUrl: 'http://localhost:8000',
  defaultMode: 'PLAN',
  theme: 'dark',
  language: 'fr',
  defaultMusicDir: '',
  currentDir: '',
  selectedFiles: []
};

@Injectable({ providedIn: 'root' })
export class PreferencesStore {
  private readonly state = signal<PreferencesState>(this.loadState());

  readonly apiBaseUrl      = computed(() => this.state().apiBaseUrl);
  readonly defaultMode     = computed(() => this.state().defaultMode);
  readonly theme           = computed(() => this.state().theme);
  readonly language        = computed(() => this.state().language);
  readonly defaultMusicDir = computed(() => this.state().defaultMusicDir);
  readonly currentDir      = computed(() => this.state().currentDir || null);
  readonly selectedFiles   = computed(() => this.state().selectedFiles);
  readonly selectedFileCount = computed(() => this.state().selectedFiles.length);

  private readonly _currentMode = signal<OperatingMode>(this.state().defaultMode);
  readonly currentMode = this._currentMode.asReadonly();

  constructor() {
    effect(() => {
      const snapshot = this.state();
      localStorage.setItem(STORAGE_KEY, JSON.stringify(snapshot));
      document.documentElement.dataset['theme'] = snapshot.theme;
    });
  }

  setApiBaseUrl(apiBaseUrl: string): void {
    this.patch({ apiBaseUrl: apiBaseUrl.trim() || DEFAULT_PREFERENCES.apiBaseUrl });
  }

  setDefaultMode(defaultMode: OperatingMode): void {
    this.patch({ defaultMode });
  }

  setTheme(theme: ThemeMode): void {
    this.patch({ theme });
  }

  setLanguage(language: Lang): void {
    this.patch({ language });
  }

  setDefaultMusicDir(dir: string): void {
    this.patch({ defaultMusicDir: dir.trim() });
  }

  setCurrentMode(mode: OperatingMode): void {
    this._currentMode.set(mode);
    this.patch({ defaultMode: mode });
  }

  setSelectedFiles(paths: string[]): void {
    this.patch({ selectedFiles: [...paths] });
  }

  clearSelectedFiles(): void {
    this.patch({ selectedFiles: [] });
  }

  setCurrentDir(dir: string | null): void {
    this.patch({ currentDir: dir?.trim() ?? '' });
  }

  exportJson(): string {
    return JSON.stringify(this.state(), null, 2);
  }

  importJson(raw: string): void {
    const parsed = JSON.parse(raw) as Partial<PreferencesState>;
    this.state.set({
      apiBaseUrl:      parsed.apiBaseUrl      ?? DEFAULT_PREFERENCES.apiBaseUrl,
      defaultMode:     parsed.defaultMode     ?? DEFAULT_PREFERENCES.defaultMode,
      theme:           parsed.theme           ?? DEFAULT_PREFERENCES.theme,
      language:        parsed.language        ?? DEFAULT_PREFERENCES.language,
      defaultMusicDir: parsed.defaultMusicDir ?? DEFAULT_PREFERENCES.defaultMusicDir,
      currentDir:      parsed.currentDir      ?? DEFAULT_PREFERENCES.currentDir,
      selectedFiles:   parsed.selectedFiles   ?? DEFAULT_PREFERENCES.selectedFiles
    });
    this._currentMode.set(this.state().defaultMode);
  }

  private patch(partial: Partial<PreferencesState>): void {
    this.state.update((current) => ({ ...current, ...partial }));
  }

  private loadState(): PreferencesState {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return DEFAULT_PREFERENCES;

    try {
      const parsed = JSON.parse(raw) as Partial<PreferencesState>;
      return {
        apiBaseUrl:      parsed.apiBaseUrl      ?? DEFAULT_PREFERENCES.apiBaseUrl,
        defaultMode:     parsed.defaultMode     ?? DEFAULT_PREFERENCES.defaultMode,
        theme:           parsed.theme           ?? DEFAULT_PREFERENCES.theme,
        language:        parsed.language        ?? DEFAULT_PREFERENCES.language,
        defaultMusicDir: parsed.defaultMusicDir ?? DEFAULT_PREFERENCES.defaultMusicDir,
        currentDir:      parsed.currentDir      ?? DEFAULT_PREFERENCES.currentDir,
        selectedFiles:   Array.isArray(parsed.selectedFiles) ? parsed.selectedFiles : []
      };
    } catch {
      return DEFAULT_PREFERENCES;
    }
  }
}
