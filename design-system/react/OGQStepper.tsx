'use client'

export interface OGQStepperProps {
  value: number
  onChange: (value: number) => void
  min?: number
  max?: number
  step?: number
  label?: string
  disabled?: boolean
  size?: 'sm' | 'md'
  className?: string
}

export function OGQStepper({ value, onChange, min = 0, max = 100, step = 1, label, disabled, size = 'md', className = '' }: OGQStepperProps) {
  const s = size === 'sm' ? { h: 28, btn: 28, font: '12px' } : { h: 36, btn: 36, font: '14px' }
  const canDec = value > min && !disabled
  const canInc = value < max && !disabled

  return (
    <div className={`inline-flex items-center gap-2 ${className}`}>
      {label && <span className="text-[13px] font-medium mr-1" style={{ color: 'var(--ogq-color-mono-700,#57676b)' }}>{label}</span>}
      <div className="inline-flex items-center rounded-[var(--ogq-radius-md,8px)] border overflow-hidden"
        style={{ borderColor: 'var(--ogq-color-mono-100,#d8dfdf)', height: s.h }}>
        <button onClick={() => canDec && onChange(Math.max(min, value - step))} disabled={!canDec}
          className="flex items-center justify-center border-r transition-colors"
          style={{ width: s.btn, height: s.h, borderColor: 'var(--ogq-color-mono-100,#d8dfdf)', opacity: canDec ? 1 : 0.3, cursor: canDec ? 'pointer' : 'not-allowed' }}>
          <span className="text-[16px]" style={{ color: 'var(--ogq-color-mono-600,#6c7e84)' }}>−</span>
        </button>
        <span className="px-3 font-mono font-medium min-w-[40px] text-center" style={{ fontSize: s.font, color: 'var(--ogq-color-mono-900,#262d2e)' }}>
          {value}
        </span>
        <button onClick={() => canInc && onChange(Math.min(max, value + step))} disabled={!canInc}
          className="flex items-center justify-center border-l transition-colors"
          style={{ width: s.btn, height: s.h, borderColor: 'var(--ogq-color-mono-100,#d8dfdf)', opacity: canInc ? 1 : 0.3, cursor: canInc ? 'pointer' : 'not-allowed' }}>
          <span className="text-[16px]" style={{ color: 'var(--ogq-color-mono-600,#6c7e84)' }}>+</span>
        </button>
      </div>
    </div>
  )
}
