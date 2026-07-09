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
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  height: 56px;
  background: #001529;
  color: #fff;
}
.logo { font-size: 20px; font-weight: bold; }
.header-right { display: flex; align-items: center; gap: 16px; }
.username { color: #ffffffa6; }
.btn-logout {
  background: transparent;
  border: 1px solid #ffffff4d;
  color: #fff;
  padding: 4px 16px;
  border-radius: 4px;
}
.btn-logout:hover { border-color: #fff; }
.btn-login {
  color: #fff;
  padding: 4px 16px;
  text-decoration: none;
}
.btn-login:hover { color: #40a9ff; }
.btn-register {
  background: #1890ff;
  color: #fff;
  padding: 4px 16px;
  border-radius: 4px;
  text-decoration: none;
}
.btn-register:hover { background: #40a9ff; color: #fff; }
</style>
