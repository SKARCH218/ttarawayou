'use client'

import { ChevronRight } from 'lucide-react'
import { type ReactNode } from 'react'

export interface BreadcrumbItem {
  label: ReactNode
  href?: string
  onClick?: () => void
}

export interface OGQBreadcrumbProps {
  items: BreadcrumbItem[]
  separator?: ReactNode
  className?: string
}

export function OGQBreadcrumb({ items, separator, className = '' }: OGQBreadcrumbProps) {
  return (
    <nav className={`flex items-center gap-1 ${className}`} aria-label="Breadcrumb">
      {items.map((item, i) => {
        const isLast = i === items.length - 1
        return (
          <span key={i} className="flex items-center gap-1">
            {i > 0 && (separator || <ChevronRight size={12} strokeWidth={1.5} className="text-gray-300" />)}
            {isLast ? (
              <span className="text-[13px] font-medium" style={{ color: 'var(--ogq-color-mono-900,#262d2e)' }}>{item.label}</span>
            ) : item.href ? (
              <a href={item.href} className="text-[13px] hover:underline" style={{ color: 'var(--ogq-color-mono-400,#7d8d92)' }}>{item.label}</a>
            ) : (
              <button onClick={item.onClick} className="text-[13px] hover:underline" style={{ color: 'var(--ogq-color-mono-400,#7d8d92)' }}>{item.label}</button>
            )}
          </span>
        )
      })}
    </nav>
  )
}
