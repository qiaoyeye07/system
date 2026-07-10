<template>
  <div class="search-page">
    <div class="search-bar">
      <input v-model="keyword" type="text" :placeholder="`搜索商品...`" @keyup.enter="doSearch" />
      <button @click="doSearch">搜索</button>
      <button class="btn-back" @click="$router.push('/')">返回首页</button>
    </div>
    <p v-if="searched" class="result-info">找到 {{ total }} 件商品</p>
    <div class="sort-bar">
      <button :class="{ active: sort === 'created_at,desc' }" @click="sort = 'created_at,desc'">最新</button>
      <button :class="{ active: sort === 'price,asc' }" @click="sort = 'price,asc'">价格升序</button>
      <button :class="{ active: sort === 'price,desc' }" @click="sort = 'price,desc'">价格降序</button>
    </div>
    <LoadingState v-if="loading" />
    <EmptyState v-else-if="searched && products.length === 0" :text="`未找到与'${searchedKeyword}'相关的商品，换个关键词试试吧。`" />
    <div v-else-if="products.length" class="product-grid">
      <ProductCard v-for="p in products" :key="p.id" :product="p" />
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { productAPI } from '../api/modules.js'
import ProductCard from '../components/common/ProductCard.vue'
import LoadingState from '../components/common/LoadingState.vue'
import EmptyState from '../components/common/EmptyState.vue'

const route = useRoute()
const keyword = ref(route.query.keyword || '')
const searchedKeyword = ref('')
const searched = ref(false)
const sort = ref('created_at,desc')
const products = ref([])
const total = ref(0)
const loading = ref(false)

const doSearch = async () => {
  if (!keyword.value.trim()) return
  searched.value = true
  searchedKeyword.value = keyword.value.trim()
  loading.value = true
  try {
    const res = await productAPI.search({ keyword: keyword.value.trim(), sort: sort.value })
    products.value = res.data?.content || []
    total.value = res.data?.totalElements || 0
  } catch (e) { products.value = []; total.value = 0 } finally { loading.value = false }
}
watch(sort, () => { if (searched.value) doSearch() })
onMounted(() => { if (keyword.value) doSearch() })
</script>

<style scoped>
.search-bar { display: flex; gap: 12px; margin-bottom: 16px; }
.search-bar input { flex: 1; padding: 10px 16px; border: 1px solid var(--border); border-radius: 4px; font-size: 14px; }
.search-bar button { padding: 10px 24px; background: var(--primary); color: #fff; border: none; border-radius: 4px; }
.btn-back { background: var(--card-bg) !important; color: var(--text) !important; border: 1px solid var(--border) !important; }
.result-info { margin-bottom: 12px; color: var(--text-secondary); font-size: 14px; }
.sort-bar { display: flex; gap: 8px; margin-bottom: 16px; }
.sort-bar button { padding: 4px 12px; border: 1px solid var(--border); background: var(--card-bg); border-radius: 4px; font-size: 13px; }
.sort-bar button.active { background: var(--primary); color: #fff; border-color: var(--primary); }
.product-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 16px; }
@media (max-width: 600px) { .action-buttons,.actions { flex-direction: column; }
  .detail-layout,.admin-page { padding: 8px; }
  button { width: 100%; }
}
</style>
