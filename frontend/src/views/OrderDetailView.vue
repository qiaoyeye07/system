<template>
  <div class="order-detail-page">
    <LoadingState v-if="loading" />
    <ErrorState v-else-if="error" :message="error" @retry="fetchOrder" />
    <template v-else-if="order">
      <button class="btn-back" @click="$router.back()">← 返回订单列表</button>
      <div v-if="msg" class="msg-tip" :class="msgType">{{ msg }}</div>
      <div class="detail-card">
        <h3>订单概要</h3>
        <div class="info-grid">
          <span>订单编号：{{ order.orderNo }}</span>
          <span>商品：{{ order.productTitle }}</span>
          <span>金额：¥{{ order.amount?.toFixed(2) }}</span>
          <span>买家：{{ order.buyerName }}</span>
          <span>卖家：{{ order.sellerName }}</span>
          <span>状态：<OrderStatusTag :status="order.status" /></span>
          <span>创建时间：{{ order.createdAt }}</span>
        </div>
      </div>

      <div v-if="order.logisticsInfo" class="detail-card">
        <h4>物流信息</h4>
        <p>{{ order.logisticsInfo }}</p>
      </div>

      <div class="detail-card">
        <h4>操作</h4>
        <div class="action-buttons">
          <template v-if="order.status === 'PENDING_PAY' && isBuyer">
            <button class="btn-primary" @click="confirmPay">付款</button>
            <button class="btn-danger" @click="confirmCancel">取消订单</button>
          </template>
          <template v-if="order.status === 'PAID' && isBuyer && !order.refundReason">
            <button @click="showRefundDialog = true">申请退款</button>
          </template>
          <template v-if="order.status === 'PAID' && isBuyer && order.refundReason && !sellerRejectedRefund">
            <p class="refund-info">退款中：{{ order.refundReason }}</p>
            <button @click="doCancelRefund">取消退款申请</button>
          </template>
          <template v-if="order.status === 'PAID' && isBuyer && order.refundReason && sellerRejectedRefund">
            <p class="refund-info dispute-info">卖家已拒绝退款：{{ order.refundReason }}</p>
            <button class="btn-warn" @click="showEscalateDialog = true">申请管理员介入</button>
          </template>
          <template v-if="order.status === 'PAID' && isSeller && order.refundReason && !sellerRejectedRefund">
            <p class="refund-info">买家申请退款：{{ order.refundReason }}</p>
            <button class="btn-primary" @click="doAgreeRefund">同意退款</button>
            <button class="btn-danger" @click="doRejectRefund">拒绝退款</button>
          </template>
          <template v-if="order.status === 'PAID' && isSeller && order.refundReason && sellerRejectedRefund">
            <p class="refund-info dispute-info">已拒绝退款申请，等待买家处理</p>
            <button class="btn-warn" @click="showEscalateDialog = true">申请管理员介入</button>
          </template>
          <template v-if="order.status === 'PAID' && isSeller && !order.cancelReason">
            <button class="btn-primary" @click="showShipDialog = true">确认发货</button>
            <button class="btn-danger" @click="confirmCancel">取消订单</button>
          </template>
          <template v-if="order.status === 'SHIPPED' && isBuyer && !order.refundReason">
            <button class="btn-primary" @click="confirmReceive">确认收货</button>
          </template>
          <template v-if="order.status === 'RECEIVED' && isBuyer && !order.refundReason">
            <p class="hint">收货后 3 天内未申请退款，订单将自动完成</p>
            <button @click="showRefundDialog = true">申请退款</button>
          </template>
          <template v-if="order.status === 'SHIPPED' && isSeller && order.refundReason && !sellerRejectedRefund">
            <p class="refund-info">买家申请退款：{{ order.refundReason }}</p>
            <button class="btn-primary" @click="doAgreeRefund">同意退款</button>
            <button class="btn-danger" @click="doRejectRefund">拒绝退款</button>
          </template>
          <template v-if="order.status === 'SHIPPED' && isSeller && order.refundReason && sellerRejectedRefund">
            <p class="refund-info dispute-info">已拒绝退款申请，等待买家处理</p>
            <button class="btn-warn" @click="showEscalateDialog = true">申请管理员介入</button>
          </template>
          <template v-if="order.status === 'DISPUTE' && isBuyer">
            <p class="refund-info dispute-info">已提交管理员仲裁，请等待处理</p>
            <p v-if="order.refundReason" class="refund-detail">申诉理由：{{ order.refundReason }}</p>
          </template>
          <template v-if="order.status === 'DISPUTE' && isSeller">
            <p class="refund-info dispute-info">买家已申请管理员介入，等待管理员裁决</p>
          </template>
          <template v-if="order.status === 'COMPLETED' && isParticipant">
            <button @click="showRating = true">评价</button>
          </template>
          <template v-if="isDeletable">
            <button class="btn-delete" @click="confirmDelete">删除订单</button>
          </template>
        </div>
      </div>

      <!-- 退款弹窗 -->
      <div v-if="showRefundDialog" class="modal-overlay" @click.self="showRefundDialog = false">
        <div class="modal-card">
          <h4>申请退款</h4>
          <div class="form-group">
            <label>退款原因 *</label>
            <textarea v-model="refundReason" rows="3" maxlength="500" placeholder="请描述退款原因..."></textarea>
          </div>
          <div class="modal-actions">
            <button class="btn-cancel" @click="showRefundDialog = false">取消</button>
            <button class="btn-primary" :disabled="!refundReason.trim()" @click="doRefund">提交申请</button>
          </div>
        </div>
      </div>

      <!-- 发货弹窗 -->
      <div v-if="showShipDialog" class="modal-overlay" @click.self="showShipDialog = false">
        <div class="modal-card">
          <h4>确认发货</h4>
          <div class="form-group">
            <label>物流/自提信息 *</label>
            <input v-model="shipInfo" type="text" placeholder="快递单号或自提说明" />
          </div>
          <div class="modal-actions">
            <button class="btn-cancel" @click="showShipDialog = false">取消</button>
            <button class="btn-primary" @click="confirmShip">确认发货</button>
          </div>
        </div>
      </div>

      <!-- 管理员介入弹窗 -->
      <div v-if="showEscalateDialog" class="modal-overlay" @click.self="showEscalateDialog = false">
        <div class="modal-card">
          <h4>申请管理员介入</h4>
          <p class="modal-desc">将提交给管理员进行仲裁处理</p>
          <div class="form-group">
            <label>申请理由 *</label>
            <textarea v-model="escalateReason" rows="3" maxlength="500" placeholder="请描述纠纷原因..."></textarea>
          </div>
          <div class="modal-actions">
            <button class="btn-cancel" @click="showEscalateDialog = false">取消</button>
            <button class="btn-primary" :disabled="!escalateReason.trim()" @click="submitEscalate">提交</button>
          </div>
        </div>
      </div>

      <!-- 评价弹窗 -->
      <div v-if="showRating" class="modal-overlay" @click.self="showRating = false">
        <div class="modal-card">
          <h4>评价</h4>
          <StarRating v-model="ratingScore" :showText="true" />
          <div class="form-group" style="margin-top:16px">
            <label>评价内容</label>
            <textarea v-model="ratingComment" rows="4" maxlength="500" placeholder="写下本次交易体验，比如沟通、商品情况、发货速度等"></textarea>
          </div>
          <div class="modal-actions" style="margin-top:16px">
            <button class="btn-cancel" @click="showRating = false">取消</button>
            <button class="btn-primary" @click="doRating">提交评价</button>
          </div>
        </div>
      </div>

      <ConfirmDialog :visible="confirmVisible" title="确认操作" :message="confirmMsg"
        @confirm="handleConfirm" @cancel="confirmVisible = false" />
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { orderAPI, swapAPI, ratingAPI } from '../api/modules.js'
import LoadingState from '../components/common/LoadingState.vue'
import ErrorState from '../components/common/ErrorState.vue'
import OrderStatusTag from '../components/common/OrderStatusTag.vue'
import StarRating from '../components/common/StarRating.vue'
import ConfirmDialog from '../components/common/ConfirmDialog.vue'

const props = defineProps({ id: [String, Number] })
const router = useRouter()
const order = ref(null)
const loading = ref(true)
const error = ref('')
const msg = ref('')
const msgType = ref('success')
const showShipDialog = ref(false)
const showRefundDialog = ref(false)
const refundReason = ref('')
const showEscalateDialog = ref(false)
const escalateReason = ref('')
const orderLogs = ref([])
const showRating = ref(false)
const confirmVisible = ref(false)
const confirmMsg = ref('')
const confirmAction = ref(null)
const shipInfo = ref('')
const ratingScore = ref(5)
const ratingComment = ref('')

const userStr = localStorage.getItem('user')
const user = userStr ? JSON.parse(userStr) : null
const isBuyer = computed(() => user?.id === order.value?.buyerId)
const isSeller = computed(() => user?.id === order.value?.sellerId)
const isParticipant = computed(() => isBuyer.value || isSeller.value)
const isDeletable = computed(() =>
  isParticipant.value && ['CANCELLED', 'COMPLETED'].includes(order.value?.status)
)
const sellerRejectedRefund = computed(() =>
  orderLogs.value.some(log => log.actionType === 'REJECT_REFUND')
)

const fetchOrder = async () => {
  loading.value = true
  error.value = ''
  try {
    const res = await orderAPI.getDetail(props.id)
    order.value = res.data
    // 也加载操作日志用于判断卖家是否已拒绝退款
    try {
      const logRes = await orderAPI.getLogs(props.id)
      orderLogs.value = logRes.data || []
    } catch (e) { orderLogs.value = [] }
  } catch (e) {
    try {
      const res = await swapAPI.getDetail(props.id)
      order.value = res.data
      orderLogs.value = []
    } catch (e2) {
      error.value = e?.message || '加载失败'
    }
  } finally { loading.value = false }
}

const showMsg = (text, type = 'success') => { msg.value = text; msgType.value = type; setTimeout(() => msg.value = '', 3000) }

const confirmPay = () => { confirmMsg.value = '确认付款 ¥' + order.value.amount?.toFixed(2) + '？'; confirmAction.value = doPay; confirmVisible.value = true }
const doPay = async () => {
  try { await orderAPI.pay(props.id); confirmVisible.value = false; showMsg('付款成功'); fetchOrder() } catch (e) { showMsg(e?.message || '操作失败', 'error') }
}
const confirmShip = () => { confirmMsg.value = '确认已将此商品发出？'; confirmAction.value = doShip; confirmVisible.value = true }
const doShip = async () => {
  try { await orderAPI.ship(props.id, { logisticsInfo: shipInfo.value }); showShipDialog.value = false; confirmVisible.value = false; showMsg('发货成功'); fetchOrder() } catch (e) { showMsg(e?.message || '操作失败', 'error') }
}
const confirmReceive = () => { confirmMsg.value = '确认收货后无法退回，请确认已收到货物。'; confirmAction.value = doReceive; confirmVisible.value = true }
const doReceive = async () => {
  try { await orderAPI.receive(props.id); confirmVisible.value = false; showMsg('收货成功'); fetchOrder() } catch (e) { showMsg(e?.message || '操作失败', 'error') }
}
const confirmComplete = () => { confirmMsg.value = '确认后交易完成，将无法申请退款。确认吗？'; confirmAction.value = doComplete; confirmVisible.value = true }
const doComplete = async () => {
  try { await orderAPI.complete(props.id); confirmVisible.value = false; showMsg('交易已完成'); fetchOrder() } catch (e) { showMsg(e?.message || '操作失败', 'error') }
}
const confirmCancel = () => { confirmMsg.value = '取消后交易终止。确认取消吗？'; confirmAction.value = doCancel; confirmVisible.value = true }
const doCancel = async () => {
  try { await orderAPI.cancel(props.id, { reason: '取消订单' }); confirmVisible.value = false; showMsg('订单已取消'); fetchOrder() } catch (e) { showMsg(e?.message || '操作失败', 'error') }
}
const requestCancel = async () => {
  try { await orderAPI.cancel(props.id, { reason: '买家申请取消' }); showMsg('取消申请已提交'); fetchOrder() } catch (e) { showMsg(e?.message || '操作失败', 'error') }
}
const submitCancelRequest = async () => {
  const reason = cancelReason.value === cancelCustom ? cancelCustomText.value : cancelReason.value
  try { await orderAPI.cancel(props.id, { reason }); showCancelDialog.value = false; showMsg('取消申请已提交'); fetchOrder() } catch (e) { showMsg(e?.message || '操作失败', 'error') }
}
const doCancelCancelRequest = async () => {
  try { await orderAPI.rejectCancel(props.id); showMsg('已撤销退款申请'); fetchOrder() } catch (e) { showMsg(e?.message || '操作失败', 'error') }
}
const doAgreeCancel = async () => {
  try { await orderAPI.agreeCancel(props.id); showMsg('已同意退款，订单已取消'); fetchOrder() } catch (e) { showMsg(e?.message || '操作失败', 'error') }
}
const doRejectCancel = async () => {
  try { await orderAPI.rejectCancel(props.id); showMsg('已拒绝退款申请'); fetchOrder() } catch (e) { showMsg(e?.message || '操作失败', 'error') }
}
const doRefund = async () => {
  try { await orderAPI.refund(props.id, { reason: refundReason.value }); showRefundDialog.value = false; showMsg('退款申请已提交'); fetchOrder() } catch (e) { showMsg(e?.message || '操作失败', 'error') }
}
const doAgreeRefund = async () => {
  try { await orderAPI.agreeRefund(props.id); showMsg('已同意退款'); fetchOrder() } catch (e) { showMsg(e?.message || '操作失败', 'error') }
}
const doRejectRefund = async () => {
  try { await orderAPI.rejectRefund(props.id); showMsg('已拒绝退款'); fetchOrder() } catch (e) { showMsg(e?.message || '操作失败', 'error') }
}
const submitEscalate = async () => {
  try {
    await orderAPI.escalate(props.id, { reason: escalateReason.value })
    showEscalateDialog.value = false; showMsg('已申请管理员介入'); fetchOrder()
  } catch (e) { showMsg(e?.message || '操作失败', 'error') }
}
const doCancelRefund = async () => {
  try { await orderAPI.cancelRefund(props.id); showMsg('已取消退款申请'); fetchOrder() } catch (e) { showMsg(e?.message || '操作失败', 'error') }
}
const doRating = async () => {
  try {
    await ratingAPI.submit({ orderId: Number(props.id), score: ratingScore.value, comment: ratingComment.value.trim() })
    showRating.value = false
    ratingComment.value = ''
    showMsg('评价已提交')
  } catch (e) { showMsg(e?.message || '操作失败', 'error') }
}
const handleConfirm = () => { if (confirmAction.value) confirmAction.value() }
const confirmDelete = () => { confirmMsg.value = '删除后无法恢复，确认删除该订单？'; confirmAction.value = doDelete; confirmVisible.value = true }
const doDelete = async () => {
  try {
    if (order.value?.orderType === 'SWAP') {
      await swapAPI.deleteSwap(props.id)
    } else {
      await orderAPI.deleteOrder(props.id)
    }
    confirmVisible.value = false; showMsg('订单已删除'); setTimeout(() => router.push('/orders'), 1500)
  } catch (e) { showMsg(e?.message || '删除失败', 'error') }
}

let timer = null
onMounted(() => {
  fetchOrder()
  timer = setInterval(fetchOrder, 30000)
})
onBeforeUnmount(() => { if (timer) clearInterval(timer) })
</script>

<style scoped>
.order-detail-page { padding: 0 0 40px; max-width: 800px; margin: 0 auto; }
.btn-back { background: none; border: none; color: #1890ff; font-size: 14px; margin-bottom: 16px; }
.msg-tip { padding: 8px 12px; border-radius: 4px; margin-bottom: 12px; font-size: 14px; }
.msg-tip.success { background: #f6ffed; color: #52c41a; border: 1px solid #b7eb8f; }
.msg-tip.error { background: #fff2f0; color: #ff4d4f; border: 1px solid #ffccc7; }
.detail-card { background: #fff; border-radius: 8px; padding: 20px; margin-bottom: 16px; }
.detail-card h3, .detail-card h4 { margin-bottom: 12px; }
.info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; font-size: 14px; color: #666; }
.action-buttons { display: flex; gap: 12px; flex-wrap: wrap; }
.action-buttons button { padding: 8px 20px; border-radius: 4px; font-size: 14px; border: 1px solid #d9d9d9; background: #fff; }
.btn-primary { background: #1890ff !important; color: #fff !important; border-color: #1890ff !important; }
.btn-danger { color: #ff4d4f !important; border-color: #ff4d4f !important; }
.btn-delete { color: #999 !important; border-color: #d9d9d9 !important; font-size: 12px !important; padding: 4px 12px !important; }
.refund-info { padding: 8px 12px; background: #fff7e6; border: 1px solid #ffd591; border-radius: 4px; font-size: 13px; color: #d46b08; margin-bottom: 8px; }
.hint { padding: 8px 12px; background: #e6f7ff; border: 1px solid #91d5ff; border-radius: 4px; font-size: 13px; color: #1890ff; margin-bottom: 8px; }
.dispute-info { background: #fff2f0; border-color: #ffccc7; color: #ff4d4f; }
.refund-detail { font-size: 13px; color: #666; margin-bottom: 8px; }
.reason-options { display: flex; flex-direction: column; gap: 6px; margin-bottom: 8px; }
.reason-option { display: flex; align-items: center; gap: 6px; padding: 6px 10px; border: 1px solid #e8e8e8; border-radius: 4px; font-size: 13px; cursor: pointer; }
.reason-option.selected { border-color: #1890ff; background: #e6f7ff; }
.reason-option input { margin: 0; }
.custom-input { margin-top: 8px; width: 100%; padding: 6px 10px; border: 1px solid #d9d9d9; border-radius: 4px; font-size: 13px; }
.modal-desc { font-size: 13px; color: #999; margin-bottom: 12px; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.45); z-index: 1000; display: flex; align-items: center; justify-content: center; }
.modal-card { background: #fff; border-radius: 8px; padding: 24px; width: 420px; }
.modal-card h4 { margin-bottom: 16px; }
.form-group { margin-bottom: 16px; }
.form-group label { display: block; margin-bottom: 6px; font-size: 14px; }
.form-group input { width: 100%; padding: 8px 12px; border: 1px solid #d9d9d9; border-radius: 4px; font-size: 14px; }
.form-group textarea { width: 100%; padding: 8px 12px; border: 1px solid #d9d9d9; border-radius: 4px; font-size: 14px; resize: vertical; }
.modal-actions { display: flex; justify-content: flex-end; gap: 12px; }
.btn-cancel { padding: 8px 20px; border: 1px solid #d9d9d9; background: #fff; border-radius: 4px; }
</style>
