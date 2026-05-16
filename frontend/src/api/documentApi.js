import axiosClient from './axiosClient'

export const documentApi = {
  /**
   * Multipart upload. onUploadProgress lets the caller drive a progress bar.
   * title is optional — backend falls back to originalFilename.
   */
  upload(file, title, onUploadProgress) {
    const form = new FormData()
    form.append('file', file)
    if (title?.trim()) form.append('title', title.trim())

    return axiosClient.post('/documents', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress,
    })
  },

  list(page = 0, size = 20) {
    return axiosClient.get('/documents', { params: { page, size } })
  },

  get(id) {
    return axiosClient.get(`/documents/${id}`)
  },

  /** Lightweight status-only endpoint — used for polling in the UI. */
  getStatus(id) {
    return axiosClient.get(`/documents/${id}/status`)
  },

  remove(id) {
    return axiosClient.delete(`/documents/${id}`)
  },
}