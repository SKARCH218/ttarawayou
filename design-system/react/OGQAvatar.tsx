'use client'

export type AvatarSize = 'xs' | 'sm' | 'md' | 'lg' | 'xl'

export interface OGQAvatarProps {
  src?: string
  alt?: string
  name?: string
  size?: AvatarSize
  className?: string
}

const sizeMap: Record<AvatarSize, number> = { xs: 24, sm: 32, md: 40, lg: 56, xl: 80 }
const fontMap: Record<AvatarSize, number> = { xs: 10, sm: 12, md: 14, lg: 20, xl: 28 }

function getInitials(name: string): string {
  return name.split(' ').map(w => w[0]).join('').toUpperCase().slice(0, 2)
}

export function OGQAvatar({ src, alt, name, size = 'md', className = '' }: OGQAvatarProps) {
  const s = sizeMap[size]
  const f = fontMap[size]
  if (src) {
    return <img src={src} alt={alt || name || ''} className={`rounded-full object-cover ${className}`} style={{ width: s, height: s }} />
  }
  return (
    <div className={`rounded-full flex items-center justify-center font-medium ${className}`}
      style={{ width: s, height: s, fontSize: f, backgroundColor: 'var(--ogq-color-primary-100,#b5e5d7)', color: 'var(--ogq-color-primary-800,#00996e)' }}>
      {name ? getInitials(name) : '?'}
    </div>
  )
}
