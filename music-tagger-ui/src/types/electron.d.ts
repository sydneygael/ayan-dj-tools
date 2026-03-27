export {};

declare global {
  interface Window {
    electron?: {
      selectAudioFiles: () => Promise<string[]>;
      selectAudioFolder: () => Promise<string[]>;
      getAppVersion: () => Promise<string>;
      getBackendStatus: () => Promise<{ ready: boolean }>;
      installUpdate: () => Promise<void>;
      onBackendStatus: (cb: (status: { ready: boolean; error: string | null }) => void) => () => void;
      onUpdateAvailable: (cb: (info: { version: string }) => void) => () => void;
      onUpdateDownloaded: (cb: () => void) => () => void;
      onMenuSelectFiles: (cb: () => void) => () => void;
    };
  }
}
