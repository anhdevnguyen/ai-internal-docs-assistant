import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { documentApi } from '../api/documentApi'
import AppLayout from '../layouts/AppLayout'
import StatusBadge from '../components/documents/StatusBadge'

const TERMINAL_STATUSES = new Set(['INDEXED', 'FAILED'])

function formatDateTime(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('en-US', {
    year: 'numeric', month: 'short', day: 'numeric',
    hour: '2-digit', minute: '2-digit',
  })
}

export default function DocumentDetailPage() {
  const { id }    = useParams()
  const navigate  = useNavigate()
  const queryClient = useQueryClient()
  const [confirmDelete, setConfirmDelete] = useState(false)

  const { data, isLoading, isError } = useQuery({
    queryKey: ['document', id],
    queryFn: () => documentApi.get(id),
    // Poll every 3 seconds until ingestion reaches a terminal state.
    // Stops automatically — no manual timeout needed.
    refetchInterval: (query) => {
      const status = query.state.data?.data?.data?.status
      if (!status) return 3000
      return TERMINAL_STATUSES.has(status) ? false : 3000
    },
    refetchIntervalInBackground: false,
  })

  const { mutate: deleteDoc, isPending: isDeleting } = useMutation({
    mutationFn: () => documentApi.remove(id),
    onSuccess: () => {
      // Invalidate list so it refreshes when user lands back on /
      queryClient.invalidateQueries({ queryKey: ['documents'] })
      queryClient.removeQueries({ queryKey: ['document', id] })
      navigate('/', { replace: true })
    },
  })

  if (isLoading) return <LoadingSkeleton />

  if (isError) {
    return (
      <AppLayout>
        <div className="app-page">
          <Link to="/" className="doc-detail-back">← Documents</Link>
          <p style={{ color: 'var(--error)', fontSize: 14 }}>
            Document not found or failed to load.
          </p>
        </div>
      </AppLayout>
    )
  }

  const doc = data?.data?.data

  return (
    <AppLayout>
      <div className="app-page">
        <Link to="/" className="doc-detail-back">← Documents</Link>

        <div className="doc-detail-header">
          <h1 className="doc-detail-title">{doc.title || doc.originalFilename}</h1>
          <StatusBadge status={doc.status} errorMessage={doc.errorMessage} />
        </div>

        <div className="doc-detail-card">
          {doc.title && doc.originalFilename !== doc.title && (
            <Row label="Filename" value={doc.originalFilename} />
          )}
          <Row label="Type" value={doc.mimeType || '—'} />
          <Row label="Uploaded"     value={formatDateTime(doc.createdAt)} />
          <Row label="Last updated" value={formatDateTime(doc.updatedAt)} />
          <div className="doc-detail-row">
            <span className="doc-detail-label">Status</span>
            <div>
              <StatusBadge status={doc.status} errorMessage={doc.errorMessage} />
              {(doc.status === 'PENDING' || doc.status === 'PROCESSING') && (
                <p className="doc-indexing-hint">Indexing in progress…</p>
              )}
            </div>
          </div>
        </div>

        {doc.status === 'FAILED' && doc.errorMessage && (
          <div className="doc-detail-error-box">
            <strong>Indexing failed: </strong>{doc.errorMessage}
          </div>
        )}

        <div className="doc-detail-actions">
          {confirmDelete ? (
            <>
              <button
                className="doc-action-btn doc-action-btn--danger"
                onClick={() => deleteDoc()}
                disabled={isDeleting}
              >
                {isDeleting ? 'Deleting…' : 'Confirm delete'}
              </button>
              <button
                className="doc-action-btn"
                onClick={() => setConfirmDelete(false)}
                disabled={isDeleting}
              >
                Cancel
              </button>
            </>
          ) : (
            <button
              className="doc-action-btn"
              onClick={() => setConfirmDelete(true)}
            >
              Delete document
            </button>
          )}
        </div>
      </div>
    </AppLayout>
  )
}

function Row({ label, value }) {
  return (
    <div className="doc-detail-row">
      <span className="doc-detail-label">{label}</span>
      <span className="doc-detail-value">{value}</span>
    </div>
  )
}

function LoadingSkeleton() {
  return (
    <AppLayout>
      <div className="app-page">
        <div className="skeleton" style={{ height: 13, width: 80, marginBottom: '1.5rem', borderRadius: 4 }} />
        <div className="skeleton" style={{ height: 22, width: '40%', marginBottom: '2rem', borderRadius: 4 }} />
        <div className="doc-detail-card" style={{ gap: '1.1rem' }}>
          {[55, 35, 48, 40].map((w, i) => (
            <div key={i} className="skeleton" style={{ height: 13, width: `${w}%`, borderRadius: 4 }} />
          ))}
        </div>
      </div>
    </AppLayout>
  )
}