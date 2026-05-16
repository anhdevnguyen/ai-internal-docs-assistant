import axiosClient from './axiosClient'

export const authApi = {
  register: (tenantId, email, password) =>
    axiosClient.post('/auth/register', { tenantId, email, password }),

  login: (tenantId, email, password) =>
    axiosClient.post('/auth/login', { tenantId, email, password }),

  refresh: (refreshToken) =>
    axiosClient.post('/auth/refresh', { refreshToken }),

  logout: () =>
    axiosClient.delete('/auth/logout'),
}