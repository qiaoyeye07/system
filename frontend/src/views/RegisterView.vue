<template>
  <div class="register-page">
    <div class="register-card">
      <h2>注册账号</h2>
      <form @submit.prevent="handleRegister">
        <div class="form-group">
          <label>用户名 <span class="required">*</span></label>
          <input
            v-model="form.username"
            type="text"
            placeholder="3-50 个字符，支持中英文、数字、下划线、连字符"
            :class="{ 'input-error': errors.username }"
            required
          />
          <p v-if="errors.username" class="field-error">{{ errors.username }}</p>
        </div>
        <div class="form-group">
          <label>密码 <span class="required">*</span></label>
          <div class="password-wrap">
            <input
              v-model="form.password"
              :type="showPwd ? 'text' : 'password'"
              placeholder="8-128 个字符"
              :class="{ 'input-error': errors.password }"
              required
            />
            <span class="pwd-toggle" @click="showPwd = !showPwd">{{ showPwd ? '隐藏' : '显示' }}</span>
          </div>
          <p v-if="errors.password" class="field-error">{{ errors.password }}</p>
        </div>
        <div class="form-group">
          <label>确认密码 <span class="required">*</span></label>
          <div class="password-wrap">
            <input
              v-model="form.confirmPassword"
              :type="showPwd ? 'text' : 'password'"
              placeholder="请再次输入密码"
              :class="{ 'input-error': errors.confirmPassword }"
              required
            />
          </div>
          <p v-if="errors.confirmPassword" class="field-error">{{ errors.confirmPassword }}</p>
        </div>
        <div v-if="errorMsg" class="error-tip">{{ errorMsg }}</div>
        <button type="submit" class="btn-primary" :disabled="loading || !canSubmit">
          {{ loading ? '注册中...' : '注册' }}
        </button>
      </form>
      <p class="link-text">已有账号？<router-link to="/login">去登录</router-link></p>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { authAPI } from '../api/modules.js'
import { useUserStore } from '../store/user.js'

const route = useRoute()
const router = useRouter()
const store = useUserStore()
const loading = ref(false)
const errorMsg = ref('')
const showPwd = ref(false)
const form = reactive({ username: '', password: '', confirmPassword: '' })
const errors = reactive({ username: '', password: '', confirmPassword: '' })

watch(() => form.username, () => { errorMsg.value = '' })
watch(() => form.password, () => { errorMsg.value = '' })

const validateUsername = (value) => {
  if (!value) return ''
  const trimmed = value.trim()
  if (trimmed.length < 3) return '用户名至少 3 个字符'
  if (!/^[\u4e00-\u9fa5a-zA-Z0-9_-]+$/.test(trimmed)) return '用户名只能包含中英文、数字、下划线、连字符'
  return ''
}

const validatePassword = (value) => {
  if (!value) return ''
  if (value.length < 8) return '密码至少 8 个字符'
  if (value.length > 128) return '密码最多 128 个字符'
  return ''
}

const validateConfirmPassword = (value) => {
  if (!value) return ''
  if (value !== form.password) return '两次输入的密码不一致'
  return ''
}

watch(() => form.username, value => {
  errors.username = validateUsername(value)
})

watch(() => form.password, value => {
  errors.password = validatePassword(value)
  if (form.confirmPassword) errors.confirmPassword = validateConfirmPassword(form.confirmPassword)
})

watch(() => form.confirmPassword, value => {
  errors.confirmPassword = validateConfirmPassword(value)
})

const hasError = computed(() => errors.username || errors.password || errors.confirmPassword)
const canSubmit = computed(() => (
  form.username.trim()
  && form.password
  && form.confirmPassword
  && !hasError.value
))

const handleRegister = async () => {
  errors.username = validateUsername(form.username)
  errors.password = validatePassword(form.password)
  errors.confirmPassword = validateConfirmPassword(form.confirmPassword)
  errorMsg.value = ''
  if (hasError.value || !canSubmit.value) return

  loading.value = true
  try {
    await authAPI.register({
      username: form.username.trim(),
      password: form.password,
      confirmPassword: form.confirmPassword
    })
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
.password-wrap { position: relative; }
.password-wrap input { padding-right: 52px; }
.pwd-toggle { position: absolute; right: 10px; top: 50%; transform: translateY(-50%); cursor: pointer; user-select: none; font-size: 12px; color: #1890ff; }
.form-group input:focus { border-color: #1890ff; outline: none; }
.error-tip { background: #fff2f0; border: 1px solid #ffccc7; color: #ff4d4f; padding: 8px 12px; border-radius: 4px; margin-bottom: 16px; font-size: 14px; }
.field-error { color: #ff4d4f; font-size: 12px; margin-top: 4px; }
.input-error { border-color: #ff4d4f !important; }
.btn-primary { width: 100%; padding: 10px; background: #1890ff; color: #fff; border: none; border-radius: 4px; font-size: 16px; }
.btn-primary:hover { background: #40a9ff; }
.btn-primary:disabled { background: #91d5ff; cursor: not-allowed; }
.link-text { text-align: center; margin-top: 16px; font-size: 14px; color: #999; }
</style>
