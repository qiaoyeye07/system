<template>
  <div class="home-page">
    <div class="search-bar">
      <input v-model="keyword" type="text" placeholder="搜索商品..." @keyup.enter="handleSearch" />
      <button @click="handleSearch">搜索</button>
      <button v-if="searching" class="btn-clear" @click="clearSearch">清除</button>
    </div>
    <div class="category-tags">
      <div class="cat-btn" :class="{ active: activeCatId === 0 }" @click="selectCategory(0)">
        <span class="cat-icon">📦</span><span>全部</span>
      </div>
      <div v-for="cat in categories" :key="cat.id" class="cat-btn" :class="{ active: activeCatId === cat.id }" @click="selectCategory(cat.id)">
        <span class="cat-icon">{{ catIcons[cat.name] || '📌' }}</span><span>{{ cat.name }}</span>
      </div>
    </div>
    <div class="sort-bar">
      <button :class="{ active: sort === 'created_at,desc' }" @click="sort = 'created_at,desc'; fetchProducts()">最新</button>
      <button :class="{ active: sort === 'price,asc' }" @click="sort = 'price,asc'; fetchProducts()">价格升序</button>
      <button :class="{ active: sort === 'price,desc' }" @click="sort = 'price,desc'; fetchProducts()">价格降序</button>
    </div>
    <p v-if="searching" class="search-info">搜索"{{ searchedKeyword }}"找到 {{ total }} 件商品</p>
    <LoadingState v-if="loading" />
    <ErrorState v-else-if="error" :message="error" @retry="fetchProducts" />
    <div v-else-if="products.length === 0" class="empty-section">
      <EmptyState :text="searching ? `未找到与'${searchedKeyword}'相关的商品` : '暂无在售商品，成为第一个卖家吧！'" :actionText="searching ? '' : '发布商品'" @action="$router.push('/publish')" />
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
const searchedKeyword = ref('')
const searching = ref(false)
const products = ref([])
const categories = ref([])
const total = ref(0)
const activeCatId = ref(0)
const sort = ref('created_at,desc')
const loading = ref(true)
const error = ref('')
const catIcons = { '数码': '📱', '服饰': '👗', '家居': '🛋', '图书': '📚', '其他': '📦', '运动户外': '⚽', '美妆': '💄' }

const fetchProducts = async () => {
  loading.value = true
  error.value = ''
  try {
    let res
    if (searching.value) {
      const params = { keyword: searchedKeyword.value, sort: sort.value }
      if (activeCatId.value !== 0) params.categoryId = activeCatId.value
      res = await productAPI.search(params)
    } else {
      const params = { sort: sort.value }
      if (activeCatId.value !== 0) params.categoryId = activeCatId.value
      res = await productAPI.getList(params)
    }
    products.value = res.data?.content || []
    total.value = res.data?.totalElements || 0
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

const selectCategory = (id) => {
  activeCatId.value = id
  fetchProducts()
}

const handleSearch = () => {
  if (keyword.value.trim()) {
    searching.value = true
    searchedKeyword.value = keyword.value.trim()
    activeCatId.value = 0
    fetchProducts()
  }
}

const clearSearch = () => {
  searching.value = false
  keyword.value = ''
  searchedKeyword.value = ''
  fetchProducts()
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
.search-bar button { padding: 10px 24px; background: #1890ff; color: #fff; border: none; border-radius: 4px; cursor: pointer; }
.btn-clear { background: #fff !important; color: #666 !important; border: 1px solid #d9d9d9 !important; }
.search-info { color: #666; font-size: 14px; margin-bottom: 16px; }
.category-tags { display: flex; gap: 12px; flex-wrap: wrap; margin-bottom: 16px; }
.cat-btn { display: flex; flex-direction: column; align-items: center; gap: 4px; padding: 12px 16px; background: #fff; border: 2px solid #f0f0f0; border-radius: 12px; cursor: pointer; min-width: 72px; transition: all 0.2s; }
.cat-btn:hover { border-color: #1890ff; transform: translateY(-2px); box-shadow: 0 4px 8px rgba(24,144,255,0.1); }
.cat-btn.active { border-color: #1890ff; background: #e6f7ff; }
.cat-icon { font-size: 24px; }
.cat-btn span:last-child { font-size: 13px; color: #666; }
.cat-btn.active span:last-child { color: #1890ff; font-weight: 600; }
.sort-bar { display: flex; gap: 8px; margin-bottom: 20px; }
.sort-bar button { padding: 4px 12px; border: 1px solid #e8e8e8; background: #fff; border-radius: 4px; font-size: 13px; cursor: pointer; }
.sort-bar button.active { background: #1890ff; color: #fff; border-color: #1890ff; }
h3 { font-size: 18px; margin-bottom: 16px; }
.product-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
@media (max-width: 900px) { .product-grid { grid-template-columns: repeat(2, 1fr); } }
</style>
