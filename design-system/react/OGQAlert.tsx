'use client'
import type { ReactNode } from 'react'

export type AlertType = 'success' | 'error' | 'warning' | 'info'

export interface OGQAlertProps {
  type?: AlertType
  title?: string
  children: ReactNode
  onClose?: () => void
  className?: string
}

const styles: Record<AlertType, { bg: string; border: string; icon: string }> = {
  success: { bg: '#dff4ea', border: '#9bdfcc', icon: '✓' },
  error: { bg: '#ffe2e4', border: '#ffd0d3', icon: '✕' },
  warning: { bg: '#fff3e0', border: '#ffe0b2', icon: '!' },
  info: { bg: '#e6f4ff', border: '#bfe0fd', icon: 'i' },
}

export function OGQAlert({ type = 'info', title, children, onClose, className = '' }: OGQAlertProps) {
  const s = styles[type]
  return (
    <div className={`flex gap-3 px-4 py-3 rounded-[var(--ogq-radius-lg,12px)] ${className}`}
      style={{ backgroundColor: s.bg, border: `1px solid ${s.border}` }} role="alert">
      <span className="w-5 h-5 rounded-full flex items-center justify-center text-[11px] font-bold flex-shrink-0 mt-0.5" style={{ backgroundColor: s.border }}>{s.icon}</span>
      <div className="flex-1 min-w-0">
        {title && <p className="text-[14px] font-semibold mb-0.5" style={{ color: 'var(--ogq-color-mono-900,#262d2e)' }}>{title}</p>}
        <div className="text-[13px]" style={{ color: 'var(--ogq-color-mono-700,#57676b)' }}>{children}</div>
      </div>
      {onClose && <button onClick={onClose} className="text-[16px] opacity-40 hover:opacity-70 flex-shrink-0" aria-label="Close">×</button>}
    </div>
  )
}
