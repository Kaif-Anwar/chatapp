import { api } from './client';

export const presenceApi = {
  getStatus: (userId) => api.get(`/presence/${userId}`),
};
