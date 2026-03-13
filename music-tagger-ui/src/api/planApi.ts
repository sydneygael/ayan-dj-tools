/**
 * Client API REST pour la gestion des plans de tagging.
 * Communique avec PlanController (POST /api/plan/create, GET/PUT/POST/DELETE /api/plan/:id).
 * Cycle de vie d'un plan : create → getPlan → approve → execute → history.
 */
import { environment } from '../config/environment';
import type { BatchApplyResult, TaggingHistoryEntry, TaggingPlan, TagPreview } from '../types/types';

/** URL de base de l'API plan. */
const BASE = `${environment.apiUrl}/api/plan`;

/** Crée un nouveau plan de tagging à partir d'une liste de chemins de fichiers audio. */
export async function createPlan(filePaths: string[]): Promise<TaggingPlan> {
  const res = await fetch(`${BASE}/create`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(filePaths),
  });
  if (!res.ok) throw new Error(`Create plan failed: ${res.status}`);
  return res.json();
}

/** Récupère un plan par son ID (avec ses opérations et statut). */
export async function getPlan(planId: string): Promise<TaggingPlan> {
  const res = await fetch(`${BASE}/${planId}`);
  if (!res.ok) throw new Error(`Get plan failed: ${res.status}`);
  return res.json();
}

/** Approuve toutes les opérations PENDING du plan (passe le plan en APPROVED). */
export async function approvePlan(planId: string): Promise<TaggingPlan> {
  const res = await fetch(`${BASE}/${planId}/approve`, { method: 'PUT' });
  if (!res.ok) throw new Error(`Approve plan failed: ${res.status}`);
  return res.json();
}

/** Exécute le plan : écrit les tags sur les fichiers approuvés. Retourne le bilan succès/échecs. */
export async function executePlan(planId: string): Promise<BatchApplyResult> {
  const res = await fetch(`${BASE}/${planId}/execute`, { method: 'POST' });
  if (!res.ok) throw new Error(`Execute plan failed: ${res.status}`);
  return res.json();
}

/** Récupère l'aperçu des changements (diff) pour toutes les opérations du plan. */
export async function previewPlan(planId: string): Promise<TagPreview[]> {
  const res = await fetch(`${BASE}/${planId}/preview`);
  if (!res.ok) throw new Error(`Preview plan failed: ${res.status}`);
  return res.json();
}

/** Récupère l'historique des modifications appliquées pour un plan donné. */
export async function getPlanHistory(planId: string): Promise<TaggingHistoryEntry[]> {
  const res = await fetch(`${BASE}/${planId}/history`);
  if (!res.ok) throw new Error(`Get plan history failed: ${res.status}`);
  return res.json();
}

/** Supprime un plan et ses données associées. */
export async function deletePlan(planId: string): Promise<void> {
  const res = await fetch(`${BASE}/${planId}`, { method: 'DELETE' });
  if (!res.ok) throw new Error(`Delete plan failed: ${res.status}`);
}
