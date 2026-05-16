import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
} from 'react'
import { authApi } from '../api/authApi'
import { tokenStore } from '../api/axiosClient'

const AuthContext = createContext(null)

/**
 * AuthProvider bao quanh toàn bộ app.
 *
 * Session lifecycle:
 * 1. Mount → thử restore session từ refreshToken trong localStorage.
 * 2. isInitializing = true trong thời gian này → ProtectedRoute không render gì.
 * 3. Nếu restore thành công → set user, set access token in memory.
 * 4. Nếu thất bại → clear storage, user = null.
 * 5. Event 'auth:session-expired' từ axiosClient → clear state, user = null.
 */
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [isInitializing, setIsInitializing] = useState(true)
  const restoredRef = useRef(false)

  const clearSession = useCallback(() => {
    tokenStore.clear()
    localStorage.removeItem('refreshToken')
    setUser(null)
  }, [])

  const persistSession = useCallback((data) => {
    tokenStore.set(data.accessToken)
    localStorage.setItem('refreshToken', data.refreshToken)
    setUser(data.user)
  }, [])

  // Restore session một lần duy nhất khi app mount
  useEffect(() => {
    if (restoredRef.current) return
    restoredRef.current = true

    const restore = async () => {
      const storedRefreshToken = localStorage.getItem('refreshToken')
      if (!storedRefreshToken) {
        setIsInitializing(false)
        return
      }
      try {
        const { data: resp } = await authApi.refresh(storedRefreshToken)
        persistSession(resp.data)
      } catch {
        // refreshToken hết hạn hoặc bị revoke → treat as logged out
        clearSession()
      } finally {
        setIsInitializing(false)
      }
    }

    restore()
  }, [clearSession, persistSession])

  // axiosClient dispatch event này khi refresh thất bại giữa chừng một request
  useEffect(() => {
    const onSessionExpired = () => clearSession()
    window.addEventListener('auth:session-expired', onSessionExpired)
    return () => window.removeEventListener('auth:session-expired', onSessionExpired)
  }, [clearSession])

  const login = useCallback(
    async (tenantId, email, password) => {
      const { data: resp } = await authApi.login(tenantId, email, password)
      persistSession(resp.data)
      return resp.data.user
    },
    [persistSession]
  )

  const register = useCallback(
    async (tenantId, email, password) => {
      const { data: resp } = await authApi.register(tenantId, email, password)
      persistSession(resp.data)
      return resp.data.user
    },
    [persistSession]
  )

  const logout = useCallback(async () => {
    try {
      await authApi.logout()
    } finally {
      clearSession()
    }
  }, [clearSession])

  return (
    <AuthContext.Provider
      value={{
        user,
        isInitializing,
        isAuthenticated: !!user,
        login,
        register,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>')
  return ctx
}