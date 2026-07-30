'use client'

export interface OGQDividerProps {
  orientation?: 'horizontal' | 'vertical'
  label?: string
  className?: string
}

export function OGQDivider({ orientation = 'horizontal', label, className = '' }: OGQDividerProps) {
  if (orientation === 'vertical') {
    return <div className={`inline-block w-px self-stretch ${className}`} style={{ backgroundColor: 'var(--ogq-color-mono-100,#d8dfdf)' }} />
  }
  if (label) {
    return (
      <div className={`flex items-center gap-3 ${className}`}>
        <div className="flex-1 h-px" style={{ backgroundColor: 'var(--ogq-color-mono-100,#d8dfdf)' }} />
        <span className="text-[12px] flex-shrink-0" style={{ color: 'var(--ogq-color-mono-400,#7d8d92)' }}>{label}</span>
        <div className="flex-1 h-px" style={{ backgroundColor: 'var(--ogq-color-mono-100,#d8dfdf)' }} />
      </div>
    )
  }
  return <div className={`w-full h-px ${className}`} style={{ backgroundColor: 'var(--ogq-color-mono-100,#d8dfdf)' }} />
}
