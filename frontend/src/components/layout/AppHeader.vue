<template>
  <header class="app-header">
    <h1 class="logo">二手交易平台</h1>
    <div class="header-right">
      <span class="username">{{ user?.username }}</span>
      <button class="btn-logout" @click="handleLogout">退出</button>
    </div>
  </header>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
const userStr = localStorage.getItem('user')
const user = userStr ? JSON.parse(userStr) : null

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
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
</style>
