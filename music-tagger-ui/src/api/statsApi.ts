/**
 * Client API REST pour les statistiques.
 * Communique avec StatsController (GET /api/stats).
 */
import { environment } from '../config/environment';
import type { StatsReport } from '../types/types';

/** Récupère le rapport de statistiques agrégées (plans, tags, fichiers, activité récente). */
export async function getStats(): Promise<StatsReport> {
  const res = await fetch(`${environment.apiUrl}/api/stats`);
  if (!res.ok) throw new Error(`Get stats failed: ${res.status}`);
  return res.json();
}
