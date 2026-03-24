import { environment } from '../config/environment';

export async function checkBackendHealth(): Promise<boolean> {
  try {
    const res = await fetch(`${environment.apiUrl}/actuator/health`, {
      signal: AbortSignal.timeout(3000),
    });
    return res.ok;
  } catch {
    return false;
  }
}
