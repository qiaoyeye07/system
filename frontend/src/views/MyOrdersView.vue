<template>
  <div class="orders-page">
    <div class="page-header">
      <h2>我的订单</h2>
      <button class="refresh-btn" @click="fetchOrders" :disabled="loading">
        {{ loading ? '刷新中...' : '刷新' }}
      </button>
    </div>

    <!-- 交易类型切换：现金 / 交换 -->
    <div class="type-tabs">
      <button :class="{ active: orderType === 'all' }" @click="orderType = 'all'">全部</button>
      <button :class="{ active: orderType === 'CASH' }" @click="orderType = 'CASH'">现金交易</button>
      <button :class="{ active: orderType === 'SWAP' }" @click="orderType = 'SWAP'">以物易物</button>
    </div>

    <!-- 角色 + 状态 放在同一行 -->
    <div class="filter-row">
      <div class="role-tabs">
        <button :class="{ active: role === 'buy' }" @click="role = 'buy'">
          {{ orderType === 'SWAP' ? '我发起的' : '我买的' }}
        </button>
        <button :class="{ active: role === 'sell' }" @click="role = 'sell'">
          {{ orderType === 'SWAP' ? '我收到的' : '我卖的' }}
        </button>
      </div>
      <div class="status-filters">
        <button v-for="s in currentStatuses" :key="s.value"
          :class="{ active: status === s.value, dispute: s.value === 'DISPUTE' }"
          @click="status = s.value">
          {{ s.label }}
        </button>
      </div>
    </div>

    <LoadingState v-if="loading" />
    <EmptyState v-else-if="orders.length === 0" text="暂无相关订单" actionText="去逛逛" @action="$router.push('/')" />
    <div v-else class="order-list">
      <div v-for="o in orders" :key="o.id" class="order-item"
        :class="{ 'is-dispute': o.status === 'DISPUTE' }"
        @click="$router.push(o._type === 'SWAP' ? `/swap/${o.id}` : `/order/${o.id}`)">
        <img v-if="o.productImage" :src="o.productImage" class="order-img" />
        <div class="order-info">
          <div class="order-title-row">
            <h4>{{ o.productTitle }}</h4>
            <span class="type-tag" :class="o._type">{{ o._type === 'SWAP' ? '交换' : '现金' }}</span>
            <span v-if="o.status === 'DISPUTE'" class="dispute-tag">纠纷</span>
          </div>
          <p class="order-amount" v-if="o._type === 'CASH'">¥{{ o.amount?.toFixed(2) }}</p>
          <p class="order-amount swap-label" v-else>以物易物</p>
          <p class="order-meta">
            {{ o.counterpartyName || (role === 'buy' ? o.sellerName : o.buyerName) }}
            · {{ o.createdAt?.substring(0, 10) }}
            <span v-if="o.status === 'DISPUTE' && o.refundReason" class="dispute-reason">
              · 退款原因：{{ o.refundReason }}
            </span>
          </p>
        </div>
        <OrderStatusTag :status="o.status" />
        <span class="arrow">›</span>
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
  { value: 'PAID', label: '已付款' },
  { value: 'SHIPPED', label: '已发货' },
  { value: 'RECEIVED', label: '已收货' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'CANCELLED', label: '已取消' },
  { value: 'DISPUTE', label: '纠纷' },
]

const swapStatuses = [
  { value: 'ALL', label: '全部' },
  { value: 'PENDING_CONFIRM', label: '待确认' },
  { value: 'CONFIRMED', label: '待发货' },
  { value: 'BOTH_SHIPPED', label: '待收货' },
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
      const cashRes = await orderAPI.getMyOrders(params)
      const cashOrders = (cashRes.data?.content || []).map(o => ({ ...o, _type: 'CASH' }))
      allOrders.push(...cashOrders)
    }

    if (orderType.value === 'all' || orderType.value === 'SWAP') {
      const swapRes = await swapAPI.getMySwaps(params)
      const swapOrders = (swapRes.data?.content || []).map(o => ({ ...o, _type: 'SWAP' }))
      allOrders.push(...swapOrders)
    }

    orders.value = allOrders.sort((a, b) =>
      new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    )
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
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { margin-bottom: 0; }
.refresh-btn { padding: 6px 16px; background: #fff; border: 1px solid #d9d9d9; border-radius: 4px; font-size: 13px; cursor: pointer; }
.refresh-btn:hover { border-color: #1890ff; color: #1890ff; }
.refresh-btn:disabled { color: #ccc; cursor: not-allowed; }

/* 类型切换 */
.type-tabs { display: flex; gap: 8px; margin-bottom: 14px; }
.type-tabs button { padding: 6px 18px; background: #f5f5f5; border: 1px solid #e8e8e8; border-radius: 16px; font-size: 13px; cursor: pointer; }
.type-tabs button.active { background: #1890ff; color: #fff; border-color: #1890ff; }

/* 筛选行：角色 + 状态 */
.filter-row { display: flex; align-items: flex-start; gap: 16px; margin-bottom: 16px; }
.role-tabs { display: flex; gap: 0; flex-shrink: 0; }
.role-tabs button { padding: 7px 18px; background: #fff; border: 1px solid #d9d9d9; font-size: 13px; cursor: pointer; }
.role-tabs button.active { background: #1890ff; color: #fff; border-color: #1890ff; }
.role-tabs button:first-child { border-radius: 4px 0 0 4px; }
.role-tabs button:last-child { border-radius: 0 4px 4px 0; }

.status-filters { display: flex; gap: 4px; flex-wrap: wrap; }
.status-filters button { padding: 4px 12px; background: #fff; border: 1px solid #e8e8e8; border-radius: 4px; font-size: 13px; cursor: pointer; white-space: nowrap; }
.status-filters button.active { background: #1890ff; color: #fff; border-color: #1890ff; }
.status-filters button.dispute.active { background: #ff4d4f; border-color: #ff4d4f; }

/* 订单列表 */
.order-list { display: flex; flex-direction: column; gap: 8px; }
.order-item { display: flex; align-items: center; gap: 16px; padding: 16px; background: #fff; border-radius: 8px; cursor: pointer; border: 1px solid transparent; }
.order-item:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.order-item.is-dispute { border-color: #ff4d4f; background: #fff2f0; }
.order-img { width: 64px; height: 64px; object-fit: cover; border-radius: 4px; flex-shrink: 0; background: #f5f5f5; }
.order-info { flex: 1; min-width: 0; }
.order-title-row { display: flex; align-items: center; gap: 8px; }
.order-info h4 { font-size: 15px; margin-bottom: 4px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.type-tag { padding: 1px 6px; border-radius: 3px; font-size: 11px; font-weight: 500; flex-shrink: 0; }
.type-tag.CASH { background: #e6f7ff; color: #1890ff; }
.type-tag.SWAP { background: #f6ffed; color: #52c41a; }
.dispute-tag { padding: 1px 6px; border-radius: 3px; font-size: 11px; font-weight: 500; background: #fff2f0; color: #ff4d4f; border: 1px solid #ffccc7; flex-shrink: 0; }
.order-amount { font-weight: bold; margin-bottom: 4px; color: #ff4d4f; }
.order-amount.swap-label { color: #52c41a; font-size: 14px; }
.order-meta { font-size: 12px; color: #999; }
.dispute-reason { color: #ff4d4f; }
.arrow { font-size: 18px; color: #ccc; flex-shrink: 0; }
</style>