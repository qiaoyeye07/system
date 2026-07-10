<template>
  <div id="app-root">
    <AppHeader />
    <AppNav />
    <main class="app-content">
      <router-view v-slot="{ Component }">
        <keep-alive include="ChatView">
          <component :is="Component" />
        </keep-alive>
      </router-view>
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import AppHeader from './components/layout/AppHeader.vue'
import AppNav from './components/layout/AppNav.vue'

const router = useRouter()
const isLoggedIn = computed(() => !!localStorage.getItem('token'))
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: -apple-system, BlinkMacSystemFont, 'Microsoft YaHei', sans-serif; color: #333; background: #f5f5f5; }
.app-content { max-width: 1200px; margin: 0 auto; padding: 20px; min-height: calc(100vh - 120px); }
a { color: #1890ff; text-decoration: none; }
a:hover { color: #40a9ff; }
button { cursor: pointer; }
</style>
