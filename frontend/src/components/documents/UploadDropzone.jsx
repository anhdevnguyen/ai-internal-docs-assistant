import { useCallback, useRef, useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { documentApi } from '../../api/documentApi'

const ACCEPTED_EXTENSIONS = ['.pdf', '.docx', '.txt']

function validateFile(file) {
  const ext = `.${file.name.split('.').pop().toLowerCase()}`
  if (!ACCEPTED_EXTENSIONS.includes(ext)) {
    return `Unsupported type. Accepted: ${ACCEPTED_EXTENSIONS.join(', ')}`
  }
  if (file.size === 0) return 'File is empty.'
  if (file.size > 50 * 1024 * 1024) return 'File exceeds 50 MB limit.'
  return null
}

export default function UploadDropzone({ onUploaded }) {
  const [isDragging, setIsDragging]     = useState(false)
  const [selectedFile, setSelectedFile] = useState(null)
  const [title, setTitle]               = useState('')
  const [progress, setProgress]         = useState(0)
  const [fileError, setFileError]       = useState(null)
  const inputRef  = useRef(null)
  const queryClient = useQueryClient()

  const { mutate, isPending, error: mutationError, reset: resetMutation } = useMutation({
    mutationFn: () =>
      documentApi.upload(selectedFile, title, (e) => {
        setProgress(Math.round((e.loaded / e.total) * 100))
      }),
    onSuccess: ({ data: resp }) => {
      queryClient.invalidateQueries({ queryKey: ['documents'] })
      onUploaded?.(resp.data)
    },
    onError: () => setProgress(0),
  })

  const commitFile = useCallback((file) => {
    const err = validateFile(file)
    if (err) { setFileError(err); return }
    setFileError(null)
    resetMutation()
    setSelectedFile(file)
    // Pre-fill title with filename (minus extension) so user doesn't have to type it
    setTitle(file.name.replace(/\.[^.]+$/, ''))
  }, [resetMutation])

  const clearFile = () => {
    setSelectedFile(null)
    setTitle('')
    setProgress(0)
    setFileError(null)
    resetMutation()
  }

  const onDragOver  = (e) => { e.preventDefault(); setIsDragging(true) }
  const onDragLeave = () => setIsDragging(false)
  const onDrop      = (e) => {
    e.preventDefault()
    setIsDragging(false)
    const file = e.dataTransfer.files[0]
    if (file) commitFile(file)
  }

  const onInputChange = (e) => {
    const file = e.target.files[0]
    if (file) commitFile(file)
    // Reset input so the same file can be re-selected after removal
    e.target.value = ''
  }

  const apiError = mutationError?.response?.data?.error?.message
    ?? (mutationError ? 'Upload failed. Please try again.' : null)

  return (
    <div className="upload-zone-wrapper">
      {/* Drop / browse area */}
      <div
        className={[
          'upload-drop-area',
          isDragging  ? 'dragging'  : '',
          selectedFile ? 'has-file' : '',
        ].filter(Boolean).join(' ')}
        onDragOver={onDragOver}
        onDragLeave={onDragLeave}
        onDrop={onDrop}
        onClick={() => !selectedFile && !isPending && inputRef.current?.click()}
        role={!selectedFile ? 'button' : undefined}
        tabIndex={!selectedFile ? 0 : undefined}
        onKeyDown={(e) => e.key === 'Enter' && !selectedFile && inputRef.current?.click()}
      >
        <input
          ref={inputRef}
          type="file"
          accept=".pdf,.docx,.txt"
          onChange={onInputChange}
          style={{ display: 'none' }}
          aria-label="Select document to upload"
        />

        {selectedFile ? (
          <div className="upload-file-selected">
            <span className="upload-file-icon"><FileIcon /></span>
            <div className="upload-file-info">
              <span className="upload-file-name">{selectedFile.name}</span>
              <span className="upload-file-size">
                {(selectedFile.size / 1024 / 1024).toFixed(2)} MB
              </span>
            </div>
            {!isPending && (
              <button
                className="upload-remove-btn"
                onClick={(e) => { e.stopPropagation(); clearFile() }}
                aria-label="Remove selected file"
              >
                <CloseIcon />
              </button>
            )}
          </div>
        ) : (
          <div className="upload-prompt">
            <span className="upload-prompt-icon"><UploadIcon /></span>
            <p className="upload-prompt-text">
              Drag & drop or <span className="upload-browse-link">browse</span>
            </p>
            <p className="upload-prompt-hint">PDF, DOCX, TXT · max 50 MB</p>
          </div>
        )}
      </div>

      {fileError && <p className="upload-feedback upload-feedback--error">{fileError}</p>}
      {apiError  && <p className="upload-feedback upload-feedback--error">{apiError}</p>}

      {selectedFile && (
        <div className="upload-meta">
          <div>
            <label className="upload-label">
              Title <span className="upload-label-optional">(optional)</span>
            </label>
            <input
              className="upload-input"
              type="text"
              placeholder={selectedFile.name.replace(/\.[^.]+$/, '')}
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              disabled={isPending}
              maxLength={200}
            />
          </div>

          {isPending && (
            <div className="upload-progress-wrapper">
              <div className="upload-progress-track">
                <div className="upload-progress-fill" style={{ width: `${progress}%` }} />
              </div>
              <span className="upload-progress-label">
                {/* progress === 100 means file is uploaded but server is still indexing */}
                {progress < 100 ? `Uploading ${progress}%` : 'Indexing…'}
              </span>
            </div>
          )}

          <button
            className="upload-submit-btn"
            onClick={() => mutate()}
            disabled={isPending}
          >
            {isPending ? 'Uploading…' : 'Upload document'}
          </button>
        </div>
      )}
    </div>
  )
}

const UploadIcon = () => (
  <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor"
    strokeWidth="1.4" strokeLinecap="round" strokeLinejoin="round">
    <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/>
    <polyline points="17 8 12 3 7 8"/>
    <line x1="12" y1="3" x2="12" y2="15"/>
  </svg>
)

const FileIcon = () => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
    strokeWidth="1.4" strokeLinecap="round" strokeLinejoin="round">
    <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/>
    <polyline points="14 2 14 8 20 8"/>
  </svg>
)

const CloseIcon = () => (
  <svg width="11" height="11" viewBox="0 0 11 11" fill="none" stroke="currentColor"
    strokeWidth="1.6" strokeLinecap="round">
    <path d="M1 1l9 9M10 1L1 10"/>
  </svg>
)