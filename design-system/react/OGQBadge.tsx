'use client'

import type { ReactNode } from 'react'

export type BadgeVariant = 'default' | 'capsule' | 'status' | 'outline'
export type BadgeColor = 'primary' | 'secondary' | 'mono' | 'error' | 'warning' | 'success' | 'info'
export type BadgeSize = 'sm' | 'md'

export interface OGQBadgeProps {
  variant?: BadgeVariant
  color?: BadgeColor
  size?: BadgeSize
  children: ReactNode
  icon?: ReactNode
  className?: string
}

const colorStyles: Record<BadgeColor, { bg: string; text: string; border: string }> = {
  primary: { bg: 'var(--ogq-color-primary-050,#dff4ea)', text: 'var(--ogq-color-primary-800,#00996e)', border: 'var(--ogq-color-primary-200,#9bdfcc)' },
  secondary: { bg: 'var(--ogq-color-secondary-050,#ebe8fc)', text: 'var(--ogq-color-secondary-700,#5b1dcd)', border: 'var(--ogq-color-secondary-200,#bfb0f2)' },
  mono: { bg: 'var(--ogq-color-mono-080,#eef1f1)', text: 'var(--ogq-color-mono-700,#57676b)', border: 'var(--ogq-color-mono-200,#a7b6b9)' },
  error: { bg: '#ffe2e4', text: '#e21235', border: '#ffd0d3' },
  warning: { bg: '#fff3e0', text: '#e65100', border: '#ffe0b2' },
  success: { bg: '#dff4ea', text: '#007a58', border: '#9bdfcc' },
  info: { bg: '#e6f4ff', text: '#1384d7', border: '#bfe0fd' },
}

const sizeStyles: Record<BadgeSize, string> = {
  sm: 'px-2 py-0.5 text-[10px]',
  md: 'px-2.5 py-1 text-[12px]',
}

export function OGQBadge({ variant = 'default', color = 'primary', size = 'md', children, icon, className = '' }: OGQBadgeProps) {
  const cs = colorStyles[color]
  const isCapsule = variant === 'capsule'
  const isOutline = variant === 'outline'

  return (
    <span
      className={`inline-flex items-center gap-1 font-medium ${sizeStyles[size]} ${className}`}
      style={{
        backgroundColor: isOutline ? 'transparent' : cs.bg,
        color: cs.text,
        border: isOutline ? `1px solid ${cs.border}` : '1px solid transparent',
        borderRadius: isCapsule ? 'var(--ogq-radius-full,1000px)' : 'var(--ogq-radius-sm,4px)',
      }}
    >
      {icon}
      {children}
    </span>
  )
}
