import { create } from 'zustand';
import { authApi } from '../api/auth';
import { usersApi } from '../api/users';

export const useAuthStore = create((set, get) => ({
  user: JSON.parse(localStorage.getItem('user') || 'null'),
  isAuthenticated: !!localStorage.getItem('accessToken'),
  loading: false,
  error: null,

  register: async (data) => {
    set({ loading: true, error: null });
    try {
      const res = await authApi.register(data);
      localStorage.setItem('accessToken', res.accessToken);
      localStorage.setItem('refreshToken', res.refreshToken);
      // Fetch full profile so avatarUrl and displayName are populated
      const profile = await usersApi.getMe();
      const user = {
        id: profile.id ?? res.userId,
        username: profile.username ?? res.username,
        email: profile.email ?? res.email,
        displayName: profile.displayName,
        avatarUrl: profile.avatarUrl,
      };
      localStorage.setItem('user', JSON.stringify(user));
      set({ user, isAuthenticated: true, loading: false });
      return true;
    } catch (err) {
      set({ error: err.message, loading: false });
      return false;
    }
  },

  login: async (data) => {
    set({ loading: true, error: null });
    try {
      const res = await authApi.login(data);
      localStorage.setItem('accessToken', res.accessToken);
      localStorage.setItem('refreshToken', res.refreshToken);
      // Fetch full profile so avatarUrl and displayName are populated
      const profile = await usersApi.getMe();
      const user = {
        id: profile.id ?? res.userId,
        username: profile.username ?? res.username,
        email: profile.email ?? res.email,
        displayName: profile.displayName,
        avatarUrl: profile.avatarUrl,
      };
      localStorage.setItem('user', JSON.stringify(user));
      set({ user, isAuthenticated: true, loading: false });
      return true;
    } catch (err) {
      set({ error: err.message, loading: false });
      return false;
    }
  },

  logout: async () => {
    try { await authApi.logout(); } catch {}
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    set({ user: null, isAuthenticated: false });
  },

  clearError: () => set({ error: null }),
}));

