<template>
  <nav class="app-nav">
    <router-link to="/">首页</router-link>
    <template v-if="user">
      <template v-if="user.role === 'ADMIN'">
        <router-link to="/chat" class="nav-chat" @click="clearUnread">聊天<span v-if="unreadCount > 0" class="nav-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span></router-link>
        <router-link to="/admin/users">用户管理</router-link>
        <router-link to="/admin/categories">分类管理</router-link>
        <router-link to="/admin/disputes">纠纷处理</router-link>
        <router-link to="/admin/reports">举报处理</router-link>
      </template>
      <template v-else>
        <router-link to="/publish">发布商品</router-link>
        <router-link to="/my-products">我的商品</router-link>
        <router-link to="/my-orders">我的订单</router-link>
        <router-link to="/chat" class="nav-chat" @click="clearUnread">聊天<span v-if="unreadCount > 0" class="nav-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span></router-link>
        <router-link :to="`/user/${user.id}`">个人主页</router-link>
        <router-link to="/my-reports">我的举报</router-link>
      </template>
    </template>
  </nav>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useUserStore } from '../../store/user.js'
import { chatAPI } from '../../api/modules.js'

const store = useUserStore()
const user = computed(() => store.state.user)
const unreadCount = ref(0)
let unreadTimer = null

const fetchUnread = async () => {
  if (!user.value) return
  try { const r = await chatAPI.getUnreadCount(); unreadCount.value = Number(r.data || 0) } catch { unreadCount.value = 0 }
}
const clearUnread = () => { unreadCount.value = 0; fetchUnread() }

const handleUnreadDelta = (e) => {
  unreadCount.value = Math.max(0, unreadCount.value + (e.detail || 0))
}

onMounted(() => {
  fetchUnread()
  unreadTimer = setInterval(fetchUnread, 30000)
  window.addEventListener('chat-unread-refresh', fetchUnread)
  window.addEventListener('chat-unread-delta', handleUnreadDelta)
})
onBeforeUnmount(() => {
  if (unreadTimer) clearInterval(unreadTimer)
  window.removeEventListener('chat-unread-refresh', fetchUnread)
})
</script>

<style scoped>
.app-nav {
  display: flex; flex-wrap: wrap; gap: 0; padding: 0 20px; min-height: 44px;
  background: var(--card-bg); border-bottom: 1px solid var(--border); align-items: center; overflow-x: auto;
}
.app-nav a {
  padding: 0 14px; height: 40px; line-height: 40px; color: var(--text);
  text-decoration: none; font-size: 13px; border-bottom: 2px solid transparent;
  transition: all .2s; white-space: nowrap; flex-shrink: 0;
}
.nav-chat { position: relative; }
.nav-badge {
  position: absolute; top: 6px; right: 2px; min-width: 16px; height: 16px;
  padding: 0 4px; border-radius: 8px; background: #ff4d4f; color: #fff;
  font-size: 10px; line-height: 16px; text-align: center;
}
.app-nav a:hover, .app-nav a.router-link-active { color: var(--primary); border-bottom-color: var(--primary); }
@media (max-width: 600px) { .app-nav { padding: 0 8px; gap: 0; } .app-nav a { padding: 0 10px; font-size: 12px; } }
</style>
