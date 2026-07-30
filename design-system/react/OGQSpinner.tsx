'use client'

export type SpinnerSize = 'sm' | 'md' | 'lg'

export interface OGQSpinnerProps {
  size?: SpinnerSize
  color?: string
  className?: string
}

const sizeMap: Record<SpinnerSize, number> = { sm: 16, md: 24, lg: 36 }

export function OGQSpinner({ size = 'md', color = 'var(--ogq-color-primary-600,#00c389)', className = '' }: OGQSpinnerProps) {
  const s = sizeMap[size]
  return (
    <svg className={`animate-spin ${className}`} width={s} height={s} viewBox="0 0 24 24" fill="none">
      <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="3" strokeDasharray="60" strokeLinecap="round" style={{ color, opacity: 0.25 }} />
      <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="3" strokeDasharray="60" strokeDashoffset="45" strokeLinecap="round" style={{ color }} />
    </svg>
  )
}
