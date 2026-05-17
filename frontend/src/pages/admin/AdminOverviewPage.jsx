import { useQuery } from '@tanstack/react-query'
import AdminLayout from '../../layouts/AdminLayout'
import { adminApi } from '../../api/adminApi'

const STATUS_CONFIG = {
  INDEXED:    { label: 'Indexed',    color: 'var(--success, #22c55e)' },
  PROCESSING: { label: 'Processing', color: 'var(--warning, #f59e0b)' },
  PENDING:    { label: 'Pending',    color: 'var(--text-muted, #6b7280)' },
  FAILED:     { label: 'Failed',     color: 'var(--error, #ef4444)' },
}

export default function AdminOverviewPage() {
  const { data: overview, isLoading, isError } = useQuery({
    queryKey: ['admin', 'overview'],
    queryFn: () => adminApi.getOverview(),
    select: (res) => res.data.data,
    // Dashboard metrics are acceptable with a short stale window
    staleTime: 60_000,
  })

  return (
    <AdminLayout>
      <div className="admin-page">
        <h1 className="admin-page-title">Overview</h1>

        {isError && (
          <p className="admin-error-msg">Failed to load metrics. Please refresh.</p>
        )}

        <div className="admin-metrics-grid">
          <MetricCard
            label="Active Users Today"
            value={overview?.activeUsersToday}
            isLoading={isLoading}
          />
          <MetricCard
            label="Chat Sessions Today"
            value={overview?.chatSessionsToday}
            isLoading={isLoading}
          />
          <MetricCard
            label="Total Users"
            value={overview?.totalUsers}
            isLoading={isLoading}
          />
          <MetricCard
            label="Indexed Documents"
            value={overview?.documentCountByStatus?.INDEXED}
            isLoading={isLoading}
          />
        </div>

        <div className="admin-section-row">
          <DocumentStatusPanel
            counts={overview?.documentCountByStatus}
            isLoading={isLoading}
          />
          <TopDocumentsPanel
            docs={overview?.topDocumentsByRetrieval}
            isLoading={isLoading}
          />
        </div>
      </div>
    </AdminLayout>
  )
}

function MetricCard({ label, value, isLoading }) {
  return (
    <div className="admin-metric-card">
      <span className="admin-metric-label">{label}</span>
      {isLoading ? (
        <div className="skeleton admin-metric-skeleton" />
      ) : (
        <span className="admin-metric-value">{value ?? 0}</span>
      )}
    </div>
  )
}

function DocumentStatusPanel({ counts, isLoading }) {
  const statuses = Object.keys(STATUS_CONFIG)
  const total = statuses.reduce((sum, s) => sum + (counts?.[s] ?? 0), 0)

  return (
    <div className="admin-panel">
      <h2 className="admin-panel-title">Documents by Status</h2>

      {isLoading ? (
        <StatusSkeleton />
      ) : (
        <div className="admin-status-list">
          {statuses.map((status) => {
            const count = counts?.[status] ?? 0
            const pct = total > 0 ? Math.round((count / total) * 100) : 0
            const cfg = STATUS_CONFIG[status]

            return (
              <div key={status} className="admin-status-row">
                <div className="admin-status-row-meta">
                  <span className="admin-status-dot" style={{ background: cfg.color }} />
                  <span className="admin-status-name">{cfg.label}</span>
                  <span className="admin-status-count">{count}</span>
                </div>
                <div className="admin-status-bar-track">
                  <div
                    className="admin-status-bar-fill"
                    style={{ width: `${pct}%`, background: cfg.color }}
                  />
                </div>
              </div>
            )
          })}
          <p className="admin-status-total">{total} total documents</p>
        </div>
      )}
    </div>
  )
}

function TopDocumentsPanel({ docs, isLoading }) {
  return (
    <div className="admin-panel">
      <h2 className="admin-panel-title">Top Documents (Last 30 days)</h2>

      {isLoading ? (
        <TopDocSkeleton />
      ) : !docs?.length ? (
        <p className="admin-empty-hint">No retrieval data yet.</p>
      ) : (
        <ol className="admin-top-docs-list">
          {docs.map((doc, i) => (
            <li key={doc.documentId} className="admin-top-doc-item">
              <span className="admin-top-doc-rank">{i + 1}</span>
              <span className="admin-top-doc-title" title={doc.title}>{doc.title}</span>
              <span className="admin-top-doc-hits">{doc.hitCount} hits</span>
            </li>
          ))}
        </ol>
      )}
    </div>
  )
}

function StatusSkeleton() {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      {[1, 2, 3, 4].map((i) => (
        <div key={i} style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
          <div className="skeleton" style={{ height: 12, width: '40%' }} />
          <div className="skeleton" style={{ height: 6, width: '100%', borderRadius: 3 }} />
        </div>
      ))}
    </div>
  )
}

function TopDocSkeleton() {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
      {[80, 65, 50, 70, 55].map((w, i) => (
        <div key={i} className="skeleton" style={{ height: 14, width: `${w}%` }} />
      ))}
    </div>
  )
}