'use client'
import type { ReactNode } from 'react'

export interface OGQEmptyStateProps {
  icon?: ReactNode
  title: string
  description?: string
  action?: ReactNode
  className?: string
}

export function OGQEmptyState({ icon, title, description, action, className = '' }: OGQEmptyStateProps) {
  return (
    <div className={`flex flex-col items-center justify-center py-16 px-6 text-center ${className}`}>
      {icon && <div className="mb-4 opacity-30">{icon}</div>}
      <h3 className="text-[16px] font-semibold mb-1" style={{ color: 'var(--ogq-color-mono-700,#57676b)' }}>{title}</h3>
      {description && <p className="text-[13px] max-w-xs mb-4" style={{ color: 'var(--ogq-color-mono-400,#7d8d92)' }}>{description}</p>}
      {action}
    </div>
  )
}
