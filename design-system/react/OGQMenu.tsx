'use client'

import { useState, useRef, useEffect, type ReactNode } from 'react'

export interface MenuItem {
  key: string
  label: ReactNode
  icon?: ReactNode
  disabled?: boolean
  danger?: boolean
  divider?: boolean
}

export interface OGQMenuProps {
  trigger: ReactNode
  items: MenuItem[]
  onSelect: (key: string) => void
  align?: 'left' | 'right'
  className?: string
}

export function OGQMenu({ trigger, items, onSelect, align = 'left', className = '' }: OGQMenuProps) {
  const [open, setOpen] = useState(false)
  const ref = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false)
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [])

  return (
    <div ref={ref} className={`relative inline-block ${className}`}>
      <div onClick={() => setOpen(!open)} className="cursor-pointer">{trigger}</div>
      {open && (
        <div className={`absolute mt-1 py-1 min-w-[180px] bg-white rounded-[var(--ogq-radius-lg,12px)] border overflow-hidden ${align === 'right' ? 'right-0' : 'left-0'}`}
          style={{ borderColor: 'var(--ogq-color-mono-080,#eef1f1)', boxShadow: 'var(--ogq-shadow-lg)', zIndex: 'var(--ogq-z-dropdown,1000)' }}>
          {items.map((item) => {
            if (item.divider) return <div key={item.key} className="my-1 h-px" style={{ backgroundColor: 'var(--ogq-color-mono-080,#eef1f1)' }} />
            return (
              <button key={item.key} disabled={item.disabled}
                onClick={() => { onSelect(item.key); setOpen(false) }}
                className={`w-full flex items-center gap-2.5 px-3 py-2 text-[13px] text-left transition-colors ${item.disabled ? 'opacity-40 cursor-not-allowed' : 'hover:bg-gray-50 cursor-pointer'}`}
                style={{ color: item.danger ? 'var(--ogq-color-semantic-error-default,#e21235)' : 'var(--ogq-color-mono-800,#425052)' }}>
                {item.icon && <span className="flex-shrink-0 opacity-60">{item.icon}</span>}
                {item.label}
              </button>
            )
          })}
        </div>
      )}
    </div>
  )
}
