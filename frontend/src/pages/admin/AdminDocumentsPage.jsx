import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import AdminLayout from '../../layouts/AdminLayout'
import StatusBadge from '../../components/documents/StatusBadge'
import { adminApi } from '../../api/adminApi'
import { useToast } from '../../contexts/ToastContext'

const PAGE_SIZE = 20

function formatDate(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString('en-US', {
    year: 'numeric', month: 'short', day: 'numeric',
  })
}

export default function AdminDocumentsPage() {
  const [page, setPage]          = useState(0)
  const [confirmDeleteId, setConfirmDeleteId] = useState(null)
  const queryClient = useQueryClient()
  const { toast }   = useToast()

  const { data: pagedData, isLoading, isError } = useQuery({
    queryKey: ['admin', 'documents', page],
    queryFn: () => adminApi.listDocuments(page, PAGE_SIZE),
    select: (res) => res.data.data,
  })

  const { mutate: deleteDoc, isPending: isDeleting } = useMutation({
    mutationFn: (id) => adminApi.deleteDocument(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'documents'] })
      queryClient.invalidateQueries({ queryKey: ['admin', 'overview'] })
      setConfirmDeleteId(null)
      toast.success('Document deleted')
    },
    onError: () => toast.error('Failed to delete document'),
  })

  const { mutate: reindex, isPending: isReindexing, variables: reindexingId } = useMutation({
    mutationFn: (id) => adminApi.reindexDocument(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'documents'] })
      queryClient.invalidateQueries({ queryKey: ['admin', 'overview'] })
      toast.info('Re-indexing started')
    },
    onError: () => toast.error('Failed to start re-index'),
  })

  const documents     = pagedData?.content ?? []
  const totalPages    = pagedData?.totalPages ?? 1
  const totalElements = pagedData?.totalElements ?? 0

  return (
    <AdminLayout>
      <div className="admin-page">
        <h1 className="admin-page-title">Documents</h1>

        <div className="docs-table-wrapper">
          {isLoading ? (
            <AdminTableSkeleton cols={5} />
          ) : isError ? (
            <p className="admin-error-msg">Failed to load documents.</p>
          ) : documents.length === 0 ? (
            <p className="admin-empty-hint">No documents found.</p>
          ) : (
            <>
              <table className="docs-table admin-table">
                <thead>
                  <tr>
                    <th>Title</th>
                    <th>Type</th>
                    <th>Status</th>
                    <th>Uploaded</th>
                    <th style={{ width: 180 }} />
                  </tr>
                </thead>
                <tbody>
                  {documents.map((doc) => (
                    <tr key={doc.id}>
                      <td>
                        <span className="docs-table-link">{doc.title || doc.originalFilename}</span>
                        {doc.errorMessage && (
                          <span className="admin-doc-error-hint" title={doc.errorMessage}>
                            {' '}· {doc.errorMessage.slice(0, 60)}{doc.errorMessage.length > 60 ? '…' : ''}
                          </span>
                        )}
                      </td>
                      <td className="docs-table-meta">{mimeLabel(doc.mimeType)}</td>
                      <td>
                        <StatusBadge status={doc.status} errorMessage={doc.errorMessage} />
                      </td>
                      <td className="docs-table-meta">{formatDate(doc.createdAt)}</td>
                      <td>
                        <div className="admin-action-cell">
                          {doc.status === 'FAILED' && (
                            <button
                              className="admin-action-btn admin-action-btn--retry"
                              onClick={() => reindex(doc.id)}
                              disabled={isReindexing && reindexingId === doc.id}
                            >
                              {isReindexing && reindexingId === doc.id ? 'Retrying…' : 'Retry'}
                            </button>
                          )}

                          {confirmDeleteId === doc.id ? (
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
                                onClick={() => setConfirmDeleteId(null)}
                                disabled={isDeleting}
                              >
                                Cancel
                              </button>
                            </span>
                          ) : (
                            <button
                              className="docs-table-action-btn"
                              onClick={() => setConfirmDeleteId(doc.id)}
                            >
                              Delete
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>

              {totalPages > 1 && (
                <div className="docs-pagination">
                  <span>{totalElements} documents</span>
                  <div className="docs-pagination-btns">
                    <button className="docs-page-btn" onClick={() => setPage((p) => p - 1)} disabled={page === 0}>← Prev</button>
                    <span className="docs-page-indicator">{page + 1} / {totalPages}</span>
                    <button className="docs-page-btn" onClick={() => setPage((p) => p + 1)} disabled={page >= totalPages - 1}>Next →</button>
                  </div>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </AdminLayout>
  )
}

function mimeLabel(mimeType) {
  if (!mimeType) return '—'
  if (mimeType === 'application/pdf') return 'PDF'
  if (mimeType.includes('word')) return 'DOCX'
  if (mimeType.startsWith('text/')) return 'TXT'
  return mimeType.split('/')[1]?.toUpperCase() ?? mimeType
}

function AdminTableSkeleton({ cols }) {
  return (
    <div className="docs-skeleton-wrapper">
      {[70, 50, 80, 60].map((w, i) => (
        <div key={i} className="docs-skeleton-row">
          {Array.from({ length: cols }, (_, j) => (
            <div key={j} className="skeleton"
              style={{ height: 13, width: j === 0 ? `${w}%` : 60, maxWidth: j === 0 ? 280 : 'none' }} />
          ))}
        </div>
      ))}
    </div>
  )
}