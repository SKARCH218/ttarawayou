'use client'

import { forwardRef, type ButtonHTMLAttributes, type ReactNode } from 'react'

export type ButtonVariant = 'default' | 'outline' | 'text' | 'ghost'
export type ButtonSize = 'sm' | 'md' | 'lg'
export type ButtonColor = 'primary' | 'secondary' | 'mono' | 'error'

export interface OGQButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant
  size?: ButtonSize
  color?: ButtonColor
  loading?: boolean
  icon?: ReactNode
  iconPosition?: 'left' | 'right'
  fullWidth?: boolean
}

const sizeStyles: Record<ButtonSize, string> = {
  sm: 'px-3 py-1.5 text-[12px] gap-1.5 rounded-[var(--ogq-radius-sm,4px)]',
  md: 'px-4 py-2 text-[14px] gap-2 rounded-[var(--ogq-radius-md,8px)]',
  lg: 'px-6 py-3 text-[16px] gap-2.5 rounded-[var(--ogq-radius-lg,12px)]',
}

const colorMap = {
  primary: { bg: 'var(--ogq-color-primary-600,#00c389)', text: '#ffffff', border: 'var(--ogq-color-primary-600,#00c389)', hover: 'var(--ogq-color-primary-700,#00b57f)' },
  secondary: { bg: 'var(--ogq-color-secondary-600,#703fe4)', text: '#ffffff', border: 'var(--ogq-color-secondary-600,#703fe4)', hover: 'var(--ogq-color-secondary-700,#5b1dcd)' },
  mono: { bg: 'var(--ogq-color-mono-900,#262d2e)', text: '#ffffff', border: 'var(--ogq-color-mono-900,#262d2e)', hover: 'var(--ogq-color-mono-800,#425052)' },
  error: { bg: 'var(--ogq-color-semantic-error-default,#e21235)', text: '#ffffff', border: 'var(--ogq-color-semantic-error-default,#e21235)', hover: 'var(--ogq-color-semantic-error-dark,#c90e2e)' },
}

export const OGQButton = forwardRef<HTMLButtonElement, OGQButtonProps>(
  ({ variant = 'default', size = 'md', color = 'primary', loading, icon, iconPosition = 'left', fullWidth, className = '', children, disabled, style, ...props }, ref) => {
    const cm = colorMap[color]
    const isDisabled = disabled || loading

    const variantStyles: Record<ButtonVariant, React.CSSProperties> = {
      default: { backgroundColor: cm.bg, color: cm.text, border: '1px solid transparent' },
      outline: { backgroundColor: 'transparent', color: cm.bg, border: `1px solid ${cm.border}` },
      text: { backgroundColor: 'transparent', color: cm.bg, border: '1px solid transparent' },
      ghost: { backgroundColor: 'transparent', color: 'var(--ogq-color-mono-700,#57676b)', border: '1px solid transparent' },
    }

    return (
      <button
        ref={ref}
        disabled={isDisabled}
        className={`inline-flex items-center justify-center font-medium transition-all ${sizeStyles[size]} ${fullWidth ? 'w-full' : ''} ${isDisabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'} ${className}`}
        style={{ ...variantStyles[variant], ...style }}
        {...props}
      >
        {loading && (
          <svg className="animate-spin h-4 w-4" viewBox="0 0 24 24" fill="none">
            <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="3" strokeDasharray="60" strokeLinecap="round" />
          </svg>
        )}
        {!loading && icon && iconPosition === 'left' && icon}
        {children}
        {!loading && icon && iconPosition === 'right' && icon}
      </button>
    )
  }
)
OGQButton.displayName = 'OGQButton'
