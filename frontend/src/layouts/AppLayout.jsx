import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'

export default function AppLayout({ children }) {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = async () => {
    await logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="app-root">
      <aside className="app-sidebar">
        <div className="app-sidebar-logo">
          <span className="app-logo-icon">
            <LogoIcon />
          </span>
          DocMind
        </div>

        <nav className="app-nav">
          <NavLink
            to="/chat"
            className={({ isActive }) => `app-nav-item${isActive ? ' active' : ''}`}
          >
            <ChatIcon />
            Chat
          </NavLink>

          <NavLink
            to="/documents"
            className={({ isActive }) => `app-nav-item${isActive ? ' active' : ''}`}
          >
            <DocsIcon />
            Documents
          </NavLink>

          {user?.role === 'ADMIN' && (
            <NavLink
              to="/admin"
              className={({ isActive }) => `app-nav-item${isActive ? ' active' : ''}`}
            >
              <AdminIcon />
              Admin
            </NavLink>
          )}
        </nav>

        <div className="app-sidebar-footer">
          <div className="app-user-info">
            <span className="app-user-email">{user?.email}</span>
            <span className="app-user-role">{user?.role}</span>
          </div>
          <button className="app-logout-btn" onClick={handleLogout}>
            Sign out
          </button>
        </div>
      </aside>

      <main className="app-main">{children}</main>
    </div>
  )
}

const LogoIcon = () => (
  <svg width="15" height="15" viewBox="0 0 15 15" fill="none">
    <path d="M3 2.5A1.5 1.5 0 014.5 1h6L13 3.5V12.5A1.5 1.5 0 0111.5 14h-7A1.5 1.5 0 013 12.5V2.5z"
      stroke="currentColor" strokeWidth="1.2" fill="none"/>
    <path d="M10.5 1v3h3" stroke="currentColor" strokeWidth="1.2"/>
    <path d="M5 7.5h5M5 9.5h3.5" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round"/>
  </svg>
)

const ChatIcon = () => (
  <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
    <path d="M1.5 2.5A1 1 0 012.5 1.5h9a1 1 0 011 1v6a1 1 0 01-1 1H8L5 12.5V8.5H2.5a1 1 0 01-1-1v-5z"
      stroke="currentColor" strokeWidth="1.2" fill="none" strokeLinejoin="round"/>
  </svg>
)

const DocsIcon = () => (
  <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
    <rect x="1.5" y="1.5" width="11" height="11" rx="2" stroke="currentColor" strokeWidth="1.2" fill="none"/>
    <path d="M4 5h6M4 7.5h6M4 10h4" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round"/>
  </svg>
)

const AdminIcon = () => (
  <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
    <circle cx="7" cy="7" r="5.5" stroke="currentColor" strokeWidth="1.2"/>
    <circle cx="7" cy="7" r="2.5" stroke="currentColor" strokeWidth="1.2"/>
    <path d="M7 1.5v1M7 11.5v1M1.5 7h1M11.5 7h1" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round"/>
  </svg>
)