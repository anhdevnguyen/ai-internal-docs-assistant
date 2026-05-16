const CONFIG = {
  PENDING:    { label: 'Pending',    mod: 'pending' },
  PROCESSING: { label: 'Processing', mod: 'processing' },
  INDEXED:    { label: 'Indexed',    mod: 'indexed' },
  FAILED:     { label: 'Failed',     mod: 'failed' },
}

/**
 * errorMessage is rendered as a native tooltip on FAILED badges —
 * gives the user context without cluttering the layout.
 */
export default function StatusBadge({ status, errorMessage }) {
  const { label, mod } = CONFIG[status] ?? CONFIG.PENDING

  return (
    <span
      className={`status-badge status-badge--${mod}`}
      title={status === 'FAILED' && errorMessage ? errorMessage : undefined}
    >
      {status === 'PROCESSING' && <span className="status-pulse" aria-hidden="true" />}
      {label}
    </span>
  )
}