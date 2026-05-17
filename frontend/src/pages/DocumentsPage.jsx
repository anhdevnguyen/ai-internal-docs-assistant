import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { documentApi } from '../api/documentApi'
import { useToast } from '../contexts/ToastContext'
import AppLayout from '../layouts/AppLayout'
import StatusBadge from '../components/documents/StatusBadge'
import UploadDropzone from '../components/documents/UploadDropzone'

const PAGE_SIZE = 20
const ACTIVE_STATUSES = new Set(['PENDING', 'PROCESSING'])

function formatDate(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString('en-US', {
    year: 'numeric', month: 'short', day: 'numeric',
  })
}

export default function DocumentsPage() {
  const [page, setPage]             = useState(0)
  const [showUpload, setShowUpload] = useState(false)
  const [deletingId, setDeletingId] = useState(null)
  const queryClient = useQueryClient()
  const { toast }   = useToast()

  const { data: pagedData, isLoading, isError } = useQuery({
    queryKey: ['documents', page],
    select: (res) => res.data.data,
    queryFn: () => documentApi.list(page, PAGE_SIZE),
    refetchOnWindowFocus: true,
    refetchInterval: (query) => {
      const content = query.state.data?.content ?? []
      return content.some((d) => ACTIVE_STATUSES.has(d.status)) ? 3000 : false
    },
  })

  const { mutate: deleteDoc, isPending: isDeleting } = useMutation({
    mutationFn: (id) => documentApi.remove(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['documents'] })
      setDeletingId(null)
      toast.success('Document deleted')
    },
    onError: () => {
      toast.error('Failed to delete document')
    },
  })

  const documents     = pagedData?.content ?? []
  const totalPages    = pagedData?.totalPages ?? 1
  const totalElements = pagedData?.totalElements ?? 0

  const handleUploaded = () => {
    setShowUpload(false)
    queryClient.invalidateQueries({ queryKey: ['documents', page] })
    toast.success('Document uploaded — ingestion started')
  }

  return (
    <AppLayout>
      <div className="app-page">
        <div className="docs-page-header">
          <h1 className="docs-page-title">Documents</h1>
          <button
            className="docs-upload-toggle-btn"
            onClick={() => setShowUpload((v) => !v)}
          >
            <PlusIcon />
            {showUpload ? 'Cancel' : 'Upload document'}
          </button>
        </div>

        {showUpload && (
          <div className="upload-panel">
            <p className="upload-panel-title">Upload a document</p>
            <UploadDropzone onUploaded={handleUploaded} />
          </div>
        )}

        <div className="docs-table-wrapper">
          {isLoading ? (
            <SkeletonRows />
          ) : isError ? (
            <div className="docs-empty">
              <p style={{ color: 'var(--error)', fontSize: 14 }}>
                Failed to load documents. Please refresh.
              </p>
            </div>
          ) : documents.length === 0 ? (
            <div className="docs-empty">
              <div className="docs-empty-icon"><EmptyIcon /></div>
              <p className="docs-empty-text">No documents yet. Upload one to get started.</p>
            </div>
          ) : (
            <>
              <table className="docs-table">
                <thead>
                  <tr>
                    <th>Title</th>
                    <th>Status</th>
                    <th>Uploaded</th>
                    <th style={{ width: 120 }} />
                  </tr>
                </thead>
                <tbody>
                  {documents.map((doc) => (
                    <tr key={doc.id}>
                      <td>
                        <Link to={`/documents/${doc.id}`} className="docs-table-link">
                          {doc.title || doc.originalFilename}
                        </Link>
                        {doc.title && doc.originalFilename && doc.title !== doc.originalFilename && (
                          <span className="docs-table-meta"> · {doc.originalFilename}</span>
                        )}
                      </td>
                      <td>
                        <StatusBadge status={doc.status} errorMessage={doc.errorMessage} />
                      </td>
                      <td className="docs-table-meta">{formatDate(doc.createdAt)}</td>
                      <td>
                        {deletingId === doc.id ? (
                          <span className="docs-confirm-delete">
                            <button
                              className="docs-confirm-btn docs-confirm-btn--yes"
                              onClick={() => deleteDoc(doc.id)}
                              disabled={isDeleting}
                            >
                              {isDeleting ? '…' : 'Delete'}
                            </button>
                            <button
                              className="docs-confirm-btn docs-confirm-btn--no"
                              onClick={() => setDeletingId(null)}
                              disabled={isDeleting}
                            >
                              Cancel
                            </button>
                          </span>
                        ) : (
                          <button
                            className="docs-table-action-btn"
                            onClick={() => setDeletingId(doc.id)}
                          >
                            Delete
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>

              {totalPages > 1 && (
                <div className="docs-pagination">
                  <span>{totalElements} documents</span>
                  <div className="docs-pagination-btns">
                    <button
                      className="docs-page-btn"
                      onClick={() => setPage((p) => p - 1)}
                      disabled={page === 0}
                    >
                      ← Prev
                    </button>
                    <span className="docs-page-indicator">
                      {page + 1} / {totalPages}
                    </span>
                    <button
                      className="docs-page-btn"
                      onClick={() => setPage((p) => p + 1)}
                      disabled={page >= totalPages - 1}
                    >
                      Next →
                    </button>
                  </div>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </AppLayout>
  )
}

function SkeletonRows() {
  return (
    <div className="docs-skeleton-wrapper">
      {[70, 50, 80].map((w, i) => (
        <div key={i} className="docs-skeleton-row">
          <div className="skeleton" style={{ height: 13, width: `${w}%`, maxWidth: 300 }} />
          <div className="skeleton" style={{ height: 13, width: 60 }} />
          <div className="skeleton" style={{ height: 13, width: 80 }} />
        </div>
      ))}
    </div>
  )
}

const PlusIcon = () => (
  <svg width="13" height="13" viewBox="0 0 13 13" fill="none" stroke="currentColor"
    strokeWidth="2" strokeLinecap="round">
    <path d="M6.5 1v11M1 6.5h11" />
  </svg>
)

const EmptyIcon = () => (
  <svg width="38" height="38" viewBox="0 0 24 24" fill="none" stroke="currentColor"
    strokeWidth="1" strokeLinecap="round" strokeLinejoin="round"
    style={{ color: 'var(--text-muted)', margin: '0 auto' }}>
    <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z" />
    <polyline points="14 2 14 8 20 8" />
    <line x1="16" y1="13" x2="8" y2="13" />
    <line x1="16" y1="17" x2="8" y2="17" />
  </svg>
)