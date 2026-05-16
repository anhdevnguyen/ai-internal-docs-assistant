import { useRef, useState } from 'react'

/**
 * Message input with submit guard.
 *
 * Security note: the `disabled` state is the primary idempotency guard on the frontend.
 * If the user somehow bypasses it (devtools), the backend's @Transactional block and
 * the 4000-char validation will still enforce integrity.
 *
 * @param {Function} onSend   - async (content: string) => void — called only when not already sending
 * @param {boolean} disabled  - externally driven (e.g. no session selected, or session loading)
 */
export default function MessageInput({ onSend, disabled }) {
  const [value, setValue] = useState('')
  const [isSending, setIsSending] = useState(false)
  const textareaRef = useRef(null)

  const canSubmit = value.trim().length > 0 && !isSending && !disabled

  const submit = async () => {
    if (!canSubmit) return
    const content = value.trim()
    setValue('')
    // Reset textarea height after clearing
    if (textareaRef.current) textareaRef.current.style.height = 'auto'

    setIsSending(true)
    try {
      await onSend(content)
    } finally {
      // Always re-enable even if request failed — error display is MessageThread's responsibility
      setIsSending(false)
      textareaRef.current?.focus()
    }
  }

  const handleKeyDown = (e) => {
    // Shift+Enter = newline, Enter alone = submit
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      submit()
    }
  }

  const handleChange = (e) => {
    setValue(e.target.value)
    // Auto-grow textarea up to ~6 lines
    const el = e.target
    el.style.height = 'auto'
    el.style.height = `${Math.min(el.scrollHeight, 144)}px`
  }

  return (
    <div className={`message-input-bar${isSending ? ' sending' : ''}`}>
      <textarea
        ref={textareaRef}
        className="message-input-textarea"
        placeholder="Ask a question about your documents…"
        value={value}
        onChange={handleChange}
        onKeyDown={handleKeyDown}
        disabled={disabled || isSending}
        rows={1}
        maxLength={4000}
      />
      <button
        className="message-input-send-btn"
        onClick={submit}
        disabled={!canSubmit}
        title="Send (Enter)"
      >
        {isSending ? <SpinnerIcon /> : <SendIcon />}
      </button>
    </div>
  )
}

const SendIcon = () => (
  <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
    <path d="M14 8L2 2l3 6-3 6 12-6z" fill="currentColor" />
  </svg>
)

const SpinnerIcon = () => (
  <svg width="16" height="16" viewBox="0 0 16 16" fill="none" className="spin">
    <circle cx="8" cy="8" r="6" stroke="currentColor" strokeWidth="1.5" strokeDasharray="25" strokeDashoffset="10" strokeLinecap="round" />
  </svg>
)