import { contextBridge, ipcRenderer } from 'electron';

contextBridge.exposeInMainWorld('electron', {
  selectAudioFiles: (): Promise<string[]> =>
    ipcRenderer.invoke('select-audio-files'),

  getAppVersion: (): Promise<string> =>
    ipcRenderer.invoke('get-app-version'),

  getBackendStatus: (): Promise<{ ready: boolean }> =>
    ipcRenderer.invoke('get-backend-status'),

  installUpdate: (): Promise<void> =>
    ipcRenderer.invoke('install-update'),

  onBackendStatus: (cb: (status: { ready: boolean; error: string | null }) => void): (() => void) => {
    const listener = (_: Electron.IpcRendererEvent, status: { ready: boolean; error: string | null }) => cb(status);
    ipcRenderer.on('backend-status', listener);
    return () => ipcRenderer.removeListener('backend-status', listener);
  },

  onUpdateAvailable: (cb: (info: { version: string }) => void): (() => void) => {
    const listener = (_: Electron.IpcRendererEvent, info: { version: string }) => cb(info);
    ipcRenderer.on('update-available', listener);
    return () => ipcRenderer.removeListener('update-available', listener);
  },

  onUpdateDownloaded: (cb: () => void): (() => void) => {
    const listener = () => cb();
    ipcRenderer.on('update-downloaded', listener);
    return () => ipcRenderer.removeListener('update-downloaded', listener);
  },

  onMenuSelectFiles: (cb: () => void): (() => void) => {
    const listener = () => cb();
    ipcRenderer.on('menu-select-files', listener);
    return () => ipcRenderer.removeListener('menu-select-files', listener);
  },
});
