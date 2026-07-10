<template>
  <div class="orders-page">
    <div class="page-header">
      <h2>我的订单</h2>
      <button class="refresh-btn" @click="fetchOrders" :disabled="loading">刷新</button>
    </div>

    <!-- 买/卖 + 类型 -->
    <div class="tab-row">
      <div class="tabs">
        <button :class="{ active: role === 'buy' }" @click="role = 'buy'">我买的</button>
        <button :class="{ active: role === 'sell' }" @click="role = 'sell'">我卖的</button>
      </div>
      <div class="type-tabs">
        <button :class="{ active: orderType === 'all' }" @click="orderType = 'all'">全部</button>
        <button :class="{ active: orderType === 'CASH' }" @click="orderType = 'CASH'">现金</button>
        <button :class="{ active: orderType === 'SWAP' }" @click="orderType = 'SWAP'">交换</button>
      </div>
    </div>

    <!-- 状态筛选 -->
    <div class="status-row">
      <button v-for="s in currentStatuses" :key="s.value"
        :class="{ active: status === s.value }" @click="status = s.value">
        {{ s.label }}
      </button>
    </div>

    <LoadingState v-if="loading" />
    <EmptyState v-else-if="orders.length === 0" text="暂无订单" actionText="去逛逛" @action="$router.push('/')" />
    <div v-else class="order-list">
      <div v-for="o in orders" :key="o.id" class="order-card"
        :class="{ 'is-dispute': o.status === 'DISPUTE' }"
        @click="$router.push(o._type === 'SWAP' ? `/swap/${o.id}` : `/order/${o.id}`)">
        <img v-if="o.productImage" :src="o.productImage" class="thumb" />
        <div v-else class="thumb-placeholder"></div>
        <div class="info">
          <div class="title-line">
            <span class="title">{{ o.productTitle }}</span>
            <span class="tag" :class="o._type">{{ o._type === 'SWAP' ? '交换' : '现金' }}</span>
            <span v-if="o.status === 'DISPUTE'" class="tag dispute">纠纷</span>
          </div>
          <div class="meta-line">
            <span class="amount" v-if="o._type === 'CASH'">¥{{ o.amount?.toFixed(2) }}</span>
            <span class="amount swap" v-else>以物易物</span>
            <span class="sep">·</span>
            <span>{{ o.counterpartyName || (role === 'buy' ? o.sellerName : o.buyerName) }}</span>
            <span class="sep">·</span>
            <span>{{ o.createdAt?.substring(0, 10) }}</span>
          </div>
        </div>
        <div class="right">
          <OrderStatusTag :status="o.status" />
          <span class="arrow">›</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { orderAPI, swapAPI } from '../api/modules.js'
import OrderStatusTag from '../components/common/OrderStatusTag.vue'
import LoadingState from '../components/common/LoadingState.vue'
import EmptyState from '../components/common/EmptyState.vue'

const role = ref('buy')
const orderType = ref('all')
const status = ref('ALL')
const orders = ref([])
const loading = ref(false)

const cashStatuses = [
  { value: 'ALL', label: '全部' },
  { value: 'PENDING_PAY', label: '待付款' },
  { value: 'PAID', label: '待发货' },
  { value: 'SHIPPED', label: '已发货' },
  { value: 'DISPUTE', label: '纠纷' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'CANCELLED', label: '已取消' },
]

const swapStatuses = [
  { value: 'ALL', label: '全部' },
  { value: 'PENDING_CONFIRM', label: '待确认' },
  { value: 'CONFIRMED', label: '待发货' },
  { value: 'BOTH_SHIPPED', label: '已发货' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'REJECTED', label: '已拒绝' },
  { value: 'CANCELLED', label: '已取消' },
]

const currentStatuses = computed(() => orderType.value === 'SWAP' ? swapStatuses : cashStatuses)

const fetchOrders = async () => {
  loading.value = true
  try {
    const params = { role: role.value, size: 50 }
    if (status.value !== 'ALL') params.status = status.value
    let allOrders = []
    if (orderType.value === 'all' || orderType.value === 'CASH') {
      const r = await orderAPI.getMyOrders(params)
      allOrders.push(...(r.data?.content || []).map(o => ({ ...o, _type: 'CASH' })))
    }
    if (orderType.value === 'all' || orderType.value === 'SWAP') {
      const r = await swapAPI.getMySwaps(params)
      allOrders.push(...(r.data?.content || []).map(o => ({ ...o, _type: 'SWAP' })))
    }
    orders.value = allOrders.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
  } catch (e) {
    orders.value = []
  } finally {
    loading.value = false
  }
}

watch(orderType, () => { status.value = 'ALL'; fetchOrders() })
watch([role, status], fetchOrders)
onMounted(fetchOrders)
</script>

<style scoped>
.orders-page { max-width: 860px; margin: 0 auto; }

.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { font-size: 20px; font-weight: 600; }
.refresh-btn { padding: 5px 14px; background: #fff; border: 1px solid #d9d9d9; border-radius: 4px; font-size: 13px; cursor: pointer; color: #666; }
.refresh-btn:hover { border-color: #1890ff; color: #1890ff; }
.refresh-btn:disabled { color: #ccc; cursor: not-allowed; }

.tab-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.tabs { display: flex; }
.tabs button { padding: 6px 20px; background: #fff; border: 1px solid #d9d9d9; font-size: 14px; cursor: pointer; }
.tabs button:first-child { border-radius: 4px 0 0 4px; }
.tabs button:last-child { border-radius: 0 4px 4px 0; }
.tabs button.active { background: #1890ff; color: #fff; border-color: #1890ff; }

.type-tabs { display: flex; gap: 4px; }
.type-tabs button { padding: 5px 14px; background: #fafafa; border: 1px solid #e8e8e8; border-radius: 14px; font-size: 13px; cursor: pointer; }
.type-tabs button.active { background: #1890ff; color: #fff; border-color: #1890ff; }

.status-row { display: flex; gap: 4px; flex-wrap: wrap; margin-bottom: 16px; }
.status-row button { padding: 3px 10px; background: #fff; border: 1px solid #e8e8e8; border-radius: 4px; font-size: 12px; cursor: pointer; color: #666; }
.status-row button.active { background: #1890ff; color: #fff; border-color: #1890ff; }

.order-list { display: flex; flex-direction: column; gap: 6px; }
.order-card { display: flex; align-items: center; gap: 14px; padding: 14px 16px; background: #fff; border-radius: 8px; border: 1px solid #f0f0f0; cursor: pointer; transition: box-shadow .2s; }
.order-card:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.order-card.is-dispute { border-color: #ffccc7; background: #fffbfb; }

.thumb { width: 56px; height: 56px; object-fit: cover; border-radius: 6px; flex-shrink: 0; background: #f5f5f5; }
.thumb-placeholder { width: 56px; height: 56px; border-radius: 6px; flex-shrink: 0; background: #f5f5f5; }

.info { flex: 1; min-width: 0; }
.title-line { display: flex; align-items: center; gap: 6px; margin-bottom: 6px; }
.title { font-size: 14px; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.tag { padding: 0 5px; border-radius: 2px; font-size: 10px; font-weight: 500; flex-shrink: 0; }
.tag.CASH { background: #e6f7ff; color: #1890ff; }
.tag.SWAP { background: #f6ffed; color: #52c41a; }
.tag.dispute { background: #fff2f0; color: #ff4d4f; border: 1px solid #ffccc7; }

.meta-line { display: flex; align-items: center; gap: 4px; font-size: 12px; color: #999; }
.amount { color: #ff4d4f; font-weight: 500; }
.amount.swap { color: #52c41a; }
.sep { color: #ddd; }

.right { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.arrow { font-size: 16px; color: #ccc; }
</style>