export {};

declare global {
  interface Window {
    electron?: {
      selectAudioFiles: () => Promise<string[]>;
    };
  }
}
