import { useEffect, useRef } from 'react'

/**
 * Renders the session sidebar.
 *
 * @param {Object[]} sessions     - list of { id, title, createdAt }
 * @param {string|null} activeId  - currently selected session id
 * @param {boolean} isLoading     - skeleton state on initial fetch
 * @param {Function} onSelect     - (sessionId) => void
 * @param {Function} onNewSession - () => void
 * @param {boolean} isCreating    - disable "New Chat" while a session is being created
 */
export default function SessionList({
  sessions = [],
  activeId,
  isLoading,
  onSelect,
  onNewSession,
  isCreating,
}) {
  const activeRef = useRef(null)

  // Scroll active session into view when it changes (e.g. after creation)
  useEffect(() => {
    activeRef.current?.scrollIntoView({ block: 'nearest', behavior: 'smooth' })
  }, [activeId])

  return (
    <aside className="chat-session-list">
      <div className="chat-session-list-header">
        <span className="chat-session-list-title">Conversations</span>
        <button
          className="chat-new-session-btn"
          onClick={onNewSession}
          disabled={isCreating}
          title="New conversation"
        >
          <PlusIcon />
        </button>
      </div>

      <div className="chat-session-list-body">
        {isLoading ? (
          Array.from({ length: 5 }, (_, i) => (
            <div key={i} className="chat-session-skeleton" />
          ))
        ) : sessions.length === 0 ? (
          <p className="chat-session-empty">No conversations yet.</p>
        ) : (
          sessions.map((session) => (
            <button
              key={session.id}
              ref={session.id === activeId ? activeRef : null}
              className={`chat-session-item${session.id === activeId ? ' active' : ''}`}
              onClick={() => onSelect(session.id)}
            >
              <span className="chat-session-item-title">
                {session.title ?? 'New conversation'}
              </span>
              <span className="chat-session-item-date">
                {formatRelativeTime(session.createdAt)}
              </span>
            </button>
          ))
        )}
      </div>
    </aside>
  )
}

function formatRelativeTime(isoString) {
  if (!isoString) return ''
  const diff = Date.now() - new Date(isoString).getTime()
  const minutes = Math.floor(diff / 60_000)
  if (minutes < 1) return 'just now'
  if (minutes < 60) return `${minutes}m ago`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}h ago`
  return `${Math.floor(hours / 24)}d ago`
}

const PlusIcon = () => (
  <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
    <path d="M7 2v10M2 7h10" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
  </svg>
)