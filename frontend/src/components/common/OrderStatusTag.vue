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
.status-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  line-height: 20px;
}
.status-warning { background: #fff7e6; color: #fa8c16; border: 1px solid #ffd591; }
.status-info { background: #e6f7ff; color: #1890ff; border: 1px solid #91d5ff; }
.status-success { background: #f6ffed; color: #52c41a; border: 1px solid #b7eb8f; }
.status-default { background: #f5f5f5; color: #999; border: 1px solid #d9d9d9; }
</style>
