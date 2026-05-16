import { useAuth } from '../hooks/useAuth'

export default function DashboardPage() {
  const { user, logout } = useAuth()

  return (
    <div style={{ padding: '2rem', fontFamily: 'monospace' }}>
      <p>Logged in as <strong>{user?.email}</strong> · role: {user?.role}</p>
      <button onClick={logout} style={{ marginTop: '1rem' }}>Logout</button>
    </div>
  )
}