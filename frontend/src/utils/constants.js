// All URLs are relative — nginx (production) and Vite dev proxy (local)
// both route /api, /ws, and /uploads to the Spring Boot backend.
// No hardcoded hostnames needed anywhere in the frontend.

export const API_BASE_URL = '/api';

// WebSocket needs a full URL for SockJS. We derive it from the current
// page origin so it works on any domain (localhost, DigitalOcean, etc.)
export const WS_URL = window.location.origin + '/ws';

// Avatar images are served at /uploads/... — same origin, no prefix needed.
export const API_BASE_HOST = '';
