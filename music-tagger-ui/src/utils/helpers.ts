/** Extensions de fichiers audio supportées par l'application. */
const AUDIO_EXTENSIONS = ['.mp3', '.flac', '.wav', '.aiff', '.m4a', '.ogg'];

/** Extrait le nom de fichier à partir d'un chemin complet (supporte / et \). */
export function extractFilename(filepath: string): string {
  return filepath.split(/[/\\]/).pop() ?? filepath;
}

/** Formate une durée en secondes au format "m:ss" (ex: 3:05). */
export function formatTime(seconds: number): string {
  const m = Math.floor(seconds / 60);
  const s = Math.floor(seconds % 60);
  return `${m}:${s.toString().padStart(2, '0')}`;
}

/** Vérifie si un nom de fichier correspond à une extension audio supportée. */
export function isAudioFile(filename: string): boolean {
  const lower = filename.toLowerCase();
  return AUDIO_EXTENSIONS.some((ext) => lower.endsWith(ext));
}

/** Formate un timestamp ISO en heure locale française (HH:mm:ss). */
export function formatTimestamp(iso: string): string {
  return new Date(iso).toLocaleString('fr-FR', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
}
