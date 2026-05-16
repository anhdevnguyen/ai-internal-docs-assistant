import { useEffect, useRef } from 'react'
import CitationChip from './CitationChip'

/**
 * Renders the full message history for a session.
 *
 * @param {Object[]} messages  - list of { id, role, content, citations, createdAt }
 * @param {boolean} isLoading  - true while fetching history (shows skeleton)
 * @param {boolean} isThinking - true while waiting for assistant response (shows typing indicator)
 */
export default function MessageThread({ messages = [], isLoading, isThinking }) {
  const bottomRef = useRef(null)

  // Auto-scroll to bottom whenever messages change or thinking state toggles
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, isThinking])

  if (isLoading) {
    return (
      <div className="message-thread">
        {Array.from({ length: 4 }, (_, i) => (
          <div key={i} className={`message-skeleton ${i % 2 === 0 ? 'user' : 'assistant'}`} />
        ))}
      </div>
    )
  }

  if (messages.length === 0 && !isThinking) {
    return (
      <div className="message-thread message-thread--empty">
        <div className="message-thread-empty-state">
          <BrainIcon />
          <p>Ask anything about your documents.</p>
          <span>The AI will only answer from indexed content — no hallucination.</span>
        </div>
      </div>
    )
  }

  return (
    <div className="message-thread">
      {messages.map((msg) => (
        <MessageBubble key={msg.id} message={msg} />
      ))}

      {isThinking && <TypingIndicator />}

      <div ref={bottomRef} />
    </div>
  )
}

function MessageBubble({ message }) {
  const isUser = message.role === 'USER'
  const citations = message.citations ?? []

  return (
    <div className={`message-bubble-row ${isUser ? 'user' : 'assistant'}`}>
      <div className="message-bubble">
        <p className="message-content">{message.content}</p>

        {citations.length > 0 && (
          <div className="message-citations">
            {citations.map((c, i) => (
              <CitationChip
                key={c.documentId + i}
                index={i + 1}
                documentTitle={c.documentTitle}
                chunkExcerpt={c.chunkExcerpt}
              />
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

function TypingIndicator() {
  return (
    <div className="message-bubble-row assistant">
      <div className="message-bubble typing-indicator">
        <span /><span /><span />
      </div>
    </div>
  )
}

const BrainIcon = () => (
  <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.2">
    <path d="M9.5 2a4.5 4.5 0 014.5 4.5v.5a3 3 0 010 6v.5a4.5 4.5 0 01-9 0V6.5A4.5 4.5 0 019.5 2z" />
    <path d="M14.5 2a4.5 4.5 0 014.5 4.5V14a4.5 4.5 0 01-9 0" />
    <path d="M5 10a2 2 0 000 4" />
  </svg>
)