import { create } from 'zustand';

interface BackendState {
  ready: boolean;
  error: string | null;
  updateAvailable: string | null;
  updateDownloaded: boolean;
  setReady: (ready: boolean, error?: string | null) => void;
  setUpdateAvailable: (version: string) => void;
  setUpdateDownloaded: () => void;
}

export const useBackendStore = create<BackendState>()((set) => ({
  ready: false,
  error: null,
  updateAvailable: null,
  updateDownloaded: false,
  setReady: (ready, error = null) => set({ ready, error }),
  setUpdateAvailable: (version) => set({ updateAvailable: version }),
  setUpdateDownloaded: () => set({ updateDownloaded: true }),
}));
