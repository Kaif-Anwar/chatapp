import { create } from 'zustand';
import { notificationsApi } from '../api/notifications';

export const useNotificationStore = create((set, get) => ({
  notifications: [],
  unreadCount: 0,
  loading: false,

  fetchNotifications: async () => {
    set({ loading: true });
    try {
      const data = await notificationsApi.getAll();
      set({ notifications: data.content || [], loading: false });
    } catch {
      set({ loading: false });
    }
  },

  fetchUnreadCount: async () => {
    try {
      const data = await notificationsApi.getUnreadCount();
      set({ unreadCount: typeof data === 'number' ? data : data.count || 0 });
    } catch {}
  },

  markAllRead: async () => {
    try {
      await notificationsApi.markAllRead();
      set((s) => ({
        unreadCount: 0,
        notifications: s.notifications.map((n) => ({ ...n, read: true })),
      }));
    } catch {}
  },

  addNotification: (notification) => {
    set((s) => ({
      notifications: [notification, ...s.notifications],
      unreadCount: s.unreadCount + 1,
    }));
  },
}));
