'use client'

import { useEffect, useCallback, type ReactNode } from 'react'

export interface OGQModalProps {
  open: boolean
  onClose: () => void
  title?: string
  children: ReactNode
  footer?: ReactNode
  size?: 'sm' | 'md' | 'lg' | 'full'
  className?: string
}

const sizeMap = {
  sm: 'max-w-sm',
  md: 'max-w-lg',
  lg: 'max-w-2xl',
  full: 'max-w-[calc(100vw-2rem)] max-h-[calc(100vh-2rem)]',
}

export function OGQModal({ open, onClose, title, children, footer, size = 'md', className = '' }: OGQModalProps) {
  const handleKeyDown = useCallback((e: KeyboardEvent) => {
    if (e.key === 'Escape') onClose()
  }, [onClose])

  useEffect(() => {
    if (open) {
      document.addEventListener('keydown', handleKeyDown)
      document.body.style.overflow = 'hidden'
    }
    return () => {
      document.removeEventListener('keydown', handleKeyDown)
      document.body.style.overflow = ''
    }
  }, [open, handleKeyDown])

  if (!open) return null

  return (
    <div
      className="fixed inset-0 flex items-center justify-center p-4"
      style={{ zIndex: 'var(--ogq-z-modal,1300)' }}
    >
      <div
        className="absolute inset-0 bg-black/50 transition-opacity"
        style={{ backdropFilter: 'blur(4px)' }}
        onClick={onClose}
      />
      <div
        className={`relative bg-white w-full ${sizeMap[size]} rounded-[var(--ogq-radius-2xl,20px)] overflow-hidden ${className}`}
        style={{ boxShadow: 'var(--ogq-elevation-5, 0px 25px 50px -12px rgba(0,0,0,0.25))' }}
      >
        {title && (
          <div className="px-6 py-4 border-b" style={{ borderColor: 'var(--ogq-color-mono-080,#eef1f1)' }}>
            <h2 className="text-[18px] font-semibold" style={{ color: 'var(--ogq-color-mono-900,#262d2e)' }}>
              {title}
            </h2>
          </div>
        )}
        <div className="px-6 py-5">{children}</div>
        {footer && (
          <div className="px-6 py-4 border-t flex justify-end gap-2" style={{ borderColor: 'var(--ogq-color-mono-080,#eef1f1)' }}>
            {footer}
          </div>
        )}
      </div>
    </div>
  )
}
