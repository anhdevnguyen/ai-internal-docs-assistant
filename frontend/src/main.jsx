import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import './index.css'
import './admin.css'
import App from './App.jsx'

/**
 * Single QueryClient instance at root — shared across the entire app.
 * defaultOptions tuned for this app's UX contract:
 * - retry: 1 on network errors (not for 4xx — those are domain errors, not transient)
 * - staleTime: 0 (default) — most data here is user-action-driven, not ambient background data
 * - refetchOnWindowFocus: true only where polling is explicitly needed (configured per-query)
 */
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: (failureCount, error) => {
        // Never retry client errors — 4xx means the request itself is wrong
        const status = error?.response?.status
        if (status >= 400 && status < 500) return false
        return failureCount < 1
      },
      refetchOnWindowFocus: false,
      staleTime: 0,
    },
  },
})

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <App />
    </QueryClientProvider>
  </StrictMode>,
)