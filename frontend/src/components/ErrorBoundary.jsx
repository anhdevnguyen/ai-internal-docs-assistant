import { Component } from 'react'

/**
 * Catches render-time exceptions anywhere in the subtree.
 * Prevents white screen of death — shows a recoverable fallback.
 *
 * Placed at app root in App.jsx so every route is covered.
 * Can also be placed around individual high-risk subtrees (e.g. ChatPage).
 */
export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props)
    this.state = { hasError: false, errorMessage: null }
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, errorMessage: error?.message ?? 'Unknown error' }
  }

  componentDidCatch(error, info) {
    // In production, pipe this to your observability stack (Sentry, Datadog, etc.)
    console.error('[ErrorBoundary] Uncaught render error:', error, info.componentStack)
  }

  handleReset = () => {
    this.setState({ hasError: false, errorMessage: null })
  }

  render() {
    if (!this.state.hasError) return this.props.children

    return (
      <div className="error-boundary-screen">
        <div className="error-boundary-card">
          <span className="error-boundary-icon">
            <WarningIcon />
          </span>
          <h2 className="error-boundary-title">Something went wrong</h2>
          <p className="error-boundary-desc">
            An unexpected error occurred. The issue has been recorded.
          </p>
          <div className="error-boundary-actions">
            <button className="error-boundary-btn error-boundary-btn--primary" onClick={this.handleReset}>
              Try again
            </button>
            <button
              className="error-boundary-btn error-boundary-btn--secondary"
              onClick={() => window.location.assign('/')}
            >
              Go to home
            </button>
          </div>
        </div>
      </div>
    )
  }
}

const WarningIcon = () => (
  <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
    <path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z" />
    <line x1="12" y1="9" x2="12" y2="13" />
    <line x1="12" y1="17" x2="12.01" y2="17" />
  </svg>
)