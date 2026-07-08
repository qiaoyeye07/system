<template>
  <div class="register-page">
    <div class="register-card">
      <h2>注册账号</h2>
      <form @submit.prevent="handleRegister">
        <div class="form-group">
          <label>用户名 <span class="required">*</span></label>
          <input v-model="form.username" type="text" placeholder="3-50个字符，支持中英文、数字" required />
        </div>
        <div class="form-group">
          <label>密码 <span class="required">*</span></label>
          <input v-model="form.password" type="password" placeholder="8-128个字符" required />
        </div>
        <div class="form-group">
          <label>确认密码 <span class="required">*</span></label>
          <input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" required />
        </div>
        <div v-if="errorMsg" class="error-tip">{{ errorMsg }}</div>
        <div v-if="successMsg" class="success-tip">{{ successMsg }}</div>
        <button type="submit" class="btn-primary" :disabled="loading">注册</button>
      </form>
      <p class="link-text">已有账号？<router-link to="/login">去登录</router-link></p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { authAPI } from '../api/modules.js'

const router = useRouter()
const loading = ref(false)
const errorMsg = ref('')
const successMsg = ref('')
const form = reactive({ username: '', password: '', confirmPassword: '' })

const handleRegister = async () => {
  errorMsg.value = ''
  successMsg.value = ''
  if (form.password !== form.confirmPassword) {
    errorMsg.value = '两次输入的密码不一致'
    return
  }
  loading.value = true
  try {
    await authAPI.register({ username: form.username.trim(), password: form.password, confirmPassword: form.confirmPassword })
    successMsg.value = '注册成功，即将跳转登录页...'
    setTimeout(() => router.push('/login'), 1500)
  } catch (e) {
    errorMsg.value = e?.message || '注册失败，请重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page { display: flex; align-items: center; justify-content: center; min-height: 100vh; background: #f0f2f5; }
.register-card { width: 420px; padding: 40px; background: #fff; border-radius: 8px; box-shadow: 0 2px 12px rgba(0,0,0,0.08); }
.register-card h2 { text-align: center; margin-bottom: 24px; font-size: 22px; }
.form-group { margin-bottom: 16px; }
.form-group label { display: block; margin-bottom: 6px; font-size: 14px; color: #333; }
.required { color: #ff4d4f; }
.form-group input { width: 100%; padding: 8px 12px; border: 1px solid #d9d9d9; border-radius: 4px; font-size: 14px; }
.form-group input:focus { border-color: #1890ff; outline: none; }
.error-tip { background: #fff2f0; border: 1px solid #ffccc7; color: #ff4d4f; padding: 8px 12px; border-radius: 4px; margin-bottom: 16px; font-size: 14px; }
.success-tip { background: #f6ffed; border: 1px solid #b7eb8f; color: #52c41a; padding: 8px 12px; border-radius: 4px; margin-bottom: 16px; font-size: 14px; }
.btn-primary { width: 100%; padding: 10px; background: #1890ff; color: #fff; border: none; border-radius: 4px; font-size: 16px; }
.btn-primary:hover { background: #40a9ff; }
.btn-primary:disabled { background: #91d5ff; }
.link-text { text-align: center; margin-top: 16px; font-size: 14px; color: #999; }
</style>
