<template>
  <div class="login-page">
    <div class="login-card">
      <h2>二手交易平台</h2>
      <p class="subtitle">登录</p>
      <div v-if="errorMsg" class="error-tip">{{ errorMsg }}</div>
      <form @submit.prevent="handleLogin">
        <div class="form-group">
          <label>用户名</label>
          <input v-model="form.username" type="text" placeholder="请输入用户名" required />
        </div>
        <div class="form-group">
          <label>密码</label>
          <input v-model="form.password" type="password" placeholder="请输入密码" required />
        </div>
        <button type="submit" class="btn-primary" :disabled="loading">登录</button>
      </form>
      <p class="link-text">还没有账号？<router-link to="/register">立即注册</router-link></p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { authAPI } from '../api/modules.js'
import { useUserStore } from '../store/user.js'

const router = useRouter()
const route = useRoute()
const store = useUserStore()
const loading = ref(false)
const errorMsg = ref('')
const form = reactive({ username: '', password: '' })

const handleLogin = async () => {
  errorMsg.value = ''
  loading.value = true
  try {
    const res = await authAPI.login({ username: form.username.trim(), password: form.password })
    store.login(res.data.token, {
      id: res.data.userId,
      username: res.data.username,
      role: res.data.role
    })
    const redirect = route.query.redirect || '/'
    router.push(redirect)
  } catch (e) {
    errorMsg.value = e?.message || '登录失败，请重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page { display: flex; align-items: center; justify-content: center; min-height: 100vh; background: #f0f2f5; }
.login-card { width: 400px; padding: 40px; background: #fff; border-radius: 8px; box-shadow: 0 2px 12px rgba(0,0,0,0.08); }
.login-card h2 { text-align: center; margin-bottom: 8px; font-size: 24px; }
.subtitle { text-align: center; color: #999; margin-bottom: 24px; }
.error-tip { background: #fff2f0; border: 1px solid #ffccc7; color: #ff4d4f; padding: 8px 12px; border-radius: 4px; margin-bottom: 16px; font-size: 14px; }
.form-group { margin-bottom: 16px; }
.form-group label { display: block; margin-bottom: 6px; font-size: 14px; color: #333; }
.form-group input { width: 100%; padding: 8px 12px; border: 1px solid #d9d9d9; border-radius: 4px; font-size: 14px; }
.form-group input:focus { border-color: #1890ff; outline: none; box-shadow: 0 0 0 2px rgba(24,144,255,0.2); }
.btn-primary { width: 100%; padding: 10px; background: #1890ff; color: #fff; border: none; border-radius: 4px; font-size: 16px; }
.btn-primary:hover { background: #40a9ff; }
.btn-primary:disabled { background: #91d5ff; cursor: not-allowed; }
.link-text { text-align: center; margin-top: 16px; font-size: 14px; color: #999; }
</style>
