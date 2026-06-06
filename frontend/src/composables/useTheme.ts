import { ref, onMounted } from 'vue'

const isDark = ref(false)

export function useTheme() {
  // 应用主题
  const applyTheme = (dark: boolean) => {
    const html = document.documentElement
    if (dark) {
      html.setAttribute('data-theme', 'dark')
    } else {
      html.setAttribute('data-theme', 'light')
    }
    isDark.value = dark
    localStorage.setItem('theme', dark ? 'dark' : 'light')
    const rawSettings = localStorage.getItem('app-settings')
    let settings = {}
    if (rawSettings) {
      try {
        settings = JSON.parse(rawSettings)
      } catch {}
    }
    localStorage.setItem('app-settings', JSON.stringify({ ...settings, darkMode: dark }))
  }

  // 切换主题
  const toggleTheme = () => {
    applyTheme(!isDark.value)
  }

  // 设置主题
  const setTheme = (dark: boolean) => {
    applyTheme(dark)
  }

  // 获取当前主题状态
  const getIsDark = () => isDark.value

  // 初始化主题
  const initTheme = () => {
    const savedTheme = localStorage.getItem('theme')
    if (savedTheme === 'dark') {
      applyTheme(true)
    } else if (savedTheme === 'light') {
      applyTheme(false)
    } else {
      // 跟随系统
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
      applyTheme(prefersDark)
    }
  }

  onMounted(() => {
    initTheme()
  })

  return {
    isDark,
    toggleTheme,
    setTheme,
    getIsDark,
    initTheme
  }
}
