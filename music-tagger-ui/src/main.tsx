import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import './i18n';
import './index.css';
import App from './App';

/**
 * Point d'entrée de l'application React.
 * Monte le composant racine App dans l'élément #root du DOM.
 * StrictMode active les vérifications supplémentaires en développement
 * (double-rendu, détection d'effets de bord dans les useEffect).
 */
createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
