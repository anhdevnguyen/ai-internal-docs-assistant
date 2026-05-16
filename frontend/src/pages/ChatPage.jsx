import { useCallback, useEffect, useRef, useState } from 'react'
import AppLayout from '../layouts/AppLayout'
import SessionList from '../components/chat/SessionList'
import MessageThread from '../components/chat/MessageThread'
import MessageInput from '../components/chat/MessageInput'
import { chatApi } from '../api/chatApi'

/**
 * Error codes mapped from GlobalExceptionHandler on the backend.
 * These drive user-facing messaging — never show raw server errors.
 */
const ERROR_MESSAGES = {
  PROVIDER_RATE_LIMIT: 'AI service is busy right now. Please wait a moment and try again.',
  LLM_COMPLETION_FAILED: 'Failed to generate a response. Please try again.',
  RESOURCE_NOT_FOUND: 'This conversation no longer exists.',
  default: 'Something went wrong. Please try again.',
}

function resolveErrorMessage(error) {
  const code = error?.response?.data?.error?.code
  return ERROR_MESSAGES[code] ?? ERROR_MESSAGES.default
}

export default function ChatPage() {
  const [sessions, setSessions] = useState([])
  const [activeSessionId, setActiveSessionId] = useState(null)
  const [messages, setMessages] = useState([])
  const [sessionsLoading, setSessionsLoading] = useState(true)
  const [messagesLoading, setMessagesLoading] = useState(false)
  const [isThinking, setIsThinking] = useState(false)
  const [isCreatingSession, setIsCreatingSession] = useState(false)
  const [sendError, setSendError] = useState(null)

  // Guard: prevent double-send if parent somehow calls onSend while already in-flight
  const inFlightRef = useRef(false)

  // ── Load sessions on mount ─────────────────────────────────────────────────
  useEffect(() => {
    let cancelled = false
    chatApi.listSessions()
      .then(({ data: resp }) => {
        if (!cancelled) setSessions(resp.data)
      })
      .catch(() => {
        // Non-fatal — user still sees empty state and can create a new session
      })
      .finally(() => {
        if (!cancelled) setSessionsLoading(false)
      })
    return () => { cancelled = true }
  }, [])

  // ── Load messages when session changes ────────────────────────────────────
  useEffect(() => {
    if (!activeSessionId) return
    let cancelled = false
    setMessagesLoading(true)
    setSendError(null)

    chatApi.listMessages(activeSessionId)
      .then(({ data: resp }) => {
        if (!cancelled) setMessages(resp.data)
      })
      .catch(() => {
        if (!cancelled) setMessages([])
      })
      .finally(() => {
        if (!cancelled) setMessagesLoading(false)
      })

    return () => { cancelled = true }
  }, [activeSessionId])

  // ── Create new session ────────────────────────────────────────────────────
  const handleNewSession = useCallback(async () => {
    if (isCreatingSession) return
    setIsCreatingSession(true)
    try {
      const { data: resp } = await chatApi.createSession()
      const newSession = resp.data
      setSessions((prev) => [newSession, ...prev])
      setActiveSessionId(newSession.id)
      setMessages([])
      setSendError(null)
    } catch {
      // If session creation fails, don't navigate — user retains current state
    } finally {
      setIsCreatingSession(false)
    }
  }, [isCreatingSession])

  // ── Send message ──────────────────────────────────────────────────────────
  const handleSend = useCallback(async (content) => {
    // Double-send guard — MessageInput's isSending state is the primary lock,
    // this ref is a secondary backstop for edge cases
    if (inFlightRef.current || !activeSessionId) return
    inFlightRef.current = true
    setSendError(null)

    // Optimistic: append user message immediately for responsive feel
    const optimisticUserMsg = {
      id: `optimistic-${Date.now()}`,
      role: 'USER',
      content,
      citations: [],
      createdAt: new Date().toISOString(),
    }
    setMessages((prev) => [...prev, optimisticUserMsg])
    setIsThinking(true)

    try {
      const { data: resp } = await chatApi.sendMessage(activeSessionId, content)
      const assistantMsg = {
        id: resp.data.messageId,
        role: 'ASSISTANT',
        content: resp.data.content,
        citations: resp.data.citations,
        createdAt: new Date().toISOString(),
      }
      setMessages((prev) => [...prev, assistantMsg])

      // Update session title if backend generated one async
      // We do a lightweight refresh of session list to pick up the new title
      chatApi.listSessions()
        .then(({ data: listResp }) => setSessions(listResp.data))
        .catch(() => {})
    } catch (error) {
      // Remove optimistic user message on failure — it was never persisted
      setMessages((prev) => prev.filter((m) => m.id !== optimisticUserMsg.id))
      setSendError(resolveErrorMessage(error))
    } finally {
      setIsThinking(false)
      inFlightRef.current = false
    }
  }, [activeSessionId])

  const handleSelectSession = useCallback((sessionId) => {
    if (sessionId === activeSessionId) return
    setActiveSessionId(sessionId)
  }, [activeSessionId])

  const noSessionSelected = !activeSessionId

  return (
    <AppLayout>
      <div className="chat-page">
        <SessionList
          sessions={sessions}
          activeId={activeSessionId}
          isLoading={sessionsLoading}
          onSelect={handleSelectSession}
          onNewSession={handleNewSession}
          isCreating={isCreatingSession}
        />

        <div className="chat-main">
          {noSessionSelected ? (
            <div className="chat-no-session">
              <p>Select a conversation or start a new one.</p>
              <button
                className="chat-start-btn"
                onClick={handleNewSession}
                disabled={isCreatingSession}
              >
                Start new conversation
              </button>
            </div>
          ) : (
            <>
              <MessageThread
                messages={messages}
                isLoading={messagesLoading}
                isThinking={isThinking}
              />

              {sendError && (
                <div className="chat-send-error" role="alert">
                  <ErrorIcon />
                  {sendError}
                  <button
                    className="chat-send-error-dismiss"
                    onClick={() => setSendError(null)}
                    aria-label="Dismiss error"
                  >
                    ×
                  </button>
                </div>
              )}

              <MessageInput
                onSend={handleSend}
                disabled={messagesLoading}
              />
            </>
          )}
        </div>
      </div>
    </AppLayout>
  )
}

const ErrorIcon = () => (
  <svg width="14" height="14" viewBox="0 0 14 14" fill="none" style={{ flexShrink: 0 }}>
    <circle cx="7" cy="7" r="6" stroke="currentColor" strokeWidth="1.2" />
    <path d="M7 4v3M7 9.5v.5" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" />
  </svg>
)