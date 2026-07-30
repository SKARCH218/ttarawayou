'use client'

export interface OGQRadioProps {
  name: string
  value: string
  checked?: boolean
  onChange?: (value: string) => void
  label?: string
  disabled?: boolean
  size?: 'sm' | 'md' | 'lg'
}

const sizeMap = { sm: 16, md: 20, lg: 24 }
const dotMap = { sm: 6, md: 8, lg: 10 }

export function OGQRadio({ name, value, checked, onChange, label, disabled, size = 'md' }: OGQRadioProps) {
  const s = sizeMap[size]
  const d = dotMap[size]
  return (
    <label className={`inline-flex items-center gap-2 cursor-pointer ${disabled ? 'opacity-50 cursor-not-allowed' : ''}`}>
      <span className="relative flex items-center justify-center" style={{ width: s, height: s }}>
        <input type="radio" name={name} value={value} checked={checked} onChange={() => onChange?.(value)} disabled={disabled} className="sr-only peer" />
        <span className="w-full h-full rounded-full border-2 transition-colors"
          style={{ borderColor: checked ? 'var(--ogq-color-primary-600,#00c389)' : 'var(--ogq-color-mono-200,#a7b6b9)' }} />
        {checked && <span className="absolute rounded-full" style={{ width: d, height: d, backgroundColor: 'var(--ogq-color-primary-600,#00c389)' }} />}
      </span>
      {label && <span className="text-[14px]" style={{ color: 'var(--ogq-color-mono-800,#425052)' }}>{label}</span>}
    </label>
  )
}

export interface OGQRadioGroupProps {
  name: string
  value?: string
  onChange?: (value: string) => void
  options: Array<{ value: string; label: string; disabled?: boolean }>
  direction?: 'horizontal' | 'vertical'
  size?: 'sm' | 'md' | 'lg'
}

export function OGQRadioGroup({ name, value, onChange, options, direction = 'vertical', size = 'md' }: OGQRadioGroupProps) {
  return (
    <div className={`flex ${direction === 'vertical' ? 'flex-col gap-3' : 'flex-row gap-5'}`}>
      {options.map((opt) => (
        <OGQRadio key={opt.value} name={name} value={opt.value} checked={value === opt.value} onChange={onChange} label={opt.label} disabled={opt.disabled} size={size} />
      ))}
    </div>
  )
}
