'use client'

import type { ReactNode, HTMLAttributes } from 'react'

export interface OGQCardProps extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode
  variant?: 'default' | 'outlined' | 'elevated'
  padding?: 'none' | 'sm' | 'md' | 'lg'
  hoverable?: boolean
}

const paddingMap = { none: '', sm: 'p-3', md: 'p-5', lg: 'p-8' }

export function OGQCard({ children, variant = 'default', padding = 'md', hoverable, className = '', style, ...props }: OGQCardProps) {
  const base = `rounded-[var(--ogq-radius-xl,16px)] transition-all ${paddingMap[padding]}`
  const variantStyles: Record<string, React.CSSProperties> = {
    default: { backgroundColor: 'var(--ogq-color-mono-000,#fff)', border: '1px solid var(--ogq-color-mono-080,#eef1f1)' },
    outlined: { backgroundColor: 'transparent', border: '1px solid var(--ogq-color-mono-100,#d8dfdf)' },
    elevated: { backgroundColor: 'var(--ogq-color-mono-000,#fff)', boxShadow: 'var(--ogq-shadow-sm)' },
  }

  return (
    <div
      className={`${base} ${hoverable ? 'hover:shadow-lg hover:-translate-y-0.5 cursor-pointer' : ''} ${className}`}
      style={{ ...variantStyles[variant], ...style }}
      {...props}
    >
      {children}
    </div>
  )
}
