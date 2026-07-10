<template>
  <div class="my-products-page">
    <h2>我的商品</h2>
    <div class="filters">
      <button v-for="s in statuses" :key="s.value" :class="{ active: status === s.value }" @click="status = s.value">{{ s.label }}</button>
    </div>
    <LoadingState v-if="loading" />
    <EmptyState v-else-if="products.length === 0" text="尚未发布商品" actionText="发布第一件商品" @action="$router.push('/publish')" />
    <div v-else class="product-table">
      <div class="table-header">
        <span>图片</span><span>标题</span><span>价格</span><span>状态</span><span>时间</span><span>操作</span>
      </div>
      <div v-for="p in products" :key="p.id" class="table-row">
        <img v-if="getFirstImage(p)" :src="getFirstImage(p)" class="thumb" />
        <span v-else class="no-thumb">-</span>
        <span class="title">{{ p.title }}</span>
        <span class="price">¥{{ p.price?.toFixed(2) }}</span>
        <OrderStatusTag :status="p.status" />
        <span class="time">{{ p.createdAt?.slice(0, 10) }}</span>
        <span>
          <button class="btn-view" @click="$router.push(`/product/${p.id}`)">查看</button>
          <button v-if="p.status === 'ACTIVE'" class="btn-off" @click="handleOff(p.id)">下架</button>
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { productAPI } from '../api/modules.js'
import OrderStatusTag from '../components/common/OrderStatusTag.vue'
import LoadingState from '../components/common/LoadingState.vue'
import EmptyState from '../components/common/EmptyState.vue'

const status = ref('ALL')
const products = ref([])
const loading = ref(false)
const statuses = [
  { value: 'ALL', label: '全部' }, { value: 'ACTIVE', label: '在售' },
  { value: 'SOLD', label: '已售' }, { value: 'OFF', label: '已下架' }
]

const fetchProducts = async () => {
  loading.value = true
  try {
    const params = { size: 100 }
    if (status.value !== 'ALL') params.status = status.value
    const res = await productAPI.getMyProducts(params)
    products.value = res.data?.content || []
  } catch (e) { products.value = [] } finally { loading.value = false }
}
const handleOff = async (id) => {
  if (!confirm('下架后商品不再公开展示。确认下架吗？')) return
  try { await productAPI.offShelf(id); fetchProducts() } catch (e) { alert(e?.message || '操作失败') }
}
const getFirstImage = (p) => {
  if (!p.images) return null
  return p.images.split(',')[0]
}
watch(status, fetchProducts)
onMounted(fetchProducts)
</script>

<style scoped>
.my-products-page h2 { margin-bottom: 16px; }
.filters { display: flex; gap: 4px; margin-bottom: 16px; }
.filters button { padding: 4px 12px; border: 1px solid var(--border); background: var(--card-bg); border-radius: 4px; font-size: 13px; }
.filters button.active { background: var(--primary); color: #fff; border-color: var(--primary); }
.product-table { background: var(--card-bg); border-radius: 8px; }
.table-header, .table-row { display: grid; grid-template-columns: 60px 1fr 100px 80px 100px 80px; gap: 12px; align-items: center; padding: 12px 16px; font-size: 14px; }
.table-header { font-weight: 500; border-bottom: 1px solid var(--bg); color: var(--text-secondary); }
.table-row { border-bottom: 1px solid var(--bg); }
.table-row:last-child { border-bottom: none; }
.thumb { width: 48px; height: 48px; object-fit: cover; border-radius: 4px; }
.price { color: var(--danger); font-weight: bold; }
.time { color: var(--text-muted); font-size: 12px; }
.btn-off { padding: 4px 12px; border: 1px solid var(--danger); color: var(--danger); background: var(--card-bg); border-radius: 4px; font-size: 12px; }
.btn-view { padding: 4px 12px; border: 1px solid var(--border); background: var(--card-bg); border-radius: 4px; font-size: 12px; }
</style>
