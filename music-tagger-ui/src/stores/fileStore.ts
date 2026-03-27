import {create} from 'zustand';

/** État de la sélection de fichiers audio. */
interface FileState {
    /** Liste des chemins absolus des fichiers sélectionnés. */
    selectedFiles: string[];
    /** Fichier actuellement sélectionné pour la pré-écoute dans le lecteur audio. */
    selectedSingleFile: string | null;
    /** Ajoute des fichiers (dédupliqués) à la sélection existante. */
    addFiles: (paths: string[]) => void;
    /** Retire un fichier de la sélection (et du lecteur audio s'il y était). */
    removeFile: (path: string) => void;
    /** Vide toute la sélection. */
    clearFiles: () => void;
    /** Sélectionne un fichier pour la pré-écoute dans le lecteur audio. */
    selectSingleFile: (path: string | null) => void;
    /** Ouvre le file picker natif Electron et ajoute les fichiers sélectionnés. */
    selectFiles: () => Promise<void>;
    /** Ouvre un sélecteur de dossier et ajoute récursivement tous les fichiers audio trouvés. */
    selectFolder: () => Promise<void>;
}

/**
 * Store Zustand de la gestion des fichiers audio.
 * Non persisté (les fichiers sont re-sélectionnés à chaque session).
 * La déduplication dans addFiles empêche l'ajout de doublons (comparaison par chemin).
 */
export const useFileStore = create<FileState>()
((set, get) => ({
    selectedFiles: [],
    selectedSingleFile: null,

    addFiles: (paths) =>
        set((s) => {
            // Filtre les chemins déjà présents pour éviter les doublons
            const existing = new Set(s.selectedFiles);
            const newPaths = paths.filter((p) => !existing.has(p));
            return newPaths.length > 0
                ? {selectedFiles: [...s.selectedFiles, ...newPaths]}
                : s;
        }),

    removeFile: (path) =>
        set((s) => ({
            selectedFiles: s.selectedFiles.filter((f) => f !== path),
            // Désélectionne le fichier du lecteur audio s'il est supprimé de la liste
            selectedSingleFile: s.selectedSingleFile === path ? null : s.selectedSingleFile,
        })),

    clearFiles: () => set({selectedFiles: [], selectedSingleFile: null}),

    selectSingleFile: (path) => set({selectedSingleFile: path}),

    selectFolder: async () => {
        if (window.electron) {
            // Mode Electron : scan récursif dans le main process (accès Node.js complet)
            const files = await window.electron.selectAudioFolder();
            if (files.length > 0) get().addFiles(files);
        }
        // Pas de fallback navigateur : l'accès au système de fichiers n'est pas disponible hors Electron
    },

    selectFiles: async () => {
        if (window.electron) {
            // Mode Electron : dialog natif via IPC (retourne des chemins absolus)
            const files = await window.electron.selectAudioFiles();
            if (files.length > 0) get().addFiles(files);
        } else {
            // Fallback navigateur : input HTML standard
            // En renderer Electron sans preload, file.path donne aussi le chemin absolu
            await new Promise<void>((resolve) => {
                const input = document.createElement('input');
                input.type = 'file';
                input.multiple = true;
                input.accept = '.mp3,.flac,.wav,.aiff,.m4a,.ogg';
                input.onchange = () => {
                    const paths = Array.from(input.files ?? [])
                        .map((f) => (f as File & { path: string }).path)
                        .filter(Boolean);
                    if (paths.length > 0) get().addFiles(paths);
                    resolve();
                };
                input.oncancel = () => resolve();
                input.click();
            });
        }
    },
}));
