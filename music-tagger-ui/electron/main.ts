import { app, BrowserWindow, dialog, ipcMain, Menu, shell } from 'electron';
import { autoUpdater } from 'electron-updater';
import * as path from 'path';
import * as fs from 'fs';
import { spawn, ChildProcess } from 'child_process';
import * as http from 'http';

let mainWindow: BrowserWindow | null = null;
let backendProcess: ChildProcess | null = null;
let backendReady = false;

const AUDIO_EXTENSIONS = [
  { name: 'Audio Files', extensions: ['mp3', 'flac', 'wav', 'aiff', 'm4a', 'ogg'] },
];

const AUDIO_EXT_SET = new Set(['.mp3', '.flac', '.wav', '.aiff', '.m4a', '.ogg']);

/** Scanne récursivement un dossier et retourne les chemins absolus de tous les fichiers audio. */
function scanAudioFiles(dir: string): string[] {
  const results: string[] = [];
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      results.push(...scanAudioFiles(fullPath));
    } else if (AUDIO_EXT_SET.has(path.extname(entry.name).toLowerCase())) {
      results.push(fullPath);
    }
  }
  return results;
}

const isDev = process.argv.includes('--dev');
const BACKEND_PORT = 8000;
const BACKEND_URL = `http://localhost:${BACKEND_PORT}`;

// --- Backend JAR ---

function findBackendJar(): string | null {
  const jarPath = path.join(process.resourcesPath, 'backend', 'ayan-dj-tools.jar');
  return fs.existsSync(jarPath) ? jarPath : null;
}

function waitForBackend(timeoutMs = 30000): Promise<boolean> {
  return new Promise((resolve) => {
    const start = Date.now();
    const check = () => {
      const req = http.get(`${BACKEND_URL}/actuator/health`, (res) => {
        resolve(res.statusCode === 200);
      });
      req.on('error', () => {
        if (Date.now() - start < timeoutMs) {
          setTimeout(check, 1000);
        } else {
          resolve(false);
        }
      });
      req.setTimeout(2000, () => {
        req.destroy();
        if (Date.now() - start < timeoutMs) {
          setTimeout(check, 1000);
        } else {
          resolve(false);
        }
      });
    };
    check();
  });
}

function launchBackend(jarPath: string): void {
  console.log(`[backend] Launching JAR: ${jarPath}`);
  backendProcess = spawn('java', ['-jar', jarPath], {
    detached: false,
    stdio: ['ignore', 'pipe', 'pipe'],
  });

  backendProcess.stdout?.on('data', (data: Buffer) => {
    console.log(`[backend] ${data.toString().trim()}`);
  });

  backendProcess.stderr?.on('data', (data: Buffer) => {
    console.error(`[backend] ${data.toString().trim()}`);
  });

  backendProcess.on('exit', (code) => {
    console.log(`[backend] Process exited with code ${code}`);
    backendReady = false;
    mainWindow?.webContents.send('backend-status', { ready: false, error: `Process exited (code ${code})` });
  });

  waitForBackend().then((ready) => {
    backendReady = ready;
    mainWindow?.webContents.send('backend-status', {
      ready,
      error: ready ? null : 'Backend did not respond within 30s',
    });
  });
}

// --- Window ---

function createWindow(): void {
  mainWindow = new BrowserWindow({
    width: 1400,
    height: 900,
    minWidth: 1000,
    minHeight: 700,
    title: 'Ayan DJ Tools',
    webPreferences: {
      contextIsolation: true,
      nodeIntegration: false,
      preload: path.join(__dirname, 'preload.js'),
    },
  });

  if (isDev) {
    mainWindow.loadURL('http://localhost:5173');
    mainWindow.webContents.openDevTools();
  } else {
    mainWindow.loadFile(path.join(__dirname, '..', 'dist', 'index.html'));
  }

  mainWindow.on('closed', () => {
    mainWindow = null;
  });
}

// --- Native Menu ---

function buildMenu(): void {
  const template: Electron.MenuItemConstructorOptions[] = [
    {
      label: 'Fichier',
      submenu: [
        {
          label: 'Ouvrir des fichiers audio...',
          accelerator: 'CmdOrCtrl+O',
          click: () => mainWindow?.webContents.send('menu-select-files'),
        },
        { type: 'separator' },
        { role: 'quit', label: 'Quitter' },
      ],
    },
    {
      label: 'Affichage',
      submenu: [
        { role: 'reload', label: 'Recharger' },
        { role: 'toggleDevTools', label: 'Outils de développement' },
        { type: 'separator' },
        { role: 'resetZoom', label: 'Zoom normal' },
        { role: 'zoomIn', label: 'Zoom avant' },
        { role: 'zoomOut', label: 'Zoom arrière' },
        { type: 'separator' },
        { role: 'togglefullscreen', label: 'Plein écran' },
      ],
    },
    {
      label: 'Aide',
      submenu: [
        {
          label: 'Documentation',
          click: () => shell.openExternal('https://github.com/your-org/ayan-dj-tools#readme'),
        },
        {
          label: 'Signaler un problème',
          click: () => shell.openExternal('https://github.com/your-org/ayan-dj-tools/issues'),
        },
      ],
    },
  ];
  Menu.setApplicationMenu(Menu.buildFromTemplate(template));
}

// --- Auto Updater ---

function setupAutoUpdater(): void {
  if (isDev) return;

  autoUpdater.autoDownload = false;
  autoUpdater.autoInstallOnAppQuit = true;

  autoUpdater.on('update-available', (info) => {
    mainWindow?.webContents.send('update-available', { version: info.version });
  });

  autoUpdater.on('update-downloaded', () => {
    mainWindow?.webContents.send('update-downloaded');
  });

  autoUpdater.on('error', (err) => {
    console.error('[updater] Error:', err.message);
  });

  setTimeout(() => {
    autoUpdater.checkForUpdatesAndNotify().catch((err) => {
      console.error('[updater] checkForUpdatesAndNotify failed:', err.message);
    });
  }, 5000);
}

// --- IPC Handlers ---

function registerIpcHandlers(): void {
  ipcMain.handle('select-audio-files', async () => {
    if (!mainWindow) return [];
    const result = await dialog.showOpenDialog(mainWindow, {
      properties: ['openFile', 'multiSelections'],
      filters: AUDIO_EXTENSIONS,
    });
    return result.canceled ? [] : result.filePaths;
  });

  ipcMain.handle('select-audio-folder', async () => {
    if (!mainWindow) return [];
    const result = await dialog.showOpenDialog(mainWindow, {
      properties: ['openDirectory'],
    });
    if (result.canceled || result.filePaths.length === 0) return [];
    return scanAudioFiles(result.filePaths[0]);
  });

  ipcMain.handle('get-app-version', () => app.getVersion());

  ipcMain.handle('get-backend-status', () => ({ ready: backendReady }));

  ipcMain.handle('install-update', () => {
    autoUpdater.quitAndInstall();
  });
}

// --- Graceful Shutdown ---

function gracefulShutdown(): void {
  if (!backendProcess) return;
  const pid = backendProcess.pid;
  if (!pid) return;

  console.log(`[backend] Shutting down PID ${pid}`);
  if (process.platform === 'win32') {
    spawn('taskkill', ['/pid', String(pid), '/f', '/t'], { detached: true, stdio: 'ignore' });
  } else {
    backendProcess.kill('SIGTERM');
  }
  backendProcess = null;
}

// --- Bootstrap ---

app.whenReady().then(() => {
  registerIpcHandlers();
  buildMenu();
  createWindow();
  setupAutoUpdater();

  if (!isDev) {
    const jarPath = findBackendJar();
    if (jarPath) {
      launchBackend(jarPath);
    } else {
      console.warn('[backend] JAR not found in resources, polling for existing backend...');
      waitForBackend().then((ready) => {
        backendReady = ready;
        mainWindow?.webContents.send('backend-status', {
          ready,
          error: ready ? null : 'Backend JAR introuvable et aucun backend actif',
        });
      });
    }
  } else {
    // Dev mode: assume backend is already running
    waitForBackend().then((ready) => {
      backendReady = ready;
      mainWindow?.webContents.send('backend-status', {
        ready,
        error: ready ? null : 'Backend non disponible (mode dev)',
      });
    });
  }
});

app.on('before-quit', gracefulShutdown);

app.on('window-all-closed', () => {
  gracefulShutdown();
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

app.on('activate', () => {
  if (mainWindow === null) {
    createWindow();
  }
});
