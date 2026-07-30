'use client'

import { useEffect, type ReactNode } from 'react'

export type ToastType = 'success' | 'error' | 'warning' | 'info'

export interface OGQToastProps {
  open: boolean
  onClose: () => void
  type?: ToastType
  message: ReactNode
  duration?: number
  className?: string
}

const typeStyles: Record<ToastType, { bg: string; border: string; icon: string }> = {
  success: { bg: '#dff4ea', border: '#9bdfcc', icon: '✓' },
  error: { bg: '#ffe2e4', border: '#ffd0d3', icon: '✕' },
  warning: { bg: '#fff3e0', border: '#ffe0b2', icon: '!' },
  info: { bg: '#e6f4ff', border: '#bfe0fd', icon: 'i' },
}

export function OGQToast({ open, onClose, type = 'info', message, duration = 3000, className = '' }: OGQToastProps) {
  useEffect(() => {
    if (open && duration > 0) {
      const timer = setTimeout(onClose, duration)
      return () => clearTimeout(timer)
    }
  }, [open, duration, onClose])

  if (!open) return null

  const ts = typeStyles[type]

  return (
    <div
      className={`fixed top-4 right-4 flex items-center gap-3 px-4 py-3 rounded-[var(--ogq-radius-lg,12px)] shadow-lg animate-in slide-in-from-top ${className}`}
      style={{ zIndex: 'var(--ogq-z-toast,1500)', backgroundColor: ts.bg, border: `1px solid ${ts.border}` }}
      role="alert"
    >
      <span className="w-5 h-5 rounded-full flex items-center justify-center text-[11px] font-bold" style={{ backgroundColor: ts.border }}>
        {ts.icon}
      </span>
      <span className="text-[14px]" style={{ color: 'var(--ogq-color-mono-900,#262d2e)' }}>{message}</span>
      <button onClick={onClose} className="ml-2 text-[16px] opacity-50 hover:opacity-100" aria-label="Close">×</button>
    </div>
  )
}
