// OGQ Design System — React Native Tokens
// Auto-generated from OGQ Design System v1.0.0
// Compatible with NativeWind and StyleSheet.create

// ── Colors ──

export const colors = {
  mono: {
    '000': '#ffffff', '010': '#fcfdfd', '030': '#f9fbfb',
    '050': '#f4f6f6', '080': '#eef1f1', '100': '#d8dfdf',
    '200': '#a7b6b9', '300': '#89989c', '400': '#7d8d92',
    '500': '#76888f', '600': '#6c7e84', '700': '#57676b',
    '800': '#425052', '900': '#262d2e', '990': '#262d2e',
  },
  primary: {
    '010': '#fbfdfc', '030': '#eef7f4', '050': '#dff4ea',
    '080': '#cbecdf', '100': '#b5e5d7', '200': '#9bdfcc',
    '300': '#7fdec1', '400': '#58dab1', '500': '#32d29d',
    '600': '#00c389', '700': '#00b57f', '800': '#00996e',
    '900': '#007a58', '990': '#006147',
  },
  secondary: {
    '010': '#fbfbfe', '030': '#f8f6fe', '050': '#ebe8fc',
    '080': '#e0dafb', '100': '#d2c8f9', '200': '#bfb0f2',
    '300': '#a793ec', '400': '#947be5', '500': '#7e5ae2',
    '600': '#703fe4', '700': '#5b1dcd', '800': '#4917a6',
    '900': '#37117e', '990': '#290d5e',
  },
  semantic: {
    success: { light: '#dff4ea', default: '#00c389', dark: '#007a58' },
    warning: { light: '#fff3e0', default: '#ff9800', dark: '#e65100' },
    error: { light: '#ffe2e4', default: '#e21235', dark: '#c90e2e' },
    info: { light: '#e6f4ff', default: '#1384d7', dark: '#0d5fa3' },
  },
  accent: { green: '#00ff85', orange: '#ff550d', purple: '#7f00ff' },
  social: {
    naver: '#03c75a', facebook: '#3d5a98', afreeca: '#0545b1',
    google: '#4285f4', apple: '#000000', kakao: '#fee500',
  },
} as const

// ── Typography ──

export const typography = {
  fontFamily: {
    sans: 'Pretendard Variable',
    mono: 'SF Mono',
  },
  scale: {
    displayXL: { fontSize: 56, lineHeight: 61.6, letterSpacing: -1.4, fontWeight: '700' as const },
    displayLG: { fontSize: 48, lineHeight: 55.2, letterSpacing: -0.96, fontWeight: '700' as const },
    displayMD: { fontSize: 36, lineHeight: 43.2, letterSpacing: -0.54, fontWeight: '600' as const },
    headingXL: { fontSize: 30, lineHeight: 37.5, letterSpacing: -0.3, fontWeight: '600' as const },
    headingLG: { fontSize: 24, lineHeight: 31.2, letterSpacing: -0.12, fontWeight: '600' as const },
    headingMD: { fontSize: 20, lineHeight: 28, letterSpacing: 0, fontWeight: '600' as const },
    headingSM: { fontSize: 18, lineHeight: 25.2, letterSpacing: 0, fontWeight: '500' as const },
    bodyLG: { fontSize: 18, lineHeight: 28.8, letterSpacing: 0, fontWeight: '400' as const },
    bodyMD: { fontSize: 16, lineHeight: 25.6, letterSpacing: 0, fontWeight: '400' as const },
    bodySM: { fontSize: 14, lineHeight: 21, letterSpacing: 0.07, fontWeight: '400' as const },
    caption: { fontSize: 12, lineHeight: 18, letterSpacing: 0.12, fontWeight: '400' as const },
    overline: { fontSize: 11, lineHeight: 16.5, letterSpacing: 0.88, fontWeight: '600' as const },
    // OGQ Original
    h1: { fontSize: 56, lineHeight: 56, fontWeight: '700' as const },
    h2: { fontSize: 48, lineHeight: 48, fontWeight: '700' as const },
    h3: { fontSize: 44, lineHeight: 44, fontWeight: '700' as const },
    h4: { fontSize: 40, lineHeight: 40, fontWeight: '700' as const },
    h5: { fontSize: 36, lineHeight: 36, fontWeight: '700' as const },
    h6: { fontSize: 32, lineHeight: 32, fontWeight: '700' as const },
    h7: { fontSize: 28, lineHeight: 28, fontWeight: '700' as const },
    h8: { fontSize: 24, lineHeight: 24, fontWeight: '700' as const },
    b1: { fontSize: 20, lineHeight: 28, fontWeight: '400' as const },
    b2: { fontSize: 18, lineHeight: 26, fontWeight: '400' as const },
    b3: { fontSize: 16, lineHeight: 24, fontWeight: '400' as const },
    b4: { fontSize: 14, lineHeight: 20, fontWeight: '400' as const },
    b5: { fontSize: 12, lineHeight: 18, fontWeight: '400' as const },
    c1: { fontSize: 11, lineHeight: 16, fontWeight: '400' as const },
    c2: { fontSize: 10, lineHeight: 16, fontWeight: '400' as const },
  },
} as const

// ── Spacing ──

export const spacing = {
  0: 0, 2: 2, 4: 4, 6: 6, 8: 8, 10: 10, 12: 12,
  16: 16, 20: 20, 24: 24, 28: 28, 32: 32,
  40: 40, 48: 48, 64: 64, 80: 80, 120: 120,
} as const

// ── Border Radius ──

export const radius = {
  none: 0, sm: 4, r6: 6, md: 8, lg: 12, xl: 16, xxl: 20, full: 1000,
} as const

// ── Shadows ──

export const shadows = {
  xs: { shadowColor: '#000', shadowOffset: { width: 0, height: 1 }, shadowOpacity: 0.05, shadowRadius: 1, elevation: 1 },
  sm: { shadowColor: '#000', shadowOffset: { width: 0, height: 1 }, shadowOpacity: 0.1, shadowRadius: 2, elevation: 2 },
  md: { shadowColor: '#000', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.1, shadowRadius: 4, elevation: 4 },
  lg: { shadowColor: '#000', shadowOffset: { width: 0, height: 10 }, shadowOpacity: 0.1, shadowRadius: 8, elevation: 8 },
  xl: { shadowColor: '#000', shadowOffset: { width: 0, height: 20 }, shadowOpacity: 0.1, shadowRadius: 12, elevation: 12 },
} as const

// ── Motion ──

export const motion = {
  duration: { instant: 50, fast: 100, normal: 200, moderate: 300, slow: 400, slower: 500, slowest: 700 },
  easing: {
    default: [0.2, 0, 0, 1] as const,
    decelerate: [0, 0, 0, 1] as const,
    accelerate: [0.3, 0, 1, 1] as const,
  },
} as const

// ── Z-Index ──

export const zIndex = {
  base: 0, raised: 1, dropdown: 1000, sticky: 1100,
  overlay: 1200, modal: 1300, popover: 1400, toast: 1500, tooltip: 1600,
} as const
