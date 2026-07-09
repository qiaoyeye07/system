<template>
  <div class="admin-page">
    <h2>纠纷处理</h2>
    <LoadingState v-if="loading" />
    <EmptyState v-else-if="disputes.length === 0" text="暂无待处理纠纷" />
    <div v-else class="dispute-list">
      <div v-for="d in disputes" :key="d.id" class="dispute-card">
        <div class="dispute-header">
          <span>{{ d.orderNo }}</span>
          <span>{{ d.buyerName }} / {{ d.sellerName }}</span>
          <span>¥{{ d.amount?.toFixed(2) }}</span>
          <span>{{ d.createdAt?.slice(0,10) }}</span>
        </div>
        <p>退款原因：{{ d.refundReason }}</p>
        <button @click="viewDetail(d)">查看详情并裁定</button>
      </div>
    </div>

    <!-- 纠纷详情弹窗 -->
    <div v-if="detailVisible" class="modal-overlay" @click.self="detailVisible = false">
      <div class="modal-card" style="width:600px">
        <h4>纠纷详情 — {{ currentDispute?.orderNo }}</h4>
        <div v-if="disputeDetail">
          <p>买家：{{ disputeDetail.buyerName }} | 卖家：{{ disputeDetail.sellerName }}</p>
          <p>商品：{{ disputeDetail.productTitle }} | 金额：¥{{ disputeDetail.amount?.toFixed(2) }}</p>
          <p>退款原因：{{ disputeDetail.refundReason }}</p>
          <div class="form-group" style="margin-top:16px">
            <label>裁定 *</label>
            <div class="radio-group">
              <label><input type="radio" v-model="decision" value="APPROVE_REFUND" /> 同意退款（取消订单，商品恢复在售）</label>
              <label><input type="radio" v-model="decision" value="MAINTAIN_STATUS" /> 维持原状（交易完成）</label>
            </div>
          </div>
          <div class="form-group">
            <label>裁定理由 *</label>
            <textarea v-model="judgeReason" rows="3"></textarea>
          </div>
          <div class="modal-actions">
            <button class="btn-cancel" @click="detailVisible = false">取消</button>
            <button class="btn-primary" @click="submitJudge">提交裁定</button>
          </div>
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

const disputes = ref([])
const loading = ref(false)
const detailVisible = ref(false)
const currentDispute = ref(null)
const disputeDetail = ref(null)
const decision = ref('APPROVE_REFUND')
const judgeReason = ref('')

const fetchDisputes = async () => {
  loading.value = true
  try { const res = await adminAPI.getDisputes(); disputes.value = res.data?.content || [] } catch (e) {} finally { loading.value = false }
}
const viewDetail = async (d) => {
  currentDispute.value = d
  try {
    const res = await adminAPI.getOrderDetail(d.id)
    disputeDetail.value = res.data
    detailVisible.value = true
  } catch (e) { alert('加载失败') }
}
const submitJudge = async () => {
  if (!judgeReason.value.trim()) { alert('请填写裁定理由'); return }
  try {
    await adminAPI.judgeDispute(currentDispute.value.id, { action: decision.value, reason: judgeReason.value })
    detailVisible.value = false; judgeReason.value = ''
    fetchDisputes()
  } catch (e) { alert(e?.message || '操作失败') }
}

onMounted(fetchDisputes)
</script>

<style scoped>
.admin-page h2 { margin-bottom: 16px; }
.dispute-card { background: #fff; border-radius: 8px; padding: 16px; margin-bottom: 12px; }
.dispute-header { display: flex; gap: 24px; font-size: 14px; margin-bottom: 8px; color: #666; }
.dispute-card p { font-size: 14px; margin-bottom: 8px; }
.dispute-card button { padding: 6px 16px; background: #1890ff; color: #fff; border: none; border-radius: 4px; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.45); z-index: 1000; display: flex; align-items: center; justify-content: center; }
.modal-card { background: #fff; border-radius: 8px; padding: 24px; max-height: 80vh; overflow-y: auto; }
.modal-card h4 { margin-bottom: 16px; }
.modal-card p { margin-bottom: 8px; font-size: 14px; }
.form-group { margin-bottom: 16px; }
.form-group label { display: block; margin-bottom: 6px; font-size: 14px; }
.form-group textarea { width: 100%; padding: 8px 12px; border: 1px solid #d9d9d9; border-radius: 4px; font-size: 14px; resize: vertical; }
.radio-group label { display: block; margin-bottom: 6px; font-size: 14px; }
.modal-actions { display: flex; justify-content: flex-end; gap: 12px; margin-top: 16px; }
.btn-cancel { padding: 8px 20px; border: 1px solid #d9d9d9; background: #fff; border-radius: 4px; }
.btn-primary { padding: 8px 20px; background: #1890ff; color: #fff; border: none; border-radius: 4px; }
</style>
