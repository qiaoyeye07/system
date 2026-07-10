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
          <div class="password-wrap">
            <input v-model="form.password" :type="showPwd ? 'text' : 'password'" placeholder="请输入密码" required />
            <span class="pwd-toggle" @click="showPwd = !showPwd">{{ showPwd ? '隐藏' : '显示' }}</span>
          </div>
        </div>
        <button type="submit" class="btn-login" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>
      <p class="link-text">还没有账号？<router-link to="/register">立即注册</router-link></p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { authAPI } from '../api/modules.js'
import { useUserStore } from '../store/user.js'

const route = useRoute()
const router = useRouter()
const store = useUserStore()
const loading = ref(false)
const errorMsg = ref('')
const showPwd = ref(false)
const form = reactive({ username: '', password: '' })

watch(() => form.username, () => { errorMsg.value = '' })
watch(() => form.password, () => { errorMsg.value = '' })

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
    router.push(route.query.redirect || '/')
  } catch (e) {
    errorMsg.value = e?.message || '登录失败，请重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page { display: flex; align-items: center; justify-content: center; min-height: 100vh; background: linear-gradient(135deg, #c4b7a6 0%, #d4c5b9 30%, #b8a99a 70%, #c9b8a8 100%); }
.login-card { width: 400px; padding: 48px 40px; background: #faf8f5; border-radius: 16px; box-shadow: 0 20px 60px rgba(0,0,0,0.08); }
.login-card h2 { text-align: center; margin-bottom: 4px; font-size: 26px; color: var(--text); }
.subtitle { text-align: center; color: var(--text-muted); margin-bottom: 32px; font-size: 15px; }
.error-tip { background: rgba(194,120,120,0.1); border: 1px solid rgba(194,120,120,0.2); color: var(--danger); padding: 10px 14px; border-radius: 8px; margin-bottom: 20px; font-size: 14px; }
.form-group { margin-bottom: 20px; }
.form-group label { display: block; margin-bottom: 8px; font-size: 14px; color: var(--text-secondary); font-weight: 500; }
.form-group input { width: 100%; padding: 10px 14px; border: 2px solid #eee; border-radius: 10px; font-size: 15px; transition: all .2s; }
.password-wrap { position: relative; }
.password-wrap input { padding-right: 52px; }
.pwd-toggle { position: absolute; right: 12px; top: 50%; transform: translateY(-50%); cursor: pointer; user-select: none; font-size: 13px; color: var(--text-secondary); background: none; border: none; }
.form-group input:focus { border-color: #b4846c; outline: none; box-shadow: 0 0 0 3px rgba(180,132,108,0.1); }
.btn-login { width: 100%; padding: 12px; background: linear-gradient(135deg, #8b9d83, #9db0a0); color: #fff; border: none; border-radius: 10px; font-size: 16px; font-weight: 600; letter-spacing: 1px; }
.btn-login:hover { opacity: 0.9; transform: translateY(-1px); box-shadow: 0 4px 12px rgba(139,157,131,0.3); }
.btn-login:disabled { opacity: 0.6; cursor: not-allowed; transform: none; }
.link-text { text-align: center; margin-top: 24px; font-size: 14px; color: var(--text-muted); }
</style>
