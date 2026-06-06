import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

import App from './App.vue'
import router from './router'
import './styles/global.css'
import { initAppFontSize } from './composables/useFontSize'

const app = createApp(App)

function initAppTheme() {
  const savedSettings = localStorage.getItem('app-settings')
  let darkMode: boolean | null = null

  if (savedSettings) {
    try {
      const parsed = JSON.parse(savedSettings)
      if (typeof parsed.darkMode === 'boolean') {
        darkMode = parsed.darkMode
      }
    } catch {}
  }

  if (darkMode === null) {
    const savedTheme = localStorage.getItem('theme')
    if (savedTheme === 'dark') darkMode = true
    if (savedTheme === 'light') darkMode = false
  }

  if (darkMode === null) {
    darkMode = window.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false
  }

  document.documentElement.setAttribute('data-theme', darkMode ? 'dark' : 'light')
  localStorage.setItem('theme', darkMode ? 'dark' : 'light')
}

initAppTheme()
initAppFontSize()

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

app.mount('#app')
