'use client'

import { ChevronLeft, ChevronRight } from 'lucide-react'

export interface OGQPaginationProps {
  current: number
  total: number
  onChange: (page: number) => void
  siblingCount?: number
  className?: string
}

function range(start: number, end: number) {
  return Array.from({ length: end - start + 1 }, (_, i) => start + i)
}

export function OGQPagination({ current, total, onChange, siblingCount = 1, className = '' }: OGQPaginationProps) {
  const pages = (() => {
    const totalNumbers = siblingCount * 2 + 3
    if (total <= totalNumbers + 2) return range(1, total)
    const leftSibling = Math.max(current - siblingCount, 1)
    const rightSibling = Math.min(current + siblingCount, total)
    const showLeftDots = leftSibling > 2
    const showRightDots = rightSibling < total - 1
    if (!showLeftDots && showRightDots) return [...range(1, 3 + 2 * siblingCount), -1, total]
    if (showLeftDots && !showRightDots) return [1, -1, ...range(total - (2 + 2 * siblingCount), total)]
    return [1, -1, ...range(leftSibling, rightSibling), -2, total]
  })()

  const btn = 'w-8 h-8 flex items-center justify-center rounded-[var(--ogq-radius-md,8px)] text-[13px] transition-colors'

  return (
    <nav className={`flex items-center gap-1 ${className}`} aria-label="Pagination">
      <button onClick={() => current > 1 && onChange(current - 1)} disabled={current === 1}
        className={`${btn} ${current === 1 ? 'opacity-30 cursor-not-allowed' : 'hover:bg-gray-100 cursor-pointer'}`}
        style={{ color: 'var(--ogq-color-mono-500,#76888f)' }}>
        <ChevronLeft size={16} strokeWidth={1.5} />
      </button>
      {pages.map((p, i) =>
        p < 0 ? (
          <span key={`dot-${i}`} className="w-8 h-8 flex items-center justify-center text-[13px] text-gray-400">…</span>
        ) : (
          <button key={p} onClick={() => onChange(p)}
            className={`${btn} font-medium ${p === current ? 'text-white' : 'hover:bg-gray-100'}`}
            style={{
              backgroundColor: p === current ? 'var(--ogq-color-primary-600,#00c389)' : 'transparent',
              color: p === current ? '#fff' : 'var(--ogq-color-mono-700,#57676b)',
              cursor: 'pointer',
            }}>
            {p}
          </button>
        )
      )}
      <button onClick={() => current < total && onChange(current + 1)} disabled={current === total}
        className={`${btn} ${current === total ? 'opacity-30 cursor-not-allowed' : 'hover:bg-gray-100 cursor-pointer'}`}
        style={{ color: 'var(--ogq-color-mono-500,#76888f)' }}>
        <ChevronRight size={16} strokeWidth={1.5} />
      </button>
    </nav>
  )
}
