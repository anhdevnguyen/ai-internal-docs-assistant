import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'

/**
 * isInitializing = true có nghĩa app đang cố restore session từ refreshToken.
 * Trong window này KHÔNG redirect về /login — sẽ gây flash sai với user đang có session hợp lệ.
 */
export function ProtectedRoute({ children, requiredRole }) {
  const { isInitializing, isAuthenticated, user } = useAuth()
  const location = useLocation()

  if (isInitializing) {
    return (
      <div className="init-screen">
        <div className="init-spinner" />
      </div>
    )
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  if (requiredRole && user?.role !== requiredRole) {
    return <Navigate to="/" replace />
  }

  return children
}