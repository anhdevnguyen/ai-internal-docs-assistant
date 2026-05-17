import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import AdminLayout from '../../layouts/AdminLayout'
import { adminApi } from '../../api/adminApi'
import { useAuth } from '../../hooks/useAuth'

const PAGE_SIZE = 20

function formatDate(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString('en-US', {
    year: 'numeric', month: 'short', day: 'numeric',
  })
}

export default function AdminUsersPage() {
  const [page, setPage] = useState(0)
  const { user: currentUser } = useAuth()
  const queryClient = useQueryClient()

  const { data: pagedData, isLoading, isError } = useQuery({
    queryKey: ['admin', 'users', page],
    queryFn: () => adminApi.listUsers(page, PAGE_SIZE),
    select: (res) => res.data.data,
  })

  const { mutate: toggleActive, isPending: isToggling, variables: togglingUserId } = useMutation({
    mutationFn: ({ userId, isActive }) =>
      isActive ? adminApi.deactivateUser(userId) : adminApi.activateUser(userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'users'] })
      queryClient.invalidateQueries({ queryKey: ['admin', 'overview'] })
    },
  })

  const users = pagedData?.content ?? []
  const totalPages = pagedData?.totalPages ?? 1
  const totalElements = pagedData?.totalElements ?? 0

  return (
    <AdminLayout>
      <div className="admin-page">
        <h1 className="admin-page-title">Users</h1>

        <div className="docs-table-wrapper">
          {isLoading ? (
            <AdminUsersSkeleton />
          ) : isError ? (
            <p className="admin-error-msg">Failed to load users.</p>
          ) : users.length === 0 ? (
            <p className="admin-empty-hint">No users found.</p>
          ) : (
            <>
              <table className="docs-table admin-table">
                <thead>
                  <tr>
                    <th>Email</th>
                    <th>Role</th>
                    <th>Joined</th>
                    <th>Status</th>
                    <th style={{ width: 120 }} />
                  </tr>
                </thead>
                <tbody>
                  {users.map((u) => {
                    const isSelf = u.id === currentUser?.id
                    const isPending = isToggling && togglingUserId?.userId === u.id

                    return (
                      <tr key={u.id} className={!u.isActive ? 'admin-user-row--inactive' : ''}>
                        <td>
                          <span className="docs-table-link">{u.email}</span>
                          {isSelf && <span className="admin-self-badge">You</span>}
                        </td>
                        <td>
                          <span className={`admin-role-chip admin-role-chip--${u.role.toLowerCase()}`}>
                            {u.role}
                          </span>
                        </td>
                        <td className="docs-table-meta">{formatDate(u.createdAt)}</td>
                        <td>
                          <span className={`admin-active-badge ${u.isActive ? 'active' : 'inactive'}`}>
                            {u.isActive ? 'Active' : 'Inactive'}
                          </span>
                        </td>
                        <td>
                          {/*
                           * Prevent admin from deactivating their own account — would lock them out.
                           * Backend also enforces tenant isolation but self-lockout is a UX concern only.
                           */}
                          {!isSelf && (
                            <button
                              className={`admin-toggle-btn ${u.isActive ? 'admin-toggle-btn--deactivate' : 'admin-toggle-btn--activate'}`}
                              onClick={() => toggleActive({ userId: u.id, isActive: u.isActive })}
                              disabled={isPending}
                            >
                              {isPending
                                ? '…'
                                : u.isActive
                                ? 'Deactivate'
                                : 'Activate'}
                            </button>
                          )}
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>

              {totalPages > 1 && (
                <div className="docs-pagination">
                  <span>{totalElements} users</span>
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
    </AdminLayout>
  )
}

function AdminUsersSkeleton() {
  return (
    <div className="docs-skeleton-wrapper">
      {[65, 75, 55, 70].map((w, i) => (
        <div key={i} className="docs-skeleton-row">
          <div className="skeleton" style={{ height: 13, width: `${w}%`, maxWidth: 260 }} />
          <div className="skeleton" style={{ height: 13, width: 50 }} />
          <div className="skeleton" style={{ height: 13, width: 80 }} />
          <div className="skeleton" style={{ height: 13, width: 55 }} />
          <div className="skeleton" style={{ height: 13, width: 80 }} />
        </div>
      ))}
    </div>
  )
}