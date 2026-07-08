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
          <span>发起方：{{ order.buyer?.username }}</span>
          <span>接收方：{{ order.seller?.username }}</span>
        </div>
      </div>
      <div class="swap-items">
        <div class="swap-item">
          <h4>{{ order.buyer?.username }} 提供</h4>
          <p>{{ order.swapProduct?.title || '交换商品' }}</p>
        </div>
        <div class="swap-arrow">⇄</div>
        <div class="swap-item">
          <h4>{{ order.seller?.username }} 提供</h4>
          <p>{{ order.product?.title }}</p>
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
            <button class="btn-primary" @click="doAgree">同意交换</button>
            <button class="btn-danger" @click="doReject">拒绝交换</button>
          </template>
          <template v-if="order.status === 'PENDING_CONFIRM' && isBuyer">
            <button @click="doWithdraw">撤回提议</button>
          </template>
          <template v-if="order.status === 'CONFIRMED'">
            <button class="btn-primary" @click="showShipDialog = true">确认发货</button>
            <button @click="doCancelSwap">申请取消</button>
          </template>
          <template v-if="order.status === 'BOTH_SHIPPED'">
            <button class="btn-primary" @click="doReceive">确认收货</button>
          </template>
          <template v-if="order.status === 'COMPLETED' && isParticipant">
            <button @click="showRating = true">评价对方</button>
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
            <button class="btn-primary" @click="doShip">确认</button>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { swapAPI, ratingAPI } from '../api/modules.js'
import LoadingState from '../components/common/LoadingState.vue'
import ErrorState from '../components/common/ErrorState.vue'
import OrderStatusTag from '../components/common/OrderStatusTag.vue'

const props = defineProps({ id: [String, Number] })
const order = ref(null)
const loading = ref(true)
const error = ref('')
const msg = ref('')
const msgType = ref('success')
const showShipDialog = ref(false)
const shipInfo = ref('')
const showRating = ref(false)

const userStr = localStorage.getItem('user')
const user = userStr ? JSON.parse(userStr) : null
const isBuyer = computed(() => user?.id === order.value?.buyer?.id)
const isSeller = computed(() => user?.id === order.value?.seller?.id)
const isParticipant = computed(() => isBuyer.value || isSeller.value)

const showMsg = (text, type = 'success') => { msg.value = text; msgType.value = type; setTimeout(() => msg.value = '', 3000) }

const fetchOrder = async () => {
  loading.value = true
  try { const res = await swapAPI.getDetail(props.id); order.value = res.data } catch (e) { error.value = e?.message } finally { loading.value = false }
}
const doAgree = async () => { try { await swapAPI.agree(props.id); showMsg('已同意交换'); fetchOrder() } catch (e) { showMsg(e?.message, 'error') } }
const doReject = async () => { try { await swapAPI.reject(props.id); showMsg('已拒绝'); fetchOrder() } catch (e) { showMsg(e?.message, 'error') } }
const doWithdraw = async () => { try { await swapAPI.withdraw(props.id); showMsg('已撤回'); fetchOrder() } catch (e) { showMsg(e?.message, 'error') } }
const doShip = async () => { try { await swapAPI.ship(props.id, { logisticsInfo: shipInfo.value }); showShipDialog.value = false; showMsg('已发货'); fetchOrder() } catch (e) { showMsg(e?.message, 'error') } }
const doReceive = async () => { try { await swapAPI.receive(props.id); showMsg('已收货'); fetchOrder() } catch (e) { showMsg(e?.message, 'error') } }
const doCancelSwap = async () => { try { await swapAPI.cancel(props.id, { reason: '申请取消' }); showMsg('取消申请已提交'); fetchOrder() } catch (e) { showMsg(e?.message, 'error') } }

onMounted(fetchOrder)
</script>

<style scoped>
.swap-detail-page { max-width: 800px; margin: 0 auto; }
.btn-back { background: none; border: none; color: #1890ff; font-size: 14px; margin-bottom: 16px; }
.msg-tip { padding: 8px 12px; border-radius: 4px; margin-bottom: 12px; font-size: 14px; }
.msg-tip.success { background: #f6ffed; color: #52c41a; border: 1px solid #b7eb8f; }
.msg-tip.error { background: #fff2f0; color: #ff4d4f; border: 1px solid #ffccc7; }
.detail-card { background: #fff; border-radius: 8px; padding: 20px; margin-bottom: 16px; }
.detail-card h3, .detail-card h4 { margin-bottom: 12px; }
.info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; font-size: 14px; color: #666; }
.swap-items { display: grid; grid-template-columns: 1fr auto 1fr; gap: 24px; align-items: center; margin-bottom: 16px; }
.swap-item { background: #fff; border-radius: 8px; padding: 20px; text-align: center; }
.swap-arrow { font-size: 28px; color: #1890ff; }
.action-buttons { display: flex; gap: 12px; flex-wrap: wrap; }
.action-buttons button { padding: 8px 20px; border-radius: 4px; font-size: 14px; border: 1px solid #d9d9d9; background: #fff; }
.btn-primary { background: #1890ff !important; color: #fff !important; border-color: #1890ff !important; }
.btn-danger { color: #ff4d4f !important; border-color: #ff4d4f !important; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.45); z-index: 1000; display: flex; align-items: center; justify-content: center; }
.modal-card { background: #fff; border-radius: 8px; padding: 24px; width: 420px; }
.modal-card h4 { margin-bottom: 16px; }
.form-group { margin-bottom: 16px; }
.form-group label { display: block; margin-bottom: 6px; font-size: 14px; }
.form-group input { width: 100%; padding: 8px 12px; border: 1px solid #d9d9d9; border-radius: 4px; font-size: 14px; }
.modal-actions { display: flex; justify-content: flex-end; gap: 12px; }
.btn-cancel { padding: 8px 20px; border: 1px solid #d9d9d9; background: #fff; border-radius: 4px; }
</style>
