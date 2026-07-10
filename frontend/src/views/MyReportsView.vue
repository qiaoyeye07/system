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
        <span class="target-summary">{{ r.targetSummary || '[' + targetTypeMap[r.targetType] + '] ID:' + r.targetId }}</span>
        <span class="reason">{{ reasonMap[r.reason] || r.reason }}</span>
        <OrderStatusTag :status="r.status" />
        <span class="time">{{ r.createdAt?.slice(0, 10) }}</span>
        <button v-if="r.status === 'ACCEPTED' || r.status === 'REJECTED'" class="btn-appeal" @click.stop="showAppeal(r)">申诉</button>
      </div>
    </div>
    <!-- 举报详情弹窗 -->
    <div v-if="selectedReport" class="modal-overlay" @click.self="selectedReport = null">
      <div class="modal-card">
        <h4>举报详情 #{{ selectedReport.id }}</h4>
        <div class="detail-grid">
          <div class="detail-item">
            <span class="detail-label">状态</span>
            <OrderStatusTag :status="selectedReport.status" />
          </div>
          <div class="detail-item">
            <span class="detail-label">举报对象</span>
            <span>{{ selectedReport.targetSummary || '[' + targetTypeMap[selectedReport.targetType] + '] ID:' + selectedReport.targetId }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">举报原因</span>
            <span>{{ reasonMap[selectedReport.reason] || selectedReport.reason }}</span>
          </div>
          <div class="detail-item" v-if="selectedReport.description">
            <span class="detail-label">详细描述</span>
            <span>{{ selectedReport.description }}</span>
          </div>
          <div class="detail-item" v-if="selectedReport.adminNote">
            <span class="detail-label">处理备注</span>
            <span>{{ selectedReport.adminNote }}</span>
          </div>
          <div class="detail-item" v-if="selectedReport.appealReason">
            <span class="detail-label">我的申诉理由</span>
            <span>{{ selectedReport.appealReason }}</span>
          </div>
          <div class="detail-item" v-if="selectedReport.appealResult">
            <span class="detail-label">申诉结果</span>
            <span :class="selectedReport.appealResult === 'OVERTURNED' ? 'text-green' : 'text-red'">
              {{ selectedReport.appealResult === 'UPHELD' ? '维持原判' : '改判' }}
            </span>
          </div>
          <div class="detail-item">
            <span class="detail-label">提交时间</span>
            <span>{{ selectedReport.createdAt?.slice(0, 16) }}</span>
          </div>
        </div>
        <div class="modal-actions">
          <button class="btn-cancel" @click="selectedReport = null">关闭</button>
          <button v-if="selectedReport.status === 'ACCEPTED' || selectedReport.status === 'REJECTED'" class="btn-primary" @click="showAppeal(selectedReport); selectedReport = null">申诉</button>
        </div>
      </div>
    </div>
    <!-- 申诉弹窗 -->
    <div v-if="appealVisible" class="modal-overlay" @click.self="appealVisible = false">
      <div class="modal-card">
        <h4>提交申诉</h4>
        <p class="appeal-context">对「{{ reasonMap[appealReport?.reason] || appealReport?.reason }}」的{{ appealReport?.status === 'ACCEPTED' ? '受理' : '驳回' }}决定进行申诉</p>
        <div class="form-group">
          <label>申诉理由 *</label>
          <textarea v-model="appealReason" rows="4" maxlength="500" placeholder="请说明不服处理决定的理由"></textarea>
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
const selectedReport = ref(null)
const status = ref('ALL')
const appealVisible = ref(false)
const appealReportId = ref(null)
const appealReport = ref(null)
const appealReason = ref('')

const statuses = [
  { value: 'ALL', label: '全部' }, { value: 'PENDING', label: '待处理' },
  { value: 'ACCEPTED', label: '已受理' },
  { value: 'REJECTED', label: '已驳回' }, { value: 'APPEALING', label: '申诉中' }
]
const targetTypeMap = { PRODUCT: '商品', USER: '用户', MESSAGE: '消息' }
const reasonMap = { FAKE_DESC: '描述不符', PROHIBITED: '违禁品', FRAUD: '诈骗', HARASS: '骚扰', OTHER: '其他' }

const fetchReports = async () => {
  loading.value = true
  try {
    const res = await reportAPI.getMyReports({ page: 0, size: 100 })
    const allReports = res.data?.content || []
    if (status.value === 'ALL') {
      reports.value = allReports
    } else {
      reports.value = allReports.filter(r => r.status === status.value)
    }
  } catch (e) {} finally { loading.value = false }
}

const showAppeal = (r) => { appealReportId.value = r.id; appealReport.value = r; appealReason.value = ''; appealVisible.value = true }
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
.filters button { padding: 4px 12px; border: 1px solid var(--border); background: var(--card-bg); border-radius: 4px; font-size: 13px; }
.filters button.active { background: var(--primary); color: #fff; border-color: var(--primary); }
.report-item { display: flex; align-items: center; gap: 16px; padding: 14px 16px; background: var(--card-bg); border-radius: 8px; margin-bottom: 8px; cursor: pointer; font-size: 14px; }
.report-item:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.target-type { color: var(--primary); font-weight: 500; min-width: 40px; }
.target-summary { flex: 1; color: var(--text); font-size: 13px; }
.reason { color: var(--text-secondary); }
.time { color: var(--text-muted); font-size: 12px; }
.btn-appeal { padding: 4px 12px; border: 1px solid var(--primary); color: var(--primary); background: var(--card-bg); border-radius: 4px; font-size: 12px; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.45); z-index: 1000; display: flex; align-items: center; justify-content: center; }
.modal-card { background: var(--card-bg); border-radius: 8px; padding: 24px; width: 480px; }
.modal-card h4 { margin-bottom: 16px; }
.form-group { margin-bottom: 16px; }
.form-group label { display: block; margin-bottom: 6px; font-size: 14px; }
.form-group textarea { width: 100%; padding: 8px 12px; border: 1px solid var(--border); border-radius: 4px; font-size: 14px; resize: vertical; }
.modal-actions { display: flex; justify-content: flex-end; gap: 12px; }
.btn-cancel { padding: 8px 20px; border: 1px solid var(--border); background: var(--card-bg); border-radius: 4px; }
.btn-primary { padding: 8px 20px; background: var(--primary); color: #fff; border: none; border-radius: 4px; }
.detail-grid { margin-bottom: 16px; }
.detail-item { display: flex; gap: 12px; margin-bottom: 10px; font-size: 14px; }
.detail-label { color: var(--text-muted); min-width: 70px; flex-shrink: 0; }
.text-green { color: var(--success); font-weight: 500; }
.text-red { color: var(--danger); font-weight: 500; }
.appeal-context { font-size: 13px; color: var(--text-muted); margin-bottom: 16px; background: var(--bg); padding: 8px 12px; border-radius: 4px; }
</style>
