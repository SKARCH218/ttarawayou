'use client'
import { type InputHTMLAttributes } from 'react'

export interface OGQSliderProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'type' | 'size'> {
  label?: string
  showValue?: boolean
  size?: 'sm' | 'md'
}

export function OGQSlider({ label, showValue, size = 'md', className = '', ...props }: OGQSliderProps) {
  const h = size === 'sm' ? 4 : 6
  return (
    <div className={className}>
      {(label || showValue) && (
        <div className="flex justify-between mb-2">
          {label && <span className="text-[13px] font-medium" style={{ color: 'var(--ogq-color-mono-700,#57676b)' }}>{label}</span>}
          {showValue && <span className="text-[13px] font-mono" style={{ color: 'var(--ogq-color-mono-500,#76888f)' }}>{props.value}</span>}
        </div>
      )}
      <input
        type="range"
        className="w-full cursor-pointer appearance-none bg-transparent
          [&::-webkit-slider-runnable-track]:rounded-full [&::-webkit-slider-runnable-track]:bg-[var(--ogq-color-mono-080,#eef1f1)]
          [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:bg-[var(--ogq-color-primary-600,#00c389)] [&::-webkit-slider-thumb]:border-2 [&::-webkit-slider-thumb]:border-white [&::-webkit-slider-thumb]:shadow-md"
        style={{
          ['--track-h' as string]: `${h}px`,
          ['--thumb-size' as string]: `${h * 3}px`,
        }}
        {...props}
      />
    </div>
  )
}
