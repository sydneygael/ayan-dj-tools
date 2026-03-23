/**
 * Client API REST pour les statistiques.
 * Communique avec StatsController (GET /api/stats, /api/stats/collection, /enrichment, /activity).
 */
import { environment } from '../config/environment';
import type { StatsReport, CollectionProfile, EnrichmentStats, ActivityTimeline } from '../types/types';

/** Récupère le rapport de statistiques agrégées (plans, tags, fichiers, activité récente). */
export async function getStats(): Promise<StatsReport> {
  const res = await fetch(`${environment.apiUrl}/api/stats`);
  if (!res.ok) throw new Error(`Get stats failed: ${res.status}`);
  return res.json();
}

/** Récupère le profil de la collection (genres, BPM, tonalités, audio features). */
export async function getCollectionProfile(): Promise<CollectionProfile> {
  const res = await fetch(`${environment.apiUrl}/api/stats/collection`);
  if (!res.ok) throw new Error(`Get collection profile failed: ${res.status}`);
  return res.json();
}

/** Récupère les statistiques d'enrichissement (taux Spotify, erreurs, sources). */
export async function getEnrichmentStats(): Promise<EnrichmentStats> {
  const res = await fetch(`${environment.apiUrl}/api/stats/enrichment`);
  if (!res.ok) throw new Error(`Get enrichment stats failed: ${res.status}`);
  return res.json();
}

/** Récupère la chronologie d'activité pour une période donnée. */
export async function getActivityTimeline(period: 'week' | 'month' | 'all'): Promise<ActivityTimeline> {
  const res = await fetch(`${environment.apiUrl}/api/stats/activity?period=${period}`);
  if (!res.ok) throw new Error(`Get activity timeline failed: ${res.status}`);
  return res.json();
}
