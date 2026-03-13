/**
 * Client API REST pour l'application directe de tags (hors plan).
 * Communique avec TagController (POST /api/tags/apply, POST /api/tags/preview).
 * Utilisé pour les modifications immédiates en mode MANUAL ou APPLY.
 */
import { environment } from '../config/environment';
import type { BatchApplyResult, TagPreview } from '../types/types';

/** URL de base de l'API tags. */
const BASE = `${environment.apiUrl}/api/tags`;

/**
 * Applique des tags sur un ensemble de fichiers.
 * @param operations - Map filepath → (Map tagName → tagValue)
 * @returns Bilan avec compteurs succès/échecs et détails par fichier.
 */
export async function applyTags(
  operations: Record<string, Record<string, string>>,
): Promise<BatchApplyResult> {
  const res = await fetch(`${BASE}/apply`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(operations),
  });
  if (!res.ok) throw new Error(`Apply tags failed: ${res.status}`);
  return res.json();
}

/**
 * Aperçu des changements de tags sans les appliquer.
 * @param filepath - Chemin du fichier audio cible.
 * @param tags - Tags à prévisualiser (tagName → newValue).
 * @returns Liste des changements (ancien → nouveau) pour chaque tag modifié.
 */
export async function previewTags(
  filepath: string,
  tags: Record<string, string>,
): Promise<TagPreview> {
  const res = await fetch(`${BASE}/preview`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ filepath, tags }),
  });
  if (!res.ok) throw new Error(`Preview tags failed: ${res.status}`);
  return res.json();
}
