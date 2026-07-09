<template>
  <div class="register-page">
    <div class="register-card">
      <h2>注册账号</h2>
      <form @submit.prevent="handleRegister">
        <div class="form-group">
          <label>用户名 <span class="required">*</span></label>
          <input v-model="form.username" type="text" placeholder="3-50个字符，支持中英文、数字"
            :class="{ 'input-error': errors.username }" required />
          <p v-if="errors.username" class="field-error">{{ errors.username }}</p>
        </div>
        <div class="form-group">
          <label>密码 <span class="required">*</span></label>
          <input v-model="form.password" type="password" placeholder="8-128个字符"
            :class="{ 'input-error': errors.password }" required />
          <p v-if="errors.password" class="field-error">{{ errors.password }}</p>
        </div>
        <div class="form-group">
          <label>确认密码 <span class="required">*</span></label>
          <input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码"
            :class="{ 'input-error': errors.confirmPassword }" required />
          <p v-if="errors.confirmPassword" class="field-error">{{ errors.confirmPassword }}</p>
        </div>
        <div v-if="errorMsg" class="error-tip">{{ errorMsg }}</div>
        <button type="submit" class="btn-primary" :disabled="loading || !canSubmit">注册</button>
      </form>
      <p class="link-text">已有账号？<router-link to="/login">去登录</router-link></p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { authAPI } from '../api/modules.js'
import { useUserStore } from '../store/user.js'

import { ref, reactive, computed, watch } from 'vue'

const router = useRouter()
const route = useRoute()
const store = useUserStore()
const loading = ref(false)
const errorMsg = ref('')
const form = reactive({ username: '', password: '', confirmPassword: '' })

// Real-time field errors
const errors = reactive({ username: '', password: '', confirmPassword: '' })

const validateUsername = (v) => {
  if (!v) return ''
  const trimmed = v.trim()
  if (trimmed.length < 3) return '用户名至少 3 个字符'
  if (!/^[一-龥a-zA-Z0-9_\-]+$/.test(trimmed)) return '含无效字符，支持中英文、数字、下划线、连字符'
  return ''
}
const validatePassword = (v) => {
  if (!v) return ''
  if (v.length < 8) return '密码至少 8 个字符'
  return ''
}
const validateConfirm = (v) => {
  if (!v) return ''
  if (v !== form.password) return '两次输入的密码不一致'
  return ''
}

watch(() => form.username,  v => { errors.username  = validateUsername(v) })
watch(() => form.password,  v => { errors.password  = validatePassword(v); if (form.confirmPassword) errors.confirmPassword = validateConfirm(form.confirmPassword) })
watch(() => form.confirmPassword, v => { errors.confirmPassword = validateConfirm(v) })

const hasError = computed(() => errors.username || errors.password || errors.confirmPassword)
const canSubmit = computed(() => form.username.trim() && form.password && form.confirmPassword && !hasError.value)

const handleRegister = async () => {
  errorMsg.value = ''
  if (hasError.value) return
  loading.value = true
  try {
    await authAPI.register({ username: form.username.trim(), password: form.password, confirmPassword: form.confirmPassword })
    // Auto-login: register then login via store
    const loginRes = await authAPI.login({ username: form.username.trim(), password: form.password })
    store.login(loginRes.data.token, {
      id: loginRes.data.userId,
      username: loginRes.data.username,
      role: loginRes.data.role
    })
    router.push(route.query.redirect || '/')
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
.field-error { color: #ff4d4f; font-size: 12px; margin-top: 4px; }
.input-error { border-color: #ff4d4f !important; }
.btn-primary { width: 100%; padding: 10px; background: #1890ff; color: #fff; border: none; border-radius: 4px; font-size: 16px; }
.btn-primary:hover { background: #40a9ff; }
.btn-primary:disabled { background: #91d5ff; cursor: not-allowed; }
.link-text { text-align: center; margin-top: 16px; font-size: 14px; color: #999; }
</style>
