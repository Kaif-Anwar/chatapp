import { api } from './client';

import { API_BASE_URL } from '../utils/constants';

export const messagesApi = {
  getByConversation: (conversationId, page = 0, size = 30) =>
    api.get(`/conversations/${conversationId}/messages?page=${page}&size=${size}`),
  send: (conversationId, data) => api.post(`/conversations/${conversationId}/messages`, data),
  edit: (conversationId, messageId, data) =>
    api.put(`/conversations/${conversationId}/messages/${messageId}`, data),
  delete: (conversationId, messageId) =>
    api.delete(`/conversations/${conversationId}/messages/${messageId}`),
  markRead: (conversationId, messageId) =>
    api.post(`/conversations/${conversationId}/messages/${messageId}/read`),
  search: (conversationId, q) =>
    api.get(`/conversations/${conversationId}/messages/search?q=${encodeURIComponent(q)}`),
  sendAttachment: async (conversationId, file, content = '') => {
    const token = localStorage.getItem('accessToken');
    const form = new FormData();
    form.append('file', file);
    if (content) form.append('content', content);
    const res = await fetch(`${API_BASE_URL}/conversations/${conversationId}/messages/attachment`, {
      method: 'POST',
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      body: form,
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({ message: 'Upload failed' }));
      throw new Error(err.message || `Error ${res.status}`);
    }
    return res.json();
  },
};
