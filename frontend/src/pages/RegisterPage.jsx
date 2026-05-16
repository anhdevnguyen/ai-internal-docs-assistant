import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import { AuthLayout } from '../layouts/AuthLayout'

function EyeIcon({ open }) {
  return open ? (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
      <circle cx="12" cy="12" r="3" />
    </svg>
  ) : (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24M1 1l22 22" />
    </svg>
  )
}

function Field({ label, id, hint, error, children }) {
  return (
    <div className="auth-field">
      <label className="auth-label" htmlFor={id}>{label}</label>
      {children}
      {hint && !error && <p className="auth-hint">{hint}</p>}
      {error && <p className="auth-field-error" role="alert">{error}</p>}
    </div>
  )
}

const EMPTY = { tenantId: '', email: '', password: '', confirmPassword: '' }

export default function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()

  // Pre-fill từ invite link: /register?org=<tenantId>
  // Nếu có sẵn thì ẩn field — user không cần biết UUID là gì
  const orgFromUrl = searchParams.get('org') ?? ''

  const [form, setForm] = useState({ ...EMPTY, tenantId: orgFromUrl })
  const [showPw, setShowPw] = useState(false)
  const [fieldErrors, setFieldErrors] = useState({})
  const [serverError, setServerError] = useState(null)
  const [loading, setLoading] = useState(false)

  const handleChange = (e) => {
    setServerError(null)
    setFieldErrors((p) => ({ ...p, [e.target.name]: undefined }))
    setForm((p) => ({ ...p, [e.target.name]: e.target.value }))
  }

  const validate = () => {
    const errors = {}
    if (form.password.length < 8) {
      errors.password = 'Password must be at least 8 characters'
    }
    if (form.password !== form.confirmPassword) {
      errors.confirmPassword = 'Passwords do not match'
    }
    return errors
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setServerError(null)

    const errors = validate()
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors)
      return
    }

    setLoading(true)
    try {
      await register(form.tenantId.trim(), form.email.trim(), form.password)
      navigate('/', { replace: true })
    } catch (err) {
      setServerError(
        err.response?.data?.error?.message ?? 'An unexpected error occurred. Please try again.'
      )
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthLayout switchHref="/login" switchText="Already have an account?" switchLabel="Sign in">
      <div className="auth-form-header">
        <h2 className="auth-title">Create account</h2>
        <p className="auth-subtitle">Join your organization's workspace</p>
      </div>

      {serverError && (
        <div className="auth-alert" role="alert">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round">
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="8" x2="12" y2="12" />
            <line x1="12" y1="16" x2="12.01" y2="16" />
          </svg>
          {serverError}
        </div>
      )}

      <form className="auth-form" onSubmit={handleSubmit} noValidate>
        {orgFromUrl ? (
          // Đã có org từ invite link — hiển thị tên tổ chức thay vì UUID thô
          <div className="auth-org-badge">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round">
              <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
              <polyline points="9 22 9 12 15 12 15 22" />
            </svg>
            Joining organization <code>{orgFromUrl}</code>
          </div>
        ) : (
          <Field label="Organization ID" id="tenantId" hint="Paste the UUID from your admin's invite — or ask them for a link">
            <input
              id="tenantId" name="tenantId" className="auth-input" type="text"
              placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
              value={form.tenantId} onChange={handleChange}
              autoComplete="off" spellCheck={false} required
            />
          </Field>
        )}

        <Field label="Email address" id="email">
          <input
            id="email" name="email" className="auth-input" type="email"
            placeholder="you@company.com"
            value={form.email} onChange={handleChange}
            autoComplete="email" required
          />
        </Field>

        <Field label="Password" id="password" error={fieldErrors.password} hint="Minimum 8 characters">
          <div className="auth-input-group">
            <input
              id="password" name="password" className="auth-input"
              type={showPw ? 'text' : 'password'} placeholder="••••••••"
              value={form.password} onChange={handleChange}
              autoComplete="new-password" required
            />
            <button
              type="button" className="auth-eye-btn"
              onClick={() => setShowPw((v) => !v)}
              aria-label={showPw ? 'Hide password' : 'Show password'}
            >
              <EyeIcon open={showPw} />
            </button>
          </div>
        </Field>

        <Field label="Confirm password" id="confirmPassword" error={fieldErrors.confirmPassword}>
          <input
            id="confirmPassword" name="confirmPassword" className="auth-input"
            type={showPw ? 'text' : 'password'} placeholder="••••••••"
            value={form.confirmPassword} onChange={handleChange}
            autoComplete="new-password" required
          />
        </Field>

        <button className="auth-submit-btn" type="submit" disabled={loading}>
          {loading ? <span className="auth-spinner" /> : 'Create account'}
        </button>
      </form>
    </AuthLayout>
  )
}