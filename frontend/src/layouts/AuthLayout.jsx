import { Link } from 'react-router-dom'

function BrandPanel() {
  return (
    <div className="auth-brand">
      <div className="auth-brand-inner">
        <div className="auth-brand-logo">
          <span className="auth-logo-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round">
              <path d="M4 6h16M4 12h10M4 18h13" />
            </svg>
          </span>
          DocMind
        </div>

        <div className="auth-brand-copy">
          <h1 className="auth-brand-headline">
            Internal knowledge,<br />
            instantly.
          </h1>
          <p className="auth-brand-desc">
            Ask questions in plain language. Get answers — with sources — from your organization's documents.
          </p>
        </div>

        <ul className="auth-feature-list">
          {[
            'Semantic search across all your docs',
            'Answers with cited sources',
            'Role-based document access',
            'Audit trail on every query',
          ].map((f) => (
            <li key={f} className="auth-feature-item">
              <span className="auth-feature-dot" />
              {f}
            </li>
          ))}
        </ul>

        <p className="auth-brand-footer">© 2025 DocMind · Enterprise Knowledge Platform</p>
      </div>
    </div>
  )
}

export function AuthLayout({ children, switchHref, switchLabel, switchText }) {
  return (
    <div className="auth-root">
      <BrandPanel />

      <div className="auth-form-side">
        <div className="auth-form-box">
          {children}

          <p className="auth-switch-line">
            {switchText}{' '}
            <Link to={switchHref} className="auth-switch-link">
              {switchLabel}
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}