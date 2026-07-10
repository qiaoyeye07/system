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
* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: -apple-system, BlinkMacSystemFont, 'Microsoft YaHei', sans-serif; color: #333; background: #f5f5f5; }
.app-content { max-width: 1200px; margin: 0 auto; padding: 20px; min-height: calc(100vh - 120px); }
a { color: #1890ff; text-decoration: none; }
a:hover { color: #40a9ff; }
button { cursor: pointer; }
</style>
