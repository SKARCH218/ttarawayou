'use client'

import { useState, type ReactNode } from 'react'

export interface OGQTooltipProps {
  content: ReactNode
  children: ReactNode
  position?: 'top' | 'bottom' | 'left' | 'right'
  className?: string
}

const posMap = {
  top: 'bottom-full left-1/2 -translate-x-1/2 mb-2',
  bottom: 'top-full left-1/2 -translate-x-1/2 mt-2',
  left: 'right-full top-1/2 -translate-y-1/2 mr-2',
  right: 'left-full top-1/2 -translate-y-1/2 ml-2',
}

export function OGQTooltip({ content, children, position = 'top', className = '' }: OGQTooltipProps) {
  const [show, setShow] = useState(false)

  return (
    <span
      className={`relative inline-block ${className}`}
      onMouseEnter={() => setShow(true)}
      onMouseLeave={() => setShow(false)}
      onFocus={() => setShow(true)}
      onBlur={() => setShow(false)}
    >
      {children}
      {show && (
        <span
          className={`absolute ${posMap[position]} px-2.5 py-1.5 text-[12px] text-white rounded-[var(--ogq-radius-md,8px)] whitespace-nowrap pointer-events-none`}
          style={{
            backgroundColor: 'var(--ogq-color-mono-900,#262d2e)',
            zIndex: 'var(--ogq-z-tooltip,1600)',
          }}
          role="tooltip"
        >
          {content}
        </span>
      )}
    </span>
  )
}
