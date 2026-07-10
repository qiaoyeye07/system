<template>
  <nav class="app-nav">
    <router-link to="/">首页</router-link>
    <template v-if="user">
      <template v-if="user.role === 'ADMIN'">
        <button class="nav-link nav-chat" type="button" @click="goChat">
          聊天
          <span v-if="unreadCount > 0" class="nav-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
        </button>
        <router-link to="/admin/users">用户管理</router-link>
        <router-link to="/admin/categories">分类管理</router-link>
        <router-link to="/admin/disputes">纠纷处理</router-link>
        <router-link to="/admin/reports">举报处理</router-link>
      </template>
      <template v-else>
        <router-link to="/publish">发布商品</router-link>
        <router-link to="/my-products">我的商品</router-link>
        <router-link to="/my-orders">我的订单</router-link>
        <button class="nav-link nav-chat" type="button" @click="goChat">
          聊天
          <span v-if="unreadCount > 0" class="nav-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
        </button>
        <router-link :to="`/user/${user.id}`">个人主页</router-link>
        <router-link to="/my-reports">我的举报</router-link>
      </template>
    </template>
  </nav>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { chatAPI } from '../../api/modules.js'
import { useUserStore } from '../../store/user.js'

const store = useUserStore()
const router = useRouter()
const user = computed(() => store.state.user)
const unreadCount = ref(0)
let unreadTimer = null

const goChat = () => {
  router.push({ path: '/chat', query: { t: Date.now() } })
}

const refreshUnreadCount = async () => {
  if (!user.value) {
    unreadCount.value = 0
    return
  }

  try {
    const res = await chatAPI.getUnreadCount()
    unreadCount.value = Number(res.data || 0)
  } catch {
    unreadCount.value = 0
  }
}

const handleRealtimeEvent = (event) => {
  const detail = event.detail || {}
  if (detail.type === 'CHAT_UNREAD_CHANGED') {
    unreadCount.value = Number(detail.unreadCount || 0)
    return
  }
  if (detail.type === 'MESSAGE_NEW') {
    unreadCount.value += 1
    return
  }
  if (['CHAT_SOCKET_CONNECTED'].includes(detail.type)) {
    refreshUnreadCount()
  }
}

const startUnreadSync = () => {
  if (unreadTimer) return
  unreadTimer = setInterval(() => {
    if (!document.hidden) refreshUnreadCount()
  }, 10000)
}

const stopUnreadSync = () => {
  if (!unreadTimer) return
  clearInterval(unreadTimer)
  unreadTimer = null
}

watch(() => user.value?.id, refreshUnreadCount, { immediate: true })

onMounted(() => {
  window.addEventListener('chat-realtime', handleRealtimeEvent)
  startUnreadSync()
})

onBeforeUnmount(() => {
  window.removeEventListener('chat-realtime', handleRealtimeEvent)
  stopUnreadSync()
})
</script>

<style scoped>
.app-nav {
  display: flex;
  gap: 0;
  padding: 0 24px;
  height: 44px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  align-items: center;
}
.app-nav a,
.nav-link {
  padding: 0 16px;
  height: 44px;
  line-height: 44px;
  color: #333;
  text-decoration: none;
  font-size: 14px;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
}
.nav-link {
  background: transparent;
  border-top: 0;
  border-right: 0;
  border-left: 0;
  cursor: pointer;
  font-family: inherit;
  position: relative;
}
.nav-chat { display: inline-flex; align-items: center; gap: 4px; }
.nav-badge {
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 8px;
  background: #ff4d4f;
  color: #fff;
  font-size: 10px;
  line-height: 16px;
  text-align: center;
}
.app-nav a:hover,
.app-nav a.router-link-active,
.nav-link:hover {
  color: #1890ff;
  border-bottom-color: #1890ff;
}
</style>
