'use client'

import { useState, useMemo } from 'react'
import { ChevronLeft, ChevronRight, Calendar } from 'lucide-react'

export interface OGQDatePickerProps {
  value?: string // YYYY-MM-DD
  onChange?: (date: string) => void
  label?: string
  placeholder?: string
  className?: string
}

const DAYS = ['일', '월', '화', '수', '목', '금', '토']

function getDaysInMonth(year: number, month: number) {
  return new Date(year, month + 1, 0).getDate()
}

function getFirstDayOfMonth(year: number, month: number) {
  return new Date(year, month, 1).getDay()
}

export function OGQDatePicker({ value, onChange, label, placeholder = 'YYYY-MM-DD', className = '' }: OGQDatePickerProps) {
  const [open, setOpen] = useState(false)
  const today = new Date()
  const selected = value ? new Date(value) : null
  const [viewYear, setViewYear] = useState(selected?.getFullYear() || today.getFullYear())
  const [viewMonth, setViewMonth] = useState(selected?.getMonth() ?? today.getMonth())

  const days = useMemo(() => {
    const daysInMonth = getDaysInMonth(viewYear, viewMonth)
    const firstDay = getFirstDayOfMonth(viewYear, viewMonth)
    const cells: Array<{ day: number; current: boolean }> = []
    const prevDays = getDaysInMonth(viewYear, viewMonth - 1)
    for (let i = firstDay - 1; i >= 0; i--) cells.push({ day: prevDays - i, current: false })
    for (let i = 1; i <= daysInMonth; i++) cells.push({ day: i, current: true })
    const remaining = 42 - cells.length
    for (let i = 1; i <= remaining; i++) cells.push({ day: i, current: false })
    return cells
  }, [viewYear, viewMonth])

  const prevMonth = () => { if (viewMonth === 0) { setViewMonth(11); setViewYear(viewYear - 1) } else setViewMonth(viewMonth - 1) }
  const nextMonth = () => { if (viewMonth === 11) { setViewMonth(0); setViewYear(viewYear + 1) } else setViewMonth(viewMonth + 1) }

  const selectDate = (day: number) => {
    const m = String(viewMonth + 1).padStart(2, '0')
    const d = String(day).padStart(2, '0')
    onChange?.(`${viewYear}-${m}-${d}`)
    setOpen(false)
  }

  const isToday = (day: number, current: boolean) => current && day === today.getDate() && viewMonth === today.getMonth() && viewYear === today.getFullYear()
  const isSelected = (day: number, current: boolean) => current && selected && day === selected.getDate() && viewMonth === selected.getMonth() && viewYear === selected.getFullYear()

  return (
    <div className={`relative inline-block ${className}`}>
      {label && <label className="block text-[12px] font-medium mb-1.5" style={{ color: 'var(--ogq-color-mono-700,#57676b)' }}>{label}</label>}
      <button onClick={() => setOpen(!open)}
        className="flex items-center gap-2 px-3.5 py-2 border rounded-[var(--ogq-radius-md,8px)] text-[14px] min-w-[180px]"
        style={{ borderColor: 'var(--ogq-color-mono-100,#d8dfdf)', color: value ? 'var(--ogq-color-mono-900,#262d2e)' : 'var(--ogq-color-mono-400,#7d8d92)' }}>
        <Calendar size={16} strokeWidth={1.5} className="text-gray-400" />
        {value || placeholder}
      </button>
      {open && (
        <div className="absolute top-full mt-1 left-0 bg-white rounded-[var(--ogq-radius-xl,16px)] border p-3 w-[280px]"
          style={{ borderColor: 'var(--ogq-color-mono-080,#eef1f1)', boxShadow: 'var(--ogq-shadow-lg)', zIndex: 'var(--ogq-z-dropdown,1000)' }}>
          <div className="flex items-center justify-between mb-3">
            <button onClick={prevMonth} className="w-7 h-7 flex items-center justify-center rounded-md hover:bg-gray-100">
              <ChevronLeft size={16} strokeWidth={1.5} />
            </button>
            <span className="text-[14px] font-semibold" style={{ color: 'var(--ogq-color-mono-900,#262d2e)' }}>
              {viewYear}년 {viewMonth + 1}월
            </span>
            <button onClick={nextMonth} className="w-7 h-7 flex items-center justify-center rounded-md hover:bg-gray-100">
              <ChevronRight size={16} strokeWidth={1.5} />
            </button>
          </div>
          <div className="grid grid-cols-7 gap-0 text-center">
            {DAYS.map((d) => <div key={d} className="text-[10px] font-medium py-1" style={{ color: 'var(--ogq-color-mono-400,#7d8d92)' }}>{d}</div>)}
            {days.map((cell, i) => (
              <button key={i} disabled={!cell.current}
                onClick={() => cell.current && selectDate(cell.day)}
                className={`w-8 h-8 text-[12px] rounded-full transition-colors ${cell.current ? 'cursor-pointer hover:bg-gray-100' : 'opacity-30'}`}
                style={{
                  backgroundColor: isSelected(cell.day, cell.current) ? 'var(--ogq-color-primary-600,#00c389)' : 'transparent',
                  color: isSelected(cell.day, cell.current) ? '#fff' : isToday(cell.day, cell.current) ? 'var(--ogq-color-primary-600,#00c389)' : 'var(--ogq-color-mono-800,#425052)',
                  fontWeight: isToday(cell.day, cell.current) || isSelected(cell.day, cell.current) ? 600 : 400,
                }}>
                {cell.day}
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
