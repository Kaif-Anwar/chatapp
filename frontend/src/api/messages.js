import { api } from './client';

export const messagesApi = {
  getByConversation: (conversationId, page = 0, size = 30) =>
    api.get(`/conversations/${conversationId}/messages?page=${page}&size=${size}`),
  send: (conversationId, data) => api.post(`/conversations/${conversationId}/messages`, data),
  edit: (conversationId, messageId, data) =>
    api.put(`/conversations/${conversationId}/messages/${messageId}`, data),
  delete: (conversationId, messageId) =>
    api.delete(`/conversations/${conversationId}/messages/${messageId}`),
};
