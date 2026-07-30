'use client'

import { useState, type ReactNode } from 'react'

export type TabVariant = 'default' | 'pills' | 'underline'

export interface TabItem {
  key: string
  label: ReactNode
  content: ReactNode
  disabled?: boolean
}

export interface OGQTabsProps {
  items: TabItem[]
  defaultKey?: string
  variant?: TabVariant
  onChange?: (key: string) => void
  className?: string
}

export function OGQTabs({ items, defaultKey, variant = 'default', onChange, className = '' }: OGQTabsProps) {
  const [activeKey, setActiveKey] = useState(defaultKey || items[0]?.key || '')

  const handleSelect = (key: string) => {
    setActiveKey(key)
    onChange?.(key)
  }

  const activeContent = items.find((i) => i.key === activeKey)?.content

  return (
    <div className={className}>
      <div
        className={`flex ${variant === 'underline' ? 'border-b' : 'gap-1'}`}
        style={{ borderColor: variant === 'underline' ? 'var(--ogq-color-mono-100,#d8dfdf)' : undefined }}
        role="tablist"
      >
        {items.map((item) => {
          const isActive = item.key === activeKey
          const baseStyle = 'px-4 py-2 text-[14px] font-medium transition-colors cursor-pointer'

          const variantClass = {
            default: isActive
              ? 'bg-[var(--ogq-color-primary-050,#dff4ea)] text-[var(--ogq-color-primary-700,#00b57f)] rounded-lg'
              : 'text-[var(--ogq-color-mono-500,#76888f)] hover:text-[var(--ogq-color-mono-700,#57676b)]',
            pills: isActive
              ? 'bg-[var(--ogq-color-mono-900,#262d2e)] text-white rounded-full'
              : 'text-[var(--ogq-color-mono-500,#76888f)] rounded-full hover:bg-[var(--ogq-color-mono-050,#f4f6f6)]',
            underline: isActive
              ? 'text-[var(--ogq-color-primary-600,#00c389)] border-b-2 border-[var(--ogq-color-primary-600,#00c389)] -mb-px'
              : 'text-[var(--ogq-color-mono-500,#76888f)] hover:text-[var(--ogq-color-mono-700,#57676b)]',
          }

          return (
            <button
              key={item.key}
              role="tab"
              aria-selected={isActive}
              disabled={item.disabled}
              onClick={() => handleSelect(item.key)}
              className={`${baseStyle} ${variantClass[variant]} ${item.disabled ? 'opacity-40 cursor-not-allowed' : ''}`}
            >
              {item.label}
            </button>
          )
        })}
      </div>
      <div className="mt-4" role="tabpanel">{activeContent}</div>
    </div>
  )
}
