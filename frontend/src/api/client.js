import { API_BASE_URL } from '../utils/constants';

/**
 * Fetch wrapper that auto-attaches JWT and handles token refresh.
 */
async function request(endpoint, options = {}) {
  const token = localStorage.getItem('accessToken');
  const headers = {
    'Content-Type': 'application/json',
    ...(token && { Authorization: `Bearer ${token}` }),
    ...options.headers,
  };

  const res = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    headers,
  });

  // If 401 and we have a refresh token, try to refresh
  if (res.status === 401 && localStorage.getItem('refreshToken')) {
    const refreshed = await tryRefreshToken();
    if (refreshed) {
      // Retry original request with new token
      headers.Authorization = `Bearer ${localStorage.getItem('accessToken')}`;
      const retry = await fetch(`${API_BASE_URL}${endpoint}`, { ...options, headers });
      if (!retry.ok) {
        const err = await retry.json().catch(() => ({ message: 'Request failed' }));
        throw new Error(err.message || `Error ${retry.status}`);
      }
      return retry.status === 204 ? null : retry.json();
    }
    // Refresh failed — clear tokens and redirect to login
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    window.location.href = '/login';
    throw new Error('Session expired');
  }

  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: 'Request failed' }));
    throw new Error(err.message || `Error ${res.status}`);
  }

  return res.status === 204 ? null : res.json();
}

async function tryRefreshToken() {
  try {
    const refreshToken = localStorage.getItem('refreshToken');
    const res = await fetch(`${API_BASE_URL}/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    });
    if (!res.ok) return false;
    const data = await res.json();
    localStorage.setItem('accessToken', data.accessToken);
    localStorage.setItem('refreshToken', data.refreshToken);
    return true;
  } catch {
    return false;
  }
}

export const api = {
  get: (endpoint) => request(endpoint),
  post: (endpoint, body) => request(endpoint, { method: 'POST', body: JSON.stringify(body) }),
  put: (endpoint, body) => request(endpoint, { method: 'PUT', body: JSON.stringify(body) }),
  delete: (endpoint) => request(endpoint, { method: 'DELETE' }),
};
