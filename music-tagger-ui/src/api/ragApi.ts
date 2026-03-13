/**
 * Client API REST pour la recherche sémantique (RAG).
 * Communique avec RagController (GET /api/rag/similar).
 * Utilise Qdrant (vector store) pour trouver des pistes similaires par embedding.
 */
import { environment } from '../config/environment';
import type { SimilarTrackResult } from '../types/types';

/** URL de base de l'API RAG. */
const BASE = `${environment.apiUrl}/api/rag`;

/**
 * Recherche des pistes similaires à une requête textuelle.
 * @param query - Texte de recherche (artiste, titre, genre...).
 * @param limit - Nombre maximum de résultats (défaut : 5).
 * @returns Liste de pistes avec leur score de similarité (0 à 1).
 */
export async function findSimilar(query: string, limit = 5): Promise<SimilarTrackResult[]> {
  const res = await fetch(`${BASE}/similar?query=${encodeURIComponent(query)}&limit=${limit}`);
  if (!res.ok) throw new Error(`Find similar failed: ${res.status}`);
  return res.json();
}
