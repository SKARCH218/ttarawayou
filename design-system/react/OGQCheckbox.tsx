'use client'

import { forwardRef, type InputHTMLAttributes } from 'react'

export interface OGQCheckboxProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'type' | 'size'> {
  label?: string
  size?: 'sm' | 'md'
}

export interface OGQToggleProps {
  checked?: boolean
  onChange?: (checked: boolean) => void
  label?: string
  disabled?: boolean
  size?: 'sm' | 'md'
}

export const OGQCheckbox = forwardRef<HTMLInputElement, OGQCheckboxProps>(
  ({ label, size = 'md', className = '', ...props }, ref) => {
    const boxSize = size === 'sm' ? 16 : 20

    return (
      <label className={`inline-flex items-center gap-2 cursor-pointer ${props.disabled ? 'opacity-50 cursor-not-allowed' : ''} ${className}`}>
        <span className="relative flex items-center justify-center" style={{ width: boxSize, height: boxSize }}>
          <input ref={ref} type="checkbox" className="sr-only peer" {...props} />
          <span
            className="w-full h-full rounded-[var(--ogq-radius-sm,4px)] border-2 transition-colors peer-checked:border-transparent"
            style={{
              borderColor: 'var(--ogq-color-mono-200,#a7b6b9)',
              backgroundColor: props.checked ? 'var(--ogq-color-primary-600,#00c389)' : 'transparent',
            }}
          />
          {props.checked && (
            <svg className="absolute w-3 h-3 text-white" viewBox="0 0 12 12" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <polyline points="2 6 5 9 10 3" />
            </svg>
          )}
        </span>
        {label && <span className={`${size === 'sm' ? 'text-[12px]' : 'text-[14px]'}`} style={{ color: 'var(--ogq-color-mono-800,#425052)' }}>{label}</span>}
      </label>
    )
  }
)
OGQCheckbox.displayName = 'OGQCheckbox'

export function OGQToggle({ checked = false, onChange, label, disabled, size = 'md' }: OGQToggleProps) {
  const w = size === 'sm' ? 36 : 44
  const h = size === 'sm' ? 20 : 24
  const dot = size === 'sm' ? 16 : 20

  return (
    <label className={`inline-flex items-center gap-2 cursor-pointer ${disabled ? 'opacity-50 cursor-not-allowed' : ''}`}>
      <button
        type="button"
        role="switch"
        aria-checked={checked}
        disabled={disabled}
        onClick={() => onChange?.(!checked)}
        className="relative rounded-full transition-colors"
        style={{
          width: w, height: h,
          backgroundColor: checked ? 'var(--ogq-color-primary-600,#00c389)' : 'var(--ogq-color-mono-200,#a7b6b9)',
        }}
      >
        <span
          className="absolute top-[2px] rounded-full bg-white transition-transform shadow-sm"
          style={{
            width: dot, height: dot,
            transform: checked ? `translateX(${w - dot - 2}px)` : 'translateX(2px)',
          }}
        />
      </button>
      {label && <span className={`${size === 'sm' ? 'text-[12px]' : 'text-[14px]'}`} style={{ color: 'var(--ogq-color-mono-800,#425052)' }}>{label}</span>}
    </label>
  )
}
