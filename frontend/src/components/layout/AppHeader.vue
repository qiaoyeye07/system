<template>
  <header class="app-header">
    <h1 class="logo">二手交易平台</h1>
    <div class="header-right">
      <template v-if="user">
        <span class="username">{{ user.username }}</span>
        <button class="btn-logout" @click="handleLogout">退出</button>
      </template>
      <template v-else>
        <router-link to="/login" class="btn-login">登录</router-link>
        <router-link to="/register" class="btn-register">注册</router-link>
      </template>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../store/user.js'

const router = useRouter()
const store = useUserStore()
const user = computed(() => store.state.user)

const handleLogout = () => {
  store.logout()
  router.push('/login')
}
</script>

<style scoped>
.app-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 0 28px; height: 56px;
  background: linear-gradient(135deg, #5a5753, #6b6560, #7a7268);
  color: #f5f0ea; box-shadow: 0 2px 8px rgba(0,0,0,.1);
  position: sticky; top: 0; z-index: 100;
}
.logo { font-size: 20px; font-weight: bold; }
.header-right { display: flex; align-items: center; gap: 16px; }
.username { color: rgba(255,255,255,.9); font-weight: 500; }
.btn-logout { background: transparent; border: 1px solid rgba(255,255,255,.4); color: rgba(255,255,255,.9); padding: 5px 18px; border-radius: 6px; font-size: 13px; }
.btn-logout:hover { border-color: #fff; background: rgba(255,255,255,.1); }
.btn-login { color: rgba(255,255,255,.9); padding: 5px 18px; text-decoration: none; font-size: 13px; font-weight: 500; }
.btn-login:hover { color: var(--primary-hover); }
.btn-register {
  background: var(--primary);
  color: #fff;
  padding: 4px 16px;
  border-radius: 4px;
  text-decoration: none;
}
.btn-register:hover { background: var(--primary-hover); color: #fff; }
</style>
