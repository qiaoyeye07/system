<template>
  <div class="swap-detail-page">
    <LoadingState v-if="loading" />
    <ErrorState v-else-if="error" :message="error" @retry="fetchOrder" />
    <template v-else-if="order">
      <button class="btn-back" @click="$router.back()">← 返回</button>
      <div v-if="msg" class="msg-tip" :class="msgType">{{ msg }}</div>
      <div class="detail-card">
        <h3>交换订单详情</h3>
        <div class="info-grid">
          <span>订单编号：{{ order.orderNo }}</span>
          <span>状态：<OrderStatusTag :status="order.status" /></span>
          <span>发起方：{{ order.buyerName }}</span>
          <span>接收方：{{ order.sellerName }}</span>
        </div>
      </div>
      <div class="swap-items">
        <div class="swap-item">
          <h4>{{ order.buyerName }} 提供</h4>
          <p>{{ order.swapProductTitle || '交换商品' }}</p>
        </div>
        <div class="swap-arrow">⇄</div>
        <div class="swap-item">
          <h4>{{ order.sellerName }} 提供</h4>
          <p>{{ order.productTitle }}</p>
        </div>
      </div>
      <div v-if="order.swapNote" class="detail-card">
        <h4>意向说明</h4>
        <p>{{ order.swapNote }}</p>
      </div>
      <div class="detail-card">
        <h4>操作</h4>
        <div class="action-buttons">
          <template v-if="order.status === 'PENDING_CONFIRM' && isSeller">
            <button class="btn-primary" @click="confirmAgree">同意交换</button>
            <button class="btn-danger" @click="confirmReject">拒绝交换</button>
          </template>
          <template v-if="order.status === 'PENDING_CONFIRM' && isBuyer">
            <button @click="doWithdraw">撤回提议</button>
          </template>
          <template v-if="order.status === 'CONFIRMED'">
            <button class="btn-primary" @click="showShipDialog = true">确认发货</button>
            <button @click="doCancelSwap">申请取消</button>
          </template>
          <template v-if="order.status === 'BOTH_SHIPPED'">
            <button class="btn-primary" @click="confirmReceive">确认收货</button>
          </template>
          <template v-if="order.status === 'COMPLETED' && isParticipant">
            <button @click="showRating = true">评价</button>
          </template>
          <template v-if="isDeletable">
            <button class="btn-delete" @click="confirmDelete">删除订单</button>
          </template>
        </div>
      </div>

      <!-- 发货弹窗 -->
      <div v-if="showShipDialog" class="modal-overlay" @click.self="showShipDialog = false">
        <div class="modal-card">
          <h4>确认发货</h4>
          <div class="form-group"><label>物流/自提信息 *</label><input v-model="shipInfo" type="text" /></div>
          <div class="modal-actions">
            <button class="btn-cancel" @click="showShipDialog = false">取消</button>
            <button class="btn-primary" @click="confirmShip">确认</button>
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
            <textarea v-model="ratingComment" rows="4" maxlength="500" placeholder="写下本次交换体验，比如沟通、物品情况、履约情况等"></textarea>
          </div>
          <div class="modal-actions" style="margin-top:16px">
            <button class="btn-cancel" @click="showRating = false">取消</button>
            <button class="btn-primary" @click="doRating">提交评价</button>
          </div>
        </div>
      </div>
    </template>

    <ConfirmDialog :visible="confirmVisible" title="确认删除" :message="confirmMsg"
      @confirm="handleConfirm" @cancel="confirmVisible = false" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { swapAPI, ratingAPI } from '../api/modules.js'
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
const shipInfo = ref('')
const showRating = ref(false)
const ratingScore = ref(5)
const ratingComment = ref('')
const confirmVisible = ref(false)
const confirmMsg = ref('')
const confirmAction = ref(null)

const userStr = localStorage.getItem('user')
const user = userStr ? JSON.parse(userStr) : null
const isBuyer = computed(() => user?.id === order.value?.buyerId)
const isSeller = computed(() => user?.id === order.value?.sellerId)
const isParticipant = computed(() => isBuyer.value || isSeller.value)
const isDeletable = computed(() =>
  isParticipant.value && ['CANCELLED', 'COMPLETED', 'REJECTED'].includes(order.value?.status)
)

const showMsg = (text, type = 'success') => { msg.value = text; msgType.value = type; setTimeout(() => msg.value = '', 3000) }

const fetchOrder = async () => {
  loading.value = true
  try { const res = await swapAPI.getDetail(props.id); order.value = res.data } catch (e) { error.value = e?.message } finally { loading.value = false }
}
const confirmAgree = () => { confirmMsg.value = '同意交换后双方商品将下架。确认同意？'; confirmAction.value = doAgree; confirmVisible.value = true }
const doAgree = async () => { try { await swapAPI.agree(props.id); confirmVisible.value = false; showMsg('已同意交换'); fetchOrder() } catch (e) { showMsg(e?.message, 'error') } }
const confirmReject = () => { confirmMsg.value = '确认拒绝此交换提议？'; confirmAction.value = doReject; confirmVisible.value = true }
const doReject = async () => { try { await swapAPI.reject(props.id); confirmVisible.value = false; showMsg('已拒绝'); fetchOrder() } catch (e) { showMsg(e?.message, 'error') } }
const doWithdraw = async () => { try { await swapAPI.withdraw(props.id); showMsg('已撤回'); fetchOrder() } catch (e) { showMsg(e?.message, 'error') } }
const confirmShip = () => { confirmMsg.value = '确认已发货？'; confirmAction.value = doShip; confirmVisible.value = true }
const doShip = async () => { try { await swapAPI.ship(props.id, { logisticsInfo: shipInfo.value }); showShipDialog.value = false; confirmVisible.value = false; showMsg('已发货'); fetchOrder() } catch (e) { showMsg(e?.message, 'error') } }
const confirmReceive = () => { confirmMsg.value = '确认已收到货物？收货后不可退回。'; confirmAction.value = doReceive; confirmVisible.value = true }
const doReceive = async () => { try { await swapAPI.receive(props.id); confirmVisible.value = false; showMsg('已收货'); fetchOrder() } catch (e) { showMsg(e?.message, 'error') } }
const doCancelSwap = async () => { try { await swapAPI.cancel(props.id, { reason: '申请取消' }); showMsg('取消申请已提交'); fetchOrder() } catch (e) { showMsg(e?.message, 'error') } }
const doRating = async () => {
  try {
    await ratingAPI.submit({ orderId: Number(props.id), score: ratingScore.value, comment: ratingComment.value.trim() })
    showRating.value = false
    ratingComment.value = ''
    showMsg('评价已提交')
  } catch (e) { showMsg(e?.message || '评价失败', 'error') }
}
const confirmDelete = () => { confirmMsg.value = '删除后无法恢复，确认删除？'; confirmAction.value = doDelete; confirmVisible.value = true }
const doDelete = async () => {
  try { await swapAPI.deleteSwap(props.id); confirmVisible.value = false; showMsg('已删除'); setTimeout(() => router.push('/orders'), 1500) } catch (e) { showMsg(e?.message || '删除失败', 'error') }
}
const handleConfirm = () => { if (confirmAction.value) confirmAction.value() }

let timer = null
onMounted(() => {
  fetchOrder()
  timer = setInterval(fetchOrder, 30000)
})
onBeforeUnmount(() => { if (timer) clearInterval(timer) })
</script>

<style scoped>
.swap-detail-page { max-width: 800px; margin: 0 auto; }
.btn-back { background: none; border: none; color: var(--primary); font-size: 14px; margin-bottom: 16px; }
.msg-tip { padding: 8px 12px; border-radius: 4px; margin-bottom: 12px; font-size: 14px; }
.msg-tip.success { background: rgba(122,154,126,0.1); color: var(--success); border: 1px solid rgba(122,154,126,0.3); }
.msg-tip.error { background: rgba(194,120,120,0.1); color: var(--danger); border: 1px solid rgba(194,120,120,0.2); }
.detail-card { background: var(--card-bg); border-radius: 8px; padding: 20px; margin-bottom: 16px; }
.detail-card h3, .detail-card h4 { margin-bottom: 12px; }
.info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; font-size: 14px; color: var(--text-secondary); }
.swap-items { display: grid; grid-template-columns: 1fr auto 1fr; gap: 24px; align-items: center; margin-bottom: 16px; }
.swap-item { background: var(--card-bg); border-radius: 8px; padding: 20px; text-align: center; }
.swap-arrow { font-size: 28px; color: var(--primary); }
.action-buttons { display: flex; gap: 12px; flex-wrap: wrap; }
.action-buttons button { padding: 8px 20px; border-radius: 4px; font-size: 14px; border: 1px solid var(--border); background: var(--card-bg); }
.btn-primary { background: var(--primary) !important; color: #fff !important; border-color: var(--primary) !important; }
.btn-danger { color: var(--danger) !important; border-color: var(--danger) !important; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.45); z-index: 1000; display: flex; align-items: center; justify-content: center; }
.modal-card { background: var(--card-bg); border-radius: 8px; padding: 24px; width: 420px; }
.modal-card h4 { margin-bottom: 16px; }
.form-group { margin-bottom: 16px; }
.form-group label { display: block; margin-bottom: 6px; font-size: 14px; }
.form-group input { width: 100%; padding: 8px 12px; border: 1px solid var(--border); border-radius: 4px; font-size: 14px; }
.modal-actions { display: flex; justify-content: flex-end; gap: 12px; }
.btn-cancel { padding: 8px 20px; border: 1px solid var(--border); background: var(--card-bg); border-radius: 4px; }
.btn-delete { color: var(--text-muted) !important; border-color: var(--border) !important; font-size: 12px !important; padding: 4px 12px !important; }
</style>
