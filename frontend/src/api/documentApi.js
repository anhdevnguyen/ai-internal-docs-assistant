import axiosClient from './axiosClient'

export const documentApi = {
  upload(file, title, onUploadProgress) {
    const form = new FormData()
    form.append('file', file)
    // title is required by backend (@NotBlank), always send it
    form.append('title', title?.trim() || file.name.replace(/\.[^.]+$/, ''))

    return axiosClient.post('/api/v1/documents', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress,
    })
  },

  list(page = 0, size = 20) {
    return axiosClient.get('/api/v1/documents', { params: { page, size } })
  },

  get(id) {
    return axiosClient.get(`/api/v1/documents/${id}`)
  },

  remove(id) {
    return axiosClient.delete(`/api/v1/documents/${id}`)
  },
}