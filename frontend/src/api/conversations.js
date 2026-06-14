import { api } from './client';

export const conversationsApi = {
  getAll: () => api.get('/conversations'),
  getById: (id) => api.get(`/conversations/${id}`),
  create: (data) => api.post('/conversations', data),
  addParticipants: (id, userIds) => api.post(`/conversations/${id}/participants`, { userIds }),
  removeParticipant: (id, userId) => api.delete(`/conversations/${id}/participants/${userId}`),
  markRead: (id) => api.post(`/conversations/${id}/read`),
};
