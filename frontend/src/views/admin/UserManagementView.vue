<template>
  <div class="admin-page">
    <h2>用户管理</h2>
    <LoadingState v-if="loading" />
    <div v-else class="table">
      <div class="table-header"><span>用户名</span><span>角色</span><span>注册时间</span><span>状态</span><span>操作</span></div>
      <div v-for="u in users" :key="u.id" class="table-row">
        <span>{{ u.username }}</span>
        <span>{{ u.role === 'ADMIN' ? '管理员' : '普通用户' }}</span>
        <span>{{ u.createdAt?.slice(0, 10) }}</span>
        <span :class="u.enabled ? 'text-success' : 'text-danger'">{{ u.enabled ? '启用' : '禁用' }}</span>
        <span>
          <button v-if="u.enabled" class="btn-danger" @click="toggleStatus(u)">禁用</button>
          <button v-else class="btn-primary" @click="toggleStatus(u)">启用</button>
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminAPI } from '../../api/modules.js'
import LoadingState from '../../components/common/LoadingState.vue'

const users = ref([])
const loading = ref(false)

const fetchUsers = async () => {
  loading.value = true
  try { const res = await adminAPI.getUsers({ size: 200 }); users.value = res.data?.content || [] } catch (e) {} finally { loading.value = false }
}
const toggleStatus = async (u) => {
  if (!confirm(u.enabled ? '禁用后用户不能登录或继续操作，历史数据将保留。确认禁用吗？' : '确认启用该用户吗？')) return
  try { await adminAPI.toggleUserStatus(u.id, !u.enabled); fetchUsers() } catch (e) { alert(e?.message || '操作失败') }
}

onMounted(fetchUsers)
</script>

<style scoped>
.admin-page h2 { margin-bottom: 16px; }
.table { background: var(--card-bg); border-radius: 8px; }
.table-header, .table-row { display: grid; grid-template-columns: 1fr 1fr 1fr 80px 80px; gap: 12px; align-items: center; padding: 12px 16px; font-size: 14px; }
.table-header { font-weight: 500; border-bottom: 1px solid var(--bg); color: var(--text-secondary); }
.table-row { border-bottom: 1px solid var(--bg); }
.text-success { color: var(--success); }
.text-danger { color: var(--danger); }
.btn-danger { padding: 4px 12px; border: 1px solid var(--danger); color: var(--danger); background: var(--card-bg); border-radius: 4px; font-size: 12px; }
.btn-primary { padding: 4px 12px; background: var(--primary); color: #fff; border: none; border-radius: 4px; font-size: 12px; }
@media (max-width: 600px) { .action-buttons,.actions { flex-direction: column; }
  .detail-layout,.admin-page { padding: 8px; }
  button { width: 100%; }
}
</style>
