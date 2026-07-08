<template>
  <div class="admin-page">
    <h2>举报处理</h2>
    <div class="filters">
      <button v-for="s in statuses" :key="s.value" :class="{ active: status === s.value }" @click="status = s.value; fetchReports()">{{ s.label }}</button>
    </div>
    <LoadingState v-if="loading" />
    <EmptyState v-else-if="reports.length === 0" text="暂无举报记录" />
    <div v-else class="report-list">
      <div class="table-header"><span>编号</span><span>举报人</span><span>对象</span><span>原因</span><span>状态</span><span>时间</span></div>
      <div v-for="r in reports" :key="r.id" class="table-row" @click="viewDetail(r)">
        <span>#{{ r.id }}</span>
        <span>{{ r.reporterName || r.reporterId }}</span>
        <span>[{{ targetTypeMap[r.targetType] }}] {{ r.targetSummary }}</span>
        <span>{{ r.reason }}</span>
        <OrderStatusTag :status="r.status" />
        <span>{{ r.createdAt?.slice(0, 10) }}</span>
      </div>
    </div>

    <!-- 举报详情弹窗 -->
    <div v-if="detailVisible" class="modal-overlay" @click.self="detailVisible = false">
      <div class="modal-card" style="width:600px">
        <h4>举报详情 #{{ currentReport?.id }}</h4>
        <div v-if="currentReport">
          <p>举报人：{{ currentReport.reporterName }}</p>
          <p>对象类型：{{ targetTypeMap[currentReport.targetType] }}</p>
          <p>对象：{{ currentReport.targetSummary }}</p>
          <p>原因：{{ currentReport.reason }}</p>
          <p v-if="currentReport.description">描述：{{ currentReport.description }}</p>
          <p>状态：<OrderStatusTag :status="currentReport.status" /></p>

          <!-- 待处理操作 -->
          <template v-if="currentReport.status === 'PENDING' || currentReport.status === 'PROCESSING'">
            <div class="form-group" style="margin-top:16px">
              <label>处理动作</label>
              <div class="radio-group">
                <label><input type="radio" v-model="action" value="accept" /> 受理</label>
                <label><input type="radio" v-model="action" value="reject" /> 驳回</label>
              </div>
            </div>
            <div class="form-group">
              <label>处理备注 *</label>
              <textarea v-model="adminNote" rows="3"></textarea>
            </div>
            <div class="modal-actions">
              <button class="btn-cancel" @click="detailVisible = false">取消</button>
              <button class="btn-primary" @click="handleReport">提交处理</button>
            </div>
          </template>

          <!-- 申诉中操作 -->
          <template v-if="currentReport.status === 'APPEALING'">
            <p style="margin-top:12px">申诉理由：{{ currentReport.appealReason }}</p>
            <div class="form-group" style="margin-top:12px">
              <label>审查决定</label>
              <div class="radio-group">
                <label><input type="radio" v-model="appealDecision" value="UPHELD" /> 维持原判</label>
                <label><input type="radio" v-model="appealDecision" value="OVERTURNED" /> 改判</label>
              </div>
            </div>
            <div class="form-group">
              <label>审查备注 *</label>
              <textarea v-model="adminNote" rows="3"></textarea>
            </div>
            <div class="modal-actions">
              <button class="btn-cancel" @click="detailVisible = false">取消</button>
              <button class="btn-primary" @click="handleAppeal">提交审查</button>
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminAPI } from '../../api/modules.js'
import LoadingState from '../../components/common/LoadingState.vue'
import EmptyState from '../../components/common/EmptyState.vue'
import OrderStatusTag from '../../components/common/OrderStatusTag.vue'

const reports = ref([])
const loading = ref(false)
const status = ref('PENDING')
const detailVisible = ref(false)
const currentReport = ref(null)
const action = ref('accept')
const adminNote = ref('')
const appealDecision = ref('UPHELD')

const statuses = [
  { value: 'PENDING', label: '待处理' }, { value: 'PROCESSING', label: '处理中' },
  { value: 'ACCEPTED', label: '已受理' }, { value: 'REJECTED', label: '已驳回' },
  { value: 'APPEALING', label: '申诉中' }
]
const targetTypeMap = { PRODUCT: '商品', USER: '用户', MESSAGE: '消息' }

const fetchReports = async () => {
  loading.value = true
  try { const res = await adminAPI.getReports({ status: status.value, size: 100 }); reports.value = res.data?.content || [] } catch (e) {} finally { loading.value = false }
}
const viewDetail = (r) => {
  currentReport.value = r; adminNote.value = ''; action.value = 'accept'; appealDecision.value = 'UPHELD'; detailVisible.value = true
}
const handleReport = async () => {
  if (!adminNote.value.trim()) { alert('请填写处理备注'); return }
  try {
    if (action.value === 'accept') {
      const act = currentReport.value.targetType === 'PRODUCT' ? 'OFF_PRODUCT' : currentReport.value.targetType === 'USER' ? 'DISABLE_USER' : 'WARN'
      await adminAPI.acceptReport(currentReport.value.id, { action: act, note: adminNote.value })
    } else {
      await adminAPI.rejectReport(currentReport.value.id, { note: adminNote.value })
    }
    detailVisible.value = false; fetchReports()
  } catch (e) { alert(e?.message || '操作失败') }
}
const handleAppeal = async () => {
  if (!adminNote.value.trim()) { alert('请填写审查备注'); return }
  try { await adminAPI.reviewAppeal(currentReport.value.id, { decision: appealDecision.value, note: adminNote.value }); detailVisible.value = false; fetchReports() } catch (e) { alert(e?.message || '操作失败') }
}

onMounted(fetchReports)
</script>

<style scoped>
.admin-page h2 { margin-bottom: 16px; }
.filters { display: flex; gap: 4px; margin-bottom: 16px; flex-wrap: wrap; }
.filters button { padding: 4px 12px; border: 1px solid #e8e8e8; background: #fff; border-radius: 4px; font-size: 13px; }
.filters button.active { background: #1890ff; color: #fff; border-color: #1890ff; }
.table-header, .table-row { display: grid; grid-template-columns: 60px 80px 1fr 1fr 80px 100px; gap: 8px; align-items: center; padding: 10px 16px; font-size: 14px; }
.table-header { font-weight: 500; color: #666; border-bottom: 1px solid #f0f0f0; background: #fff; border-radius: 8px 8px 0 0; }
.table-row { background: #fff; border-bottom: 1px solid #f5f5f5; cursor: pointer; }
.table-row:hover { background: #fafafa; }
.table-row:last-child { border-radius: 0 0 8px 8px; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.45); z-index: 1000; display: flex; align-items: center; justify-content: center; }
.modal-card { background: #fff; border-radius: 8px; padding: 24px; max-height: 80vh; overflow-y: auto; }
.modal-card h4 { margin-bottom: 16px; }
.modal-card p { margin-bottom: 6px; font-size: 14px; }
.form-group { margin-bottom: 16px; }
.form-group label { display: block; margin-bottom: 6px; font-size: 14px; }
.form-group textarea { width: 100%; padding: 8px 12px; border: 1px solid #d9d9d9; border-radius: 4px; font-size: 14px; resize: vertical; }
.radio-group label { display: block; margin-bottom: 4px; font-size: 14px; }
.modal-actions { display: flex; justify-content: flex-end; gap: 12px; }
.btn-cancel { padding: 8px 20px; border: 1px solid #d9d9d9; background: #fff; border-radius: 4px; }
.btn-primary { padding: 8px 20px; background: #1890ff; color: #fff; border: none; border-radius: 4px; }
</style>
