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
import { onBeforeUnmount, watch } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import AppHeader from './components/layout/AppHeader.vue'
import AppNav from './components/layout/AppNav.vue'
import { useUserStore } from './store/user.js'

const store = useUserStore()
let stompClient = null
let chatSubscription = null

const dispatchRealtimeEvent = (event) => {
  window.dispatchEvent(new CustomEvent('chat-realtime', { detail: event }))
}

const connectRealtime = () => {
  const userId = store.state.user?.id
  if (!userId || stompClient?.active) return

  stompClient = new Client({
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    webSocketFactory: () => new SockJS('/ws/chat'),
    onConnect: () => {
      chatSubscription?.unsubscribe()
      chatSubscription = stompClient.subscribe(`/queue/chat/${userId}`, frame => {
        try {
          dispatchRealtimeEvent(JSON.parse(frame.body))
        } catch {
          // Ignore malformed realtime payloads and keep the connection alive.
        }
      })
      dispatchRealtimeEvent({ type: 'CHAT_SOCKET_CONNECTED' })
    },
    onStompError: () => {
      dispatchRealtimeEvent({ type: 'CHAT_SOCKET_ERROR' })
    },
    onWebSocketClose: () => {
      dispatchRealtimeEvent({ type: 'CHAT_SOCKET_CLOSED' })
    }
  })

  stompClient.activate()
}

const disconnectRealtime = () => {
  chatSubscription?.unsubscribe()
  chatSubscription = null
  if (stompClient) {
    stompClient.deactivate()
    stompClient = null
  }
}

watch(
  () => store.state.user?.id,
  (userId, oldUserId) => {
    if (oldUserId && oldUserId !== userId) disconnectRealtime()
    if (userId) connectRealtime()
    else disconnectRealtime()
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  disconnectRealtime()
})
</script>

<style>
:root {
  --primary: #6b8f71; --primary-hover: #7da383; --primary-active: #567358;
  --accent: #a0715c; --accent-hover: #b8846e;
  --success: #5c8a5f; --warning: #b8953c; --danger: #c0392b;
  --bg: #ede8e1; --card-bg: #fdfcf9; --border: #d5cfc7;
  --text: #2c2824; --text-secondary: #5c5854; --text-muted: #8a8580;
  --radius: 8px; --radius-sm: 4px; --shadow: 0 2px 8px rgba(0,0,0,.06);
  --shadow-hover: 0 4px 16px rgba(0,0,0,.12);
  --transition: all .2s ease;
}
* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif; font-size: 14px; color: var(--text); background: var(--bg); line-height: 1.6; -webkit-font-smoothing: antialiased; }
.app-content { width: 100%; max-width: 1400px; margin: 0 auto; padding: 20px; min-height: calc(100vh - 120px); }
@media (max-width: 768px) { .app-content { padding: 12px; } }
a { color: var(--primary); text-decoration: none; transition: var(--transition); font-weight: 500; }
a:hover { color: var(--primary-active); }
button { cursor: pointer; border: none; outline: none; font-family: inherit; font-size: 14px; transition: var(--transition); }
.btn-primary { padding: 8px 20px; background: var(--primary); color: #fff; border-radius: var(--radius-sm); font-weight: 600; }
.btn-primary:hover { background: var(--primary-hover); }
.btn-primary:disabled { background: #bcc9be; color: var(--border); cursor: not-allowed; }
.btn-danger { padding: 8px 20px; color: var(--danger); border: 2px solid var(--danger); background: var(--card-bg); border-radius: var(--radius-sm); font-weight: 600; }
.btn-danger:hover { color: #fff; background: var(--danger); }
.card { background: var(--card-bg); border-radius: var(--radius); box-shadow: var(--shadow); padding: 20px; margin-bottom: 16px; transition: var(--transition); }
.card:hover { box-shadow: var(--shadow-hover); }
input, select, textarea { font-family: inherit; font-size: 14px; padding: 8px 12px; border: 2px solid var(--border); border-radius: var(--radius-sm); transition: var(--transition); width: 100%; background: var(--card-bg); color: var(--text); }
input:focus, select:focus, textarea:focus { border-color: var(--primary); outline: none; box-shadow: 0 0 0 3px rgba(107,143,113,.15); }
</style>
