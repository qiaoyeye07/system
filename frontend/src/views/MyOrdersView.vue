<template>
  <div class="orders-page">
    <h2>我的订单</h2>
    <div class="tabs">
      <button :class="{ active: role === 'BUYER' }" @click="role = 'BUYER'">我买的</button>
      <button :class="{ active: role === 'SELLER' }" @click="role = 'SELLER'">我卖的</button>
    </div>
    <div class="filters">
      <button v-for="s in statuses" :key="s.value" :class="{ active: status === s.value }" @click="status = s.value">
        {{ s.label }}
      </button>
    </div>
    <LoadingState v-if="loading" />
    <EmptyState v-else-if="orders.length === 0" text="暂无相关订单" actionText="去逛逛" @action="$router.push('/')" />
    <div v-else class="order-list">
      <div v-for="o in orders" :key="o.id" class="order-item" @click="$router.push(`/order/${o.id}`)">
        <img v-if="o.productImage" :src="o.productImage" class="order-img" />
        <div class="order-info">
          <h4>{{ o.productTitle }}</h4>
          <p class="order-amount">¥{{ o.amount?.toFixed(2) }}</p>
          <p class="order-meta">{{ o.counterpartyName }} · {{ o.createdAt }}</p>
        </div>
        <OrderStatusTag :status="o.status" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { orderAPI, swapAPI } from '../api/modules.js'
import OrderStatusTag from '../components/common/OrderStatusTag.vue'
import LoadingState from '../components/common/LoadingState.vue'
import EmptyState from '../components/common/EmptyState.vue'

const role = ref('BUYER')
const status = ref('ALL')
const orders = ref([])
const loading = ref(false)

const statuses = [
  { value: 'ALL', label: '全部' },
  { value: 'PENDING_PAY', label: '待付款' },
  { value: 'PAID', label: '已付款' },
  { value: 'SHIPPED', label: '已发货' },
  { value: 'RECEIVED', label: '已收货' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'CANCELLED', label: '已取消' },
]

const fetchOrders = async () => {
  loading.value = true
  try {
    const params = { role: role.value, size: 50 }
    if (status.value !== 'ALL') params.status = status.value
    const cashRes = await orderAPI.getMyOrders(params)
    const swapRes = await swapAPI.getMySwaps(params)
    const cashOrders = (cashRes.data?.content || []).map(o => ({ ...o, _type: 'CASH' }))
    const swapOrders = (swapRes.data?.content || []).map(o => ({ ...o, _type: 'SWAP' }))
    orders.value = [...cashOrders, ...swapOrders].sort((a, b) =>
      new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    )
  } catch (e) {
    orders.value = []
  } finally {
    loading.value = false
  }
}

watch([role, status], fetchOrders)
onMounted(fetchOrders)
</script>

<style scoped>
.orders-page h2 { margin-bottom: 16px; }
.tabs { display: flex; gap: 0; margin-bottom: 12px; }
.tabs button { padding: 8px 24px; background: #fff; border: 1px solid #d9d9d9; font-size: 14px; }
.tabs button.active { background: #1890ff; color: #fff; border-color: #1890ff; }
.tabs button:first-child { border-radius: 4px 0 0 4px; }
.tabs button:last-child { border-radius: 0 4px 4px 0; }
.filters { display: flex; gap: 4px; flex-wrap: wrap; margin-bottom: 16px; }
.filters button { padding: 4px 12px; background: #fff; border: 1px solid #e8e8e8; border-radius: 4px; font-size: 13px; }
.filters button.active { background: #1890ff; color: #fff; border-color: #1890ff; }
.order-list { display: flex; flex-direction: column; gap: 8px; }
.order-item { display: flex; align-items: center; gap: 16px; padding: 16px; background: #fff; border-radius: 8px; cursor: pointer; }
.order-item:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.order-img { width: 64px; height: 64px; object-fit: cover; border-radius: 4px; }
.order-info { flex: 1; }
.order-info h4 { font-size: 15px; margin-bottom: 4px; }
.order-amount { color: #ff4d4f; font-weight: bold; margin-bottom: 4px; }
.order-meta { font-size: 12px; color: #999; }
</style>
