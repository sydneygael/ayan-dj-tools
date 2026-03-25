import { environment } from '../config/environment';
import type { Playlist, PlaylistGenerationRequest } from '../types/types';

const BASE = `${environment.apiUrl}/api/playlist`;

export async function generatePlaylist(req: PlaylistGenerationRequest): Promise<Playlist> {
  const res = await fetch(`${BASE}/generate`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json();
}
