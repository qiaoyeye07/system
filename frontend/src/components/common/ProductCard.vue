<template>
  <div class="product-card" @click="$router.push(`/product/${product.id}`)">
    <div class="card-image">
      <img v-if="firstImage" :src="firstImage" :alt="product.title" />
      <div v-else class="no-image">📷<br/>暂无图片</div>
    </div>
    <div class="card-body">
      <h3 class="card-title">{{ product.title }}</h3>
      <p class="card-price">¥{{ product.price?.toFixed(2) }}</p>
      <div class="card-meta">
        <span class="condition">{{ conditionText }}</span>
        <span class="time">{{ timeText }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  product: { type: Object, required: true }
})

const conditionMap = { NEW: '全新', LIKE_NEW: '几乎全新', USED: '有使用痕迹' }
const conditionText = computed(() => conditionMap[props.product.condition] || '')

const firstImage = computed(() => {
  if (!props.product.images) return null
  return props.product.images.split(',')[0]
})

const timeText = computed(() => {
  if (!props.product.createdAt) return ''
  const diff = Date.now() - new Date(props.product.createdAt).getTime()
  const minutes = Math.floor(diff / 60000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时前`
  return `${Math.floor(hours / 24)}天前`
})
</script>

<style scoped>
.product-card { background: var(--card-bg); border-radius: 12px; overflow: hidden; cursor: pointer; transition: all .25s ease; box-shadow: 0 1px 4px rgba(0,0,0,.04); }
.product-card:hover { transform: translateY(-4px); box-shadow: 0 8px 24px rgba(0,0,0,.12); }
.card-image { width: 100%; height: 180px; overflow: hidden; background: var(--bg); }
.card-image img { width: 100%; height: 100%; object-fit: cover; transition: transform .3s ease; }
.product-card:hover .card-image img { transform: scale(1.05); }
.no-image { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 6px; height: 100%; color: var(--text-muted); font-size: 13px; }
.card-body { padding: 12px 14px; }
.card-title { font-size: 14px; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-bottom: 6px; color: var(--text); }
.card-price { color: var(--danger); font-size: 18px; font-weight: bold; margin-bottom: 6px; }
.card-meta { display: flex; justify-content: space-between; font-size: 12px; color: var(--text-muted); }
</style>
