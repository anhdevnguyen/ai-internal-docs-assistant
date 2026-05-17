import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './contexts/AuthContext'
import { ProtectedRoute } from './components/ProtectedRoute'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import DashboardPage from './pages/DashboardPage'
import DocumentsPage from './pages/DocumentsPage'
import DocumentDetailPage from './pages/DocumentDetailPage'
import ChatPage from './pages/ChatPage'
import AdminOverviewPage from './pages/admin/AdminOverviewPage'
import AdminDocumentsPage from './pages/admin/AdminDocumentsPage'
import AdminUsersPage from './pages/admin/AdminUsersPage'

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          {/* Public auth routes */}
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />

          {/* Redirect root to /chat — the core product feature */}
          <Route path="/" element={<Navigate to="/chat" replace />} />

          <Route
            path="/chat"
            element={
              <ProtectedRoute>
                <ChatPage />
              </ProtectedRoute>
            }
          />

          <Route
            path="/documents"
            element={
              <ProtectedRoute>
                <DocumentsPage />
              </ProtectedRoute>
            }
          />

          <Route
            path="/documents/:id"
            element={
              <ProtectedRoute>
                <DocumentDetailPage />
              </ProtectedRoute>
            }
          />

          {/*
           * Admin routes are nested under ProtectedRoute with requiredRole="ADMIN".
           * Each sub-page carries its own AdminLayout — no shared outlet needed
           * since layout is thin (just a subnav strip inside AppLayout).
           */}
          <Route
            path="/admin"
            element={
              <ProtectedRoute requiredRole="ADMIN">
                <AdminOverviewPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/documents"
            element={
              <ProtectedRoute requiredRole="ADMIN">
                <AdminDocumentsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/users"
            element={
              <ProtectedRoute requiredRole="ADMIN">
                <AdminUsersPage />
              </ProtectedRoute>
            }
          />

          <Route path="*" element={<Navigate to="/chat" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}