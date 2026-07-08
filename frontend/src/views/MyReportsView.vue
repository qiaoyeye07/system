<template>
  <div class="reports-page">
    <h2>我的举报</h2>
    <div class="filters">
      <button v-for="s in statuses" :key="s.value" :class="{ active: status === s.value }" @click="status = s.value; fetchReports()">{{ s.label }}</button>
    </div>
    <LoadingState v-if="loading" />
    <EmptyState v-else-if="reports.length === 0" text="暂无举报记录" />
    <div v-else class="report-list">
      <div v-for="r in reports" :key="r.id" class="report-item" @click="selectedReport = r">
        <span class="target-type">{{ targetTypeMap[r.targetType] || r.targetType }}</span>
        <span class="target-summary">{{ r.targetSummary }}</span>
        <span class="reason">{{ r.reason }}</span>
        <OrderStatusTag :status="r.status" />
        <span class="time">{{ r.createdAt?.slice(0, 10) }}</span>
        <button v-if="r.status === 'ACCEPTED'" class="btn-appeal" @click.stop="showAppeal(r)">申诉</button>
      </div>
    </div>
    <!-- 申诉弹窗 -->
    <div v-if="appealVisible" class="modal-overlay" @click.self="appealVisible = false">
      <div class="modal-card">
        <h4>提交申诉</h4>
        <div class="form-group">
          <label>申诉理由 *</label>
          <textarea v-model="appealReason" rows="3" maxlength="500" placeholder="说明申诉理由"></textarea>
        </div>
        <div class="modal-actions">
          <button class="btn-cancel" @click="appealVisible = false">取消</button>
          <button class="btn-primary" @click="submitAppeal">提交申诉</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { reportAPI } from '../api/modules.js'
import LoadingState from '../components/common/LoadingState.vue'
import EmptyState from '../components/common/EmptyState.vue'
import OrderStatusTag from '../components/common/OrderStatusTag.vue'

const reports = ref([])
const loading = ref(false)
const status = ref('ALL')
const appealVisible = ref(false)
const appealReportId = ref(null)
const appealReason = ref('')

const statuses = [
  { value: 'ALL', label: '全部' }, { value: 'PENDING', label: '待处理' },
  { value: 'ACCEPTED', label: '已受理' }, { value: 'REJECTED', label: '已驳回' }
]
const targetTypeMap = { PRODUCT: '商品', USER: '用户', MESSAGE: '消息' }

const fetchReports = async () => {
  loading.value = true
  try {
    const params = {}
    if (status.value !== 'ALL') params.status = status.value
    const res = await reportAPI.getMyReports(params)
    reports.value = res.data?.content || []
  } catch (e) {} finally { loading.value = false }
}

const showAppeal = (r) => { appealReportId.value = r.id; appealReason.value = ''; appealVisible.value = true }
const submitAppeal = async () => {
  if (!appealReason.value.trim()) { alert('请填写申诉理由'); return }
  try {
    await reportAPI.appeal(appealReportId.value, { appealReason: appealReason.value.trim() })
    appealVisible.value = false
    alert('申诉已提交')
    fetchReports()
  } catch (e) { alert(e?.message || '提交失败') }
}

onMounted(fetchReports)
</script>

<style scoped>
.reports-page h2 { margin-bottom: 16px; }
.filters { display: flex; gap: 4px; margin-bottom: 16px; }
.filters button { padding: 4px 12px; border: 1px solid #e8e8e8; background: #fff; border-radius: 4px; font-size: 13px; }
.filters button.active { background: #1890ff; color: #fff; border-color: #1890ff; }
.report-item { display: flex; align-items: center; gap: 16px; padding: 14px 16px; background: #fff; border-radius: 8px; margin-bottom: 8px; cursor: pointer; font-size: 14px; }
.report-item:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.target-type { color: #1890ff; font-weight: 500; min-width: 40px; }
.target-summary { flex: 1; }
.reason { color: #666; }
.time { color: #999; font-size: 12px; }
.btn-appeal { padding: 4px 12px; border: 1px solid #1890ff; color: #1890ff; background: #fff; border-radius: 4px; font-size: 12px; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.45); z-index: 1000; display: flex; align-items: center; justify-content: center; }
.modal-card { background: #fff; border-radius: 8px; padding: 24px; width: 480px; }
.modal-card h4 { margin-bottom: 16px; }
.form-group { margin-bottom: 16px; }
.form-group label { display: block; margin-bottom: 6px; font-size: 14px; }
.form-group textarea { width: 100%; padding: 8px 12px; border: 1px solid #d9d9d9; border-radius: 4px; font-size: 14px; resize: vertical; }
.modal-actions { display: flex; justify-content: flex-end; gap: 12px; }
.btn-cancel { padding: 8px 20px; border: 1px solid #d9d9d9; background: #fff; border-radius: 4px; }
.btn-primary { padding: 8px 20px; background: #1890ff; color: #fff; border: none; border-radius: 4px; }
</style>
