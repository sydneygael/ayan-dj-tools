/**
 * Configuration de l'environnement de développement.
 * Pointe vers le backend Spring Boot local (port 8080).
 * - apiUrl : URL de base pour les appels REST (fetch)
 * - wsUrl : URL du endpoint WebSocket STOMP (SockJS)
 */
export const environment = {
  apiUrl: 'http://localhost:8080',
  wsUrl: 'ws://localhost:8080/ws',
};
