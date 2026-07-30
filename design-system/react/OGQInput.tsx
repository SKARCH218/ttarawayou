'use client'

import { forwardRef, type InputHTMLAttributes, type ReactNode } from 'react'

export type InputSize = 'sm' | 'md' | 'lg'

export interface OGQInputProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'size'> {
  label?: string
  helperText?: string
  error?: string
  size?: InputSize
  leftIcon?: ReactNode
  rightIcon?: ReactNode
  fullWidth?: boolean
}

const sizeMap: Record<InputSize, { input: string; label: string }> = {
  sm: { input: 'px-3 py-1.5 text-[12px]', label: 'text-[11px] mb-1' },
  md: { input: 'px-3.5 py-2 text-[14px]', label: 'text-[12px] mb-1.5' },
  lg: { input: 'px-4 py-2.5 text-[16px]', label: 'text-[13px] mb-2' },
}

export const OGQInput = forwardRef<HTMLInputElement, OGQInputProps>(
  ({ label, helperText, error, size = 'md', leftIcon, rightIcon, fullWidth, className = '', style, ...props }, ref) => {
    const hasError = !!error
    const s = sizeMap[size]

    return (
      <div className={fullWidth ? 'w-full' : 'inline-block'} style={style}>
        {label && (
          <label className={`block font-medium ${s.label}`} style={{ color: 'var(--ogq-color-mono-700,#57676b)' }}>
            {label}
          </label>
        )}
        <div className="relative">
          {leftIcon && (
            <span className="absolute left-3 top-1/2 -translate-y-1/2" style={{ color: 'var(--ogq-color-mono-400,#7d8d92)' }}>
              {leftIcon}
            </span>
          )}
          <input
            ref={ref}
            className={`block rounded-[var(--ogq-radius-md,8px)] border transition-colors outline-none ${s.input} ${leftIcon ? 'pl-10' : ''} ${rightIcon ? 'pr-10' : ''} ${fullWidth ? 'w-full' : ''} ${className}`}
            style={{
              borderColor: hasError ? 'var(--ogq-color-semantic-error-default,#e21235)' : 'var(--ogq-color-mono-100,#d8dfdf)',
              backgroundColor: 'var(--ogq-color-mono-000,#ffffff)',
              color: 'var(--ogq-color-mono-900,#262d2e)',
            }}
            {...props}
          />
          {rightIcon && (
            <span className="absolute right-3 top-1/2 -translate-y-1/2" style={{ color: 'var(--ogq-color-mono-400,#7d8d92)' }}>
              {rightIcon}
            </span>
          )}
        </div>
        {(error || helperText) && (
          <p className="mt-1 text-[11px]" style={{ color: hasError ? 'var(--ogq-color-semantic-error-default,#e21235)' : 'var(--ogq-color-mono-400,#7d8d92)' }}>
            {error || helperText}
          </p>
        )}
      </div>
    )
  }
)
OGQInput.displayName = 'OGQInput'
