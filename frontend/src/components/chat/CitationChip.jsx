import { useEffect, useRef, useState } from 'react'

/**
 * Renders a single citation chip.
 * Clicking it toggles a popover showing the raw chunk excerpt used by the AI.
 *
 * @param {number} index        - 1-based citation number shown on chip
 * @param {string} documentTitle
 * @param {string} chunkExcerpt - the actual text fragment retrieved from pgvector
 */
export default function CitationChip({ index, documentTitle, chunkExcerpt }) {
  const [open, setOpen] = useState(false)
  const ref = useRef(null)

  // Close popover when clicking outside
  useEffect(() => {
    if (!open) return
    const handler = (e) => {
      if (ref.current && !ref.current.contains(e.target)) setOpen(false)
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [open])

  return (
    <span className="citation-chip-wrapper" ref={ref}>
      <button
        className={`citation-chip${open ? ' open' : ''}`}
        onClick={() => setOpen((v) => !v)}
        title={documentTitle}
      >
        [{index}] {documentTitle}
      </button>

      {open && (
        <div className="citation-popover">
          <p className="citation-popover-label">Source: {documentTitle}</p>
          <blockquote className="citation-popover-excerpt">{chunkExcerpt}</blockquote>
        </div>
      )}
    </span>
  )
}