<template>
  <div id="app" class="app-container">
    <div v-if="isAuthPage" class="auth-app">
      <router-view />
    </div>
    <div v-else class="main-app">
      <AppHeader />
      <div class="main-layout">
        <AppSidebar />
        <main class="main-content">
          <router-view />
        </main>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from '@/layout/AppHeader.vue'
import AppSidebar from '@/layout/AppSidebar.vue'

const route = useRoute()
const isAuthPage = computed(() => route.path === '/login' || route.path === '/register')
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: var(--font-sans);
  background: var(--bg-primary);
  overflow-x: hidden;
}

#app {
  min-height: 100vh;
}

.main-app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.main-layout {
  display: flex;
  flex: 1;
  align-items: stretch;
  min-width: 0;
}

.main-content {
  flex: 1;
  min-width: 0;
  padding: 1.5rem;
  overflow-y: auto;
  min-height: calc(100vh - var(--app-header-height, 4rem));
  background: var(--bg-primary);
}

@media (max-width: 48rem) {
  .main-layout {
    flex-direction: column;
  }

  .main-content {
    padding: 1rem;
    min-height: auto;
  }
}
</style>
