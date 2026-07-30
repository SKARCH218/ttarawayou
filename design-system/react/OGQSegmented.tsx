'use client'

import { type ReactNode } from 'react'

export interface SegmentItem {
  key: string
  label: ReactNode
  disabled?: boolean
}

export interface OGQSegmentedProps {
  items: SegmentItem[]
  value: string
  onChange: (key: string) => void
  size?: 'sm' | 'md'
  fullWidth?: boolean
  className?: string
}

export function OGQSegmented({ items, value, onChange, size = 'md', fullWidth, className = '' }: OGQSegmentedProps) {
  const h = size === 'sm' ? 'h-8 text-[12px]' : 'h-10 text-[13px]'
  return (
    <div className={`inline-flex p-1 rounded-[var(--ogq-radius-lg,12px)] ${fullWidth ? 'w-full' : ''} ${className}`}
      style={{ backgroundColor: 'var(--ogq-color-mono-080,#eef1f1)' }} role="tablist">
      {items.map((item) => {
        const active = item.key === value
        return (
          <button key={item.key} role="tab" aria-selected={active} disabled={item.disabled}
            onClick={() => onChange(item.key)}
            className={`${h} px-4 rounded-[var(--ogq-radius-md,8px)] font-medium transition-all flex-1 ${
              active ? 'bg-white shadow-sm' : 'hover:bg-white/50'
            } ${item.disabled ? 'opacity-40 cursor-not-allowed' : 'cursor-pointer'}`}
            style={{ color: active ? 'var(--ogq-color-mono-900,#262d2e)' : 'var(--ogq-color-mono-500,#76888f)' }}>
            {item.label}
          </button>
        )
      })}
    </div>
  )
}
