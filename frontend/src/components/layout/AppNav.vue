<template>
  <nav class="app-nav">
    <router-link to="/">首页</router-link>
    <template v-if="user">
      <template v-if="user.role === 'ADMIN'">
        <router-link to="/chat">聊天</router-link>
        <router-link to="/admin/users">用户管理</router-link>
        <router-link to="/admin/categories">分类管理</router-link>
        <router-link to="/admin/disputes">纠纷处理</router-link>
        <router-link to="/admin/reports">举报处理</router-link>
      </template>
      <template v-else>
        <router-link to="/publish">发布商品</router-link>
        <router-link to="/my-products">我的商品</router-link>
        <router-link to="/my-orders">我的订单</router-link>
        <router-link to="/chat">聊天</router-link>
        <router-link :to="`/user/${user.id}`">个人主页</router-link>
        <router-link to="/my-reports">我的举报</router-link>
      </template>
    </template>
  </nav>
</template>

<script setup>
import { computed } from 'vue'
import { useUserStore } from '../../store/user.js'

const store = useUserStore()
const user = computed(() => store.state.user)
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
.app-nav a:hover, .app-nav a.router-link-active { color: var(--primary); border-bottom-color: var(--primary); }
@media (max-width: 600px) { .app-nav { padding: 0 8px; gap: 0; } .app-nav a { padding: 0 10px; font-size: 12px; } }
</style>
