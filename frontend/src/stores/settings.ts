import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useSettingsStore = defineStore('settings', () => {
  const darkMode = ref(false)
  const notification = ref(true)
  const sound = ref(false)

  function load() {
    const raw = localStorage.getItem('app-settings')
    if (raw) {
      try {
        const data = JSON.parse(raw)
        darkMode.value = !!data.darkMode
        notification.value = data.notification !== false
        sound.value = !!data.sound
        return
      } catch {}
    }
    // default: respect prefers-color-scheme
    darkMode.value = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches
  }

  function save() {
    localStorage.setItem('app-settings', JSON.stringify({ darkMode: darkMode.value, notification: notification.value, sound: sound.value }))
  }

  // initialize
  load()

  return { darkMode, notification, sound, load, save }
})
