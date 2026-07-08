<template>
  <div class="home-page">
    <div class="search-bar">
      <input v-model="keyword" type="text" placeholder="搜索商品..." @keyup.enter="handleSearch" />
      <button @click="handleSearch">搜索</button>
    </div>
    <div class="category-tags">
      <router-link v-for="cat in categories" :key="cat.id" :to="`/category/${cat.id}`" class="cat-tag">
        {{ cat.name }}
      </router-link>
    </div>
    <h3>最新发布</h3>
    <LoadingState v-if="loading" />
    <ErrorState v-else-if="error" :message="error" @retry="fetchProducts" />
    <div v-else-if="products.length === 0" class="empty-section">
      <EmptyState text="暂无在售商品，成为第一个卖家吧！" actionText="发布商品" @action="$router.push('/publish')" />
    </div>
    <div v-else class="product-grid">
      <ProductCard v-for="p in products" :key="p.id" :product="p" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { productAPI, categoryAPI } from '../api/modules.js'
import ProductCard from '../components/common/ProductCard.vue'
import LoadingState from '../components/common/LoadingState.vue'
import ErrorState from '../components/common/ErrorState.vue'
import EmptyState from '../components/common/EmptyState.vue'

const router = useRouter()
const keyword = ref('')
const products = ref([])
const categories = ref([])
const loading = ref(true)
const error = ref('')

const fetchProducts = async () => {
  loading.value = true
  error.value = ''
  try {
    const res = await productAPI.getList({ status: 'ACTIVE', sort: 'created_at,desc' })
    products.value = res.data?.content || []
  } catch (e) {
    error.value = e?.message || '加载商品失败'
  } finally {
    loading.value = false
  }
}

const fetchCategories = async () => {
  try {
    const res = await categoryAPI.getEnabled()
    categories.value = res.data || []
  } catch (e) { /* ignore */ }
}

const handleSearch = () => {
  if (keyword.value.trim()) {
    router.push({ path: '/search', query: { keyword: keyword.value.trim() } })
  }
}

onMounted(() => {
  fetchProducts()
  fetchCategories()
})
</script>

<style scoped>
.search-bar { display: flex; gap: 12px; margin-bottom: 20px; }
.search-bar input { flex: 1; padding: 10px 16px; border: 1px solid #d9d9d9; border-radius: 4px; font-size: 14px; }
.search-bar input:focus { border-color: #1890ff; outline: none; }
.search-bar button { padding: 10px 24px; background: #1890ff; color: #fff; border: none; border-radius: 4px; }
.category-tags { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 24px; }
.cat-tag { padding: 6px 16px; background: #fff; border: 1px solid #e8e8e8; border-radius: 16px; color: #333; font-size: 14px; }
.cat-tag:hover { color: #1890ff; border-color: #1890ff; }
h3 { font-size: 18px; margin-bottom: 16px; }
.product-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
@media (max-width: 900px) { .product-grid { grid-template-columns: repeat(2, 1fr); } }
</style>
