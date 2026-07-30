'use client'
import { useEffect, useCallback, type ReactNode } from 'react'

export interface OGQSheetProps {
  open: boolean
  onClose: () => void
  title?: string
  children: ReactNode
  position?: 'bottom' | 'right'
  className?: string
}

export function OGQSheet({ open, onClose, title, children, position = 'bottom', className = '' }: OGQSheetProps) {
  const handleKeyDown = useCallback((e: KeyboardEvent) => { if (e.key === 'Escape') onClose() }, [onClose])
  useEffect(() => {
    if (open) { document.addEventListener('keydown', handleKeyDown); document.body.style.overflow = 'hidden' }
    return () => { document.removeEventListener('keydown', handleKeyDown); document.body.style.overflow = '' }
  }, [open, handleKeyDown])

  if (!open) return null

  const posStyles = position === 'bottom'
    ? 'inset-x-0 bottom-0 rounded-t-[var(--ogq-radius-2xl,20px)] max-h-[80vh]'
    : 'inset-y-0 right-0 rounded-l-[var(--ogq-radius-2xl,20px)] w-[min(420px,90vw)]'

  return (
    <div className="fixed inset-0" style={{ zIndex: 'var(--ogq-z-modal,1300)' }}>
      <div className="absolute inset-0 bg-black/40" onClick={onClose} />
      <div className={`absolute bg-white overflow-y-auto ${posStyles} ${className}`} style={{ boxShadow: 'var(--ogq-elevation-5)' }}>
        {position === 'bottom' && <div className="flex justify-center pt-3 pb-1"><div className="w-10 h-1 rounded-full bg-gray-300" /></div>}
        {title && (
          <div className="px-5 py-3 border-b flex items-center justify-between" style={{ borderColor: 'var(--ogq-color-mono-080,#eef1f1)' }}>
            <h3 className="text-[16px] font-semibold" style={{ color: 'var(--ogq-color-mono-900,#262d2e)' }}>{title}</h3>
            <button onClick={onClose} className="text-[20px] opacity-40 hover:opacity-70">×</button>
          </div>
        )}
        <div className="p-5">{children}</div>
      </div>
    </div>
  )
}
