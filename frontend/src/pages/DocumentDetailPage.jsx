import { Link, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { documentApi } from '../api/documentApi'
import { useToast } from '../contexts/ToastContext'
import AppLayout from '../layouts/AppLayout'
import StatusBadge from '../components/documents/StatusBadge'

const ACTIVE_STATUSES = new Set(['PENDING', 'PROCESSING'])

function formatDateTime(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('en-US', {
    year: 'numeric', month: 'short', day: 'numeric',
    hour: '2-digit', minute: '2-digit',
  })
}

export default function DocumentDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { toast } = useToast()

  const { data: doc, isLoading, isError, error } = useQuery({
    queryKey: ['document', id],
    select: (res) => res.data.data,
    queryFn: () => documentApi.get(id),
    refetchInterval: (query) => {
      const status = query.state.data?.status
      return status && ACTIVE_STATUSES.has(status) ? 3000 : false
    },
    retry: (failureCount, err) => {
      if (err?.response?.status === 404) return false
      return failureCount < 3
    },
  })

  const { mutate: deleteDoc, isPending: isDeleting } = useMutation({
    mutationFn: () => documentApi.remove(id),
    onSuccess: () => {
      queryClient.removeQueries({ queryKey: ['document', id] })
      queryClient.invalidateQueries({ queryKey: ['documents'] })
      toast.success('Document deleted')
      navigate('/documents', { replace: true })
    },
    onError: () => {
      toast.error('Failed to delete document')
    },
  })

  if (isLoading) return <DetailSkeleton />

  if (isError) {
    const is404 = error?.response?.status === 404
    return (
      <AppLayout>
        <div className="app-page">
          <div className="docs-empty" style={{ marginTop: 64 }}>
            <p style={{ color: 'var(--error)', fontSize: 14 }}>
              {is404 ? 'Document not found.' : 'Failed to load document. Please try again.'}
            </p>
            <Link to="/documents" className="docs-back-link" style={{ marginTop: 12 }}>
              ← Back to documents
            </Link>
          </div>
        </div>
      </AppLayout>
    )
  }

  return (
    <AppLayout>
      <div className="app-page">
        <div className="doc-detail-header">
          <Link to="/documents" className="docs-back-link">← Documents</Link>
          <div className="doc-detail-title-row">
            <h1 className="doc-detail-title">{doc.title || doc.originalFilename}</h1>
            <StatusBadge status={doc.status} errorMessage={doc.errorMessage} />
          </div>
        </div>

        {doc.status === 'FAILED' && doc.errorMessage && (
          <div className="doc-detail-error-banner">
            <strong>Ingestion failed:</strong> {doc.errorMessage}
          </div>
        )}

        <dl className="doc-detail-meta">
          <MetaRow label="Original filename" value={doc.originalFilename} />
          <MetaRow label="Type"              value={doc.mimeType} />
          <MetaRow label="Uploaded"          value={formatDateTime(doc.createdAt)} />
          <MetaRow label="Last updated"      value={formatDateTime(doc.updatedAt)} />
        </dl>

        {ACTIVE_STATUSES.has(doc.status) && (
          <div className="doc-detail-processing-note">
            <span className="status-pulse" aria-hidden="true" />
            Ingestion in progress — this page updates automatically.
          </div>
        )}

        <div className="doc-detail-actions">
          <button
            className="docs-confirm-btn docs-confirm-btn--yes"
            style={{ padding: '7px 14px', fontSize: 13 }}
            onClick={() => {
              if (window.confirm(`Delete "${doc.title || doc.originalFilename}"? This cannot be undone.`)) {
                deleteDoc()
              }
            }}
            disabled={isDeleting}
          >
            {isDeleting ? 'Deleting…' : 'Delete document'}
          </button>
        </div>
      </div>
    </AppLayout>
  )
}

function MetaRow({ label, value }) {
  return (
    <>
      <dt className="doc-detail-meta-label">{label}</dt>
      <dd className="doc-detail-meta-value">{value || '—'}</dd>
    </>
  )
}

function DetailSkeleton() {
  return (
    <AppLayout>
      <div className="app-page">
        <div className="docs-skeleton-wrapper" style={{ gap: 16, padding: '0' }}>
          <div className="skeleton" style={{ height: 12, width: 100 }} />
          <div className="skeleton" style={{ height: 24, width: '60%', maxWidth: 400 }} />
          <div style={{ height: 16 }} />
          <div className="skeleton" style={{ height: 13, width: '40%', maxWidth: 260 }} />
          <div className="skeleton" style={{ height: 13, width: '35%', maxWidth: 220 }} />
          <div className="skeleton" style={{ height: 13, width: '45%', maxWidth: 300 }} />
        </div>
      </div>
    </AppLayout>
  )
}