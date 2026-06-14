import { api } from './client';
import { API_BASE_URL } from '../utils/constants';

export const usersApi = {
  getMe: () => api.get('/users/me'),
  updateMe: (data) => api.put('/users/me', data),
  search: (query) => api.get(`/users/search?q=${encodeURIComponent(query)}`),
  getById: (id) => api.get(`/users/${id}`),

  /**
   * Uploads an avatar image file to the backend.
   * Uses a raw fetch with FormData — bypasses the JSON api wrapper
   * because multipart/form-data must NOT have a Content-Type JSON header.
   *
   * @param {File} file - The image file from an <input type="file">
   * @returns {Promise<object>} Updated UserDTO from the server
   */
  uploadAvatar: async (file) => {
    const token = localStorage.getItem('accessToken');
    const formData = new FormData();
    formData.append('file', file);

    const res = await fetch(`${API_BASE_URL}/users/me/avatar`, {
      method: 'POST',
      headers: {
        ...(token && { Authorization: `Bearer ${token}` }),
        // Do NOT set Content-Type — browser sets it automatically with boundary
      },
      body: formData,
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({ message: 'Upload failed' }));
      throw new Error(err.message || `Upload failed (${res.status})`);
    }
    return res.json();
  },
};
