import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react'

/**
 * Centralised toast notification system.
 *
 * Why centralised: scattered per-component toast state leads to z-index conflicts,
 * duplicate renders, and no single exit point for programmatic dismissal.
 * One Context + one Portal renders cleanly above everything.
 *
 * Usage:
 *   const { toast } = useToast()
 *   toast.success('Document uploaded')
 *   toast.error('Upload failed')
 *   toast.info('Processing...')
 */

const ToastContext = createContext(null)

const DURATION_MS = 4000

let _idCounter = 0

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])
  const timersRef = useRef({})

  const dismiss = useCallback((id) => {
    clearTimeout(timersRef.current[id])
    delete timersRef.current[id]
    setToasts((prev) => prev.filter((t) => t.id !== id))
  }, [])

  const add = useCallback((type, message) => {
    const id = ++_idCounter
    setToasts((prev) => [...prev.slice(-4), { id, type, message }])

    timersRef.current[id] = setTimeout(() => dismiss(id), DURATION_MS)
    return id
  }, [dismiss])

  // Cleanup on unmount
  useEffect(() => {
    const timers = timersRef.current
    return () => Object.values(timers).forEach(clearTimeout)
  }, [])

  const toast = {
    success: (msg) => add('success', msg),
    error:   (msg) => add('error', msg),
    info:    (msg) => add('info', msg),
  }

  return (
    <ToastContext.Provider value={{ toast }}>
      {children}
      <ToastList toasts={toasts} onDismiss={dismiss} />
    </ToastContext.Provider>
  )
}

export function useToast() {
  const ctx = useContext(ToastContext)
  if (!ctx) throw new Error('useToast must be used inside <ToastProvider>')
  return ctx
}

function ToastList({ toasts, onDismiss }) {
  if (toasts.length === 0) return null

  return (
    <div className="toast-list" role="region" aria-label="Notifications" aria-live="polite">
      {toasts.map((t) => (
        <ToastItem key={t.id} toast={t} onDismiss={onDismiss} />
      ))}
    </div>
  )
}

function ToastItem({ toast, onDismiss }) {
  const Icon = ICONS[toast.type]
  return (
    <div className={`toast toast--${toast.type}`} role="alert">
      <span className="toast-icon"><Icon /></span>
      <span className="toast-message">{toast.message}</span>
      <button
        className="toast-close"
        onClick={() => onDismiss(toast.id)}
        aria-label="Dismiss"
      >
        ×
      </button>
    </div>
  )
}

const ICONS = {
  success: () => (
    <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
      <circle cx="7" cy="7" r="6" stroke="currentColor" strokeWidth="1.2" />
      <path d="M4.5 7l2 2 3-3" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  ),
  error: () => (
    <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
      <circle cx="7" cy="7" r="6" stroke="currentColor" strokeWidth="1.2" />
      <path d="M7 4.5v3M7 9.5v.5" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" />
    </svg>
  ),
  info: () => (
    <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
      <circle cx="7" cy="7" r="6" stroke="currentColor" strokeWidth="1.2" />
      <path d="M7 6.5v4M7 4.5v.5" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" />
    </svg>
  ),
}