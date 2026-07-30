'use client'

export interface OGQProgressProps {
  value: number
  max?: number
  size?: 'sm' | 'md' | 'lg'
  color?: string
  showLabel?: boolean
  className?: string
}

const heightMap = { sm: 4, md: 8, lg: 12 }

export function OGQProgress({ value, max = 100, size = 'md', color = 'var(--ogq-color-primary-600,#00c389)', showLabel, className = '' }: OGQProgressProps) {
  const pct = Math.min(Math.max((value / max) * 100, 0), 100)
  return (
    <div className={`flex items-center gap-3 ${className}`}>
      <div className="flex-1 rounded-full overflow-hidden" style={{ height: heightMap[size], backgroundColor: 'var(--ogq-color-mono-080,#eef1f1)' }}>
        <div className="h-full rounded-full transition-all" style={{ width: `${pct}%`, backgroundColor: color }} />
      </div>
      {showLabel && <span className="text-[12px] font-mono flex-shrink-0" style={{ color: 'var(--ogq-color-mono-600,#6c7e84)' }}>{Math.round(pct)}%</span>}
    </div>
  )
}
