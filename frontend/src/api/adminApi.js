import axiosClient from './axiosClient'

export const adminApi = {
  getOverview() {
    return axiosClient.get('/api/v1/admin/metrics/overview')
  },

  listDocuments(page = 0, size = 20) {
    return axiosClient.get('/api/v1/admin/documents', { params: { page, size } })
  },

  deleteDocument(documentId) {
    return axiosClient.delete(`/api/v1/admin/documents/${documentId}`)
  },

  reindexDocument(documentId) {
    return axiosClient.post(`/api/v1/admin/documents/${documentId}/reindex`)
  },

  listUsers(page = 0, size = 20) {
    return axiosClient.get('/api/v1/admin/users', { params: { page, size } })
  },

  activateUser(userId) {
    return axiosClient.patch(`/api/v1/admin/users/${userId}/activate`)
  },

  deactivateUser(userId) {
    return axiosClient.patch(`/api/v1/admin/users/${userId}/deactivate`)
  },
}