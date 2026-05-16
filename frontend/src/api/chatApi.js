import axiosClient from './axiosClient'

export const chatApi = {
  createSession() {
    return axiosClient.post('/api/v1/chat/sessions')
  },

  listSessions() {
    return axiosClient.get('/api/v1/chat/sessions')
  },

  sendMessage(sessionId, content) {
    return axiosClient.post(`/api/v1/chat/sessions/${sessionId}/messages`, { content })
  },

  listMessages(sessionId) {
    return axiosClient.get(`/api/v1/chat/sessions/${sessionId}/messages`)
  },
}