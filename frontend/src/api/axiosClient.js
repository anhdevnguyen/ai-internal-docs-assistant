import axios from 'axios'

/**
 * Access token sống trong module-level variable — không lưu localStorage.
 * Access token ngắn hạn (15 phút); giữ ngoài storage để giảm XSS attack surface.
 * Refresh token nằm trong localStorage vì cần persist qua browser refresh.
 */
let _accessToken = null

export const tokenStore = {
  get: () => _accessToken,
  set: (token) => { _accessToken = token },
  clear: () => { _accessToken = null },
}

const axiosClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080',
  headers: { 'Content-Type': 'application/json' },
})

// ── Request interceptor ──────────────────────────────────────────────────────
axiosClient.interceptors.request.use((config) => {
  const token = tokenStore.get()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// ── Response interceptor — silent refresh ────────────────────────────────────
// Hai request đồng thời nhận 401 → cả hai cố refresh → token thứ hai đã bị rotate.
// Pattern: queue tất cả pending request trong khi refresh đang chạy,
// drain queue sau khi refresh xong.
let isRefreshing = false
let pendingQueue = []

const drainQueue = (error, newToken) => {
  pendingQueue.forEach(({ resolve, reject }) =>
    error ? reject(error) : resolve(newToken)
  )
  pendingQueue = []
}

axiosClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config
    const status = error.response?.status

    // Không retry: không phải 401, đã retry rồi, hoặc chính request auth
    if (status !== 401 || original._retry || original.url?.startsWith('/auth/')) {
      return Promise.reject(error)
    }

    if (isRefreshing) {
      // Enqueue — sẽ được retry với access token mới sau khi refresh xong
      return new Promise((resolve, reject) => {
        pendingQueue.push({ resolve, reject })
      }).then((newToken) => {
        original.headers.Authorization = `Bearer ${newToken}`
        return axiosClient(original)
      })
    }

    original._retry = true
    isRefreshing = true

    try {
      const refreshToken = localStorage.getItem('refreshToken')
      if (!refreshToken) throw new Error('No refresh token')

      // Dùng axios thuần — không qua axiosClient để tránh vòng lặp interceptor
      const { data: resp } = await axios.post(
        `${axiosClient.defaults.baseURL}/auth/refresh`,
        { refreshToken },
        { headers: { 'Content-Type': 'application/json' } }
      )

      const newAccessToken = resp.data.accessToken
      tokenStore.set(newAccessToken)
      localStorage.setItem('refreshToken', resp.data.refreshToken)

      drainQueue(null, newAccessToken)
      original.headers.Authorization = `Bearer ${newAccessToken}`
      return axiosClient(original)
    } catch (refreshError) {
      drainQueue(refreshError, null)
      tokenStore.clear()
      localStorage.removeItem('refreshToken')
      // AuthContext lắng nghe event này để clear state và redirect về /login
      window.dispatchEvent(new CustomEvent('auth:session-expired'))
      return Promise.reject(refreshError)
    } finally {
      isRefreshing = false
    }
  }
)

export default axiosClient