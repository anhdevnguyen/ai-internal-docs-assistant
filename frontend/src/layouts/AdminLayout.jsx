import { NavLink, useNavigate } from 'react-router-dom'
import AppLayout from './AppLayout'

/**
 * Admin shell wraps AppLayout and adds a secondary tab bar for admin sub-sections.
 * AppLayout already handles the sidebar + role-gated nav link to /admin.
 * This layer adds the horizontal sub-nav specific to admin workflows.
 */
export default function AdminLayout({ children }) {
  return (
    <AppLayout>
      <div className="admin-root">
        <div className="admin-subnav">
          <NavLink
            to="/admin"
            end
            className={({ isActive }) => `admin-subnav-item${isActive ? ' active' : ''}`}
          >
            <GridIcon />
            Overview
          </NavLink>
          <NavLink
            to="/admin/documents"
            className={({ isActive }) => `admin-subnav-item${isActive ? ' active' : ''}`}
          >
            <DocsIcon />
            Documents
          </NavLink>
          <NavLink
            to="/admin/users"
            className={({ isActive }) => `admin-subnav-item${isActive ? ' active' : ''}`}
          >
            <UsersIcon />
            Users
          </NavLink>
        </div>

        <div className="admin-content">{children}</div>
      </div>
    </AppLayout>
  )
}

const GridIcon = () => (
  <svg width="13" height="13" viewBox="0 0 13 13" fill="none" stroke="currentColor" strokeWidth="1.4">
    <rect x="1" y="1" width="4.5" height="4.5" rx="1" />
    <rect x="7.5" y="1" width="4.5" height="4.5" rx="1" />
    <rect x="1" y="7.5" width="4.5" height="4.5" rx="1" />
    <rect x="7.5" y="7.5" width="4.5" height="4.5" rx="1" />
  </svg>
)

const DocsIcon = () => (
  <svg width="13" height="13" viewBox="0 0 13 13" fill="none" stroke="currentColor" strokeWidth="1.4">
    <path d="M2 2.5A1 1 0 013 1.5h5l2.5 2.5V10.5a1 1 0 01-1 1H3a1 1 0 01-1-1v-8z" />
    <path d="M8 1.5v3h3" strokeLinejoin="round" />
    <path d="M4 6h5M4 8h3.5" strokeLinecap="round" />
  </svg>
)

const UsersIcon = () => (
  <svg width="13" height="13" viewBox="0 0 13 13" fill="none" stroke="currentColor" strokeWidth="1.4">
    <circle cx="5" cy="4" r="2" />
    <path d="M1 11c0-2 1.8-3.5 4-3.5s4 1.5 4 3.5" strokeLinecap="round" />
    <path d="M9 5.5c1 0 2 .8 2 2.5" strokeLinecap="round" />
    <circle cx="9.5" cy="3" r="1.5" />
  </svg>
)