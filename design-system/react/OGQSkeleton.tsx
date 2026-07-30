'use client'

export interface OGQSkeletonProps {
  variant?: 'text' | 'circular' | 'rectangular' | 'rounded'
  width?: number | string
  height?: number | string
  className?: string
}

export function OGQSkeleton({ variant = 'text', width, height, className = '' }: OGQSkeletonProps) {
  const base = 'animate-pulse'
  const bg = 'var(--ogq-color-mono-080,#eef1f1)'
  const radiusMap = { text: '4px', circular: '50%', rectangular: '0', rounded: 'var(--ogq-radius-lg,12px)' }
  const defaultH = variant === 'text' ? 16 : variant === 'circular' ? 40 : 100

  return (
    <div
      className={`${base} ${className}`}
      style={{
        width: width || (variant === 'circular' ? 40 : '100%'),
        height: height || defaultH,
        borderRadius: radiusMap[variant],
        backgroundColor: bg,
      }}
    />
  )
}

export function OGQSkeletonCard({ className = '' }: { className?: string }) {
  return (
    <div className={`p-4 rounded-[var(--ogq-radius-xl,16px)] border border-gray-100 ${className}`}>
      <OGQSkeleton variant="rounded" height={160} className="mb-4" />
      <OGQSkeleton variant="text" width="60%" height={20} className="mb-2" />
      <OGQSkeleton variant="text" width="90%" height={14} className="mb-1" />
      <OGQSkeleton variant="text" width="75%" height={14} />
    </div>
  )
}
