import { ref } from 'vue'

export const DEFAULT_FONT_SIZE = 16
export const MIN_FONT_SIZE = 14
export const MAX_FONT_SIZE = 28

export const currentAppFontSize = ref(DEFAULT_FONT_SIZE)

export function normalizeFontSize(value: number) {
  if (Number.isNaN(value)) return DEFAULT_FONT_SIZE
  return Math.min(MAX_FONT_SIZE, Math.max(MIN_FONT_SIZE, Math.round(value)))
}

export function applyAppFontSize(value: number) {
  const size = normalizeFontSize(value)
  const root = document.documentElement

  currentAppFontSize.value = size
  root.style.setProperty('--base-font-size', `${size}px`)
  root.style.fontSize = 'var(--base-font-size)'
  root.dataset.fontScale = size >= 24 ? 'xlarge' : size >= 20 ? 'large' : 'normal'
  document.body?.classList.toggle('care-mode', size >= 20)
  localStorage.setItem('app-font-size', String(size))
  window.dispatchEvent(new CustomEvent('app-font-size-change', { detail: size }))

  return size
}

export function getSavedFontSize() {
  return normalizeFontSize(parseInt(localStorage.getItem('app-font-size') || String(DEFAULT_FONT_SIZE), 10))
}

export function initAppFontSize() {
  return applyAppFontSize(getSavedFontSize())
}
