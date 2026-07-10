<template>
  <span class="status-tag" :class="statusClass">{{ statusText }}</span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: { type: String, required: true }
})

const statusMap = {
  // 现金订单
  PENDING_PAY: '待付款', PAID: '待发货', SHIPPED: '已发货',
  RECEIVED: '已收货', DISPUTE: '纠纷中', COMPLETED: '已完成', CANCELLED: '已取消',
  // 交换订单
  PENDING_CONFIRM: '待确认', CONFIRMED: '已确认', BOTH_SHIPPED: '双方已发货',
  REJECTED: '已拒绝',
  // 商品
  ACTIVE: '在售', SOLD: '已售', OFF: '已下架',
  // 举报
  PENDING: '待处理', PROCESSING: '处理中', ACCEPTED: '已受理', APPEALING: '申诉中'
}

const statusText = computed(() => statusMap[props.status] || props.status)

const statusClass = computed(() => {
  const s = props.status
  if (['PENDING_PAY','PAID','PENDING_CONFIRM','PENDING','PROCESSING','APPEALING'].includes(s)) return 'status-warning'
  if (['SHIPPED','BOTH_SHIPPED','DISPUTE'].includes(s)) return 'status-info'
  if (['COMPLETED','ACTIVE','ACCEPTED'].includes(s)) return 'status-success'
  if (['CANCELLED','REJECTED','SOLD','OFF'].includes(s)) return 'status-default'
  return 'status-default'
})
</script>

<style scoped>
.status-tag { display: inline-flex; align-items: center; gap: 4px; padding: 3px 10px; border-radius: 20px; font-size: 12px; font-weight: 600; }
.status-tag::before { content: ''; width: 7px; height: 7px; border-radius: 50%; flex-shrink: 0; }
.status-warning { background: #fdf2e0; color: #b8860b; }
.status-warning::before { background: #d4a017; }
.status-info { background: #e8f0e9; color: #3d6b4f; }
.status-info::before { background: #5c8a5f; }
.status-success { background: #e6f0e7; color: #2d5a2e; }
.status-success::before { background: #3d7a3e; }
.status-default { background: #f0efed; color: #6e6a66; }
.status-default::before { background: #8a8580; }
</style>
