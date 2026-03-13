import { contextBridge, ipcRenderer } from 'electron';

contextBridge.exposeInMainWorld('electron', {
  selectAudioFiles: (): Promise<string[]> => ipcRenderer.invoke('select-audio-files'),
});
