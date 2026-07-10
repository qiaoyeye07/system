<template>
  <div class="category-page">
    <h2>商品分类</h2>
    <div class="search-bar">
      <input v-model="keyword" type="text" placeholder="在分类中搜索商品..." @keyup.enter="doSearch" />
      <button @click="doSearch">搜索</button>
      <button v-if="keyword" class="btn-clear" @click="clearSearch">清除</button>
    </div>
    <div class="cat-nav">
      <button :class="{ active: activeId === 0 }" @click="selectCat(0)">全部</button>
      <button v-for="cat in categories" :key="cat.id" :class="{ active: activeId == cat.id }" @click="selectCat(cat.id)">
        {{ cat.name }}
      </button>
    </div>
    <div class="sort-bar">
      <button :class="{ active: sort === 'created_at,desc' }" @click="sort = 'created_at,desc'">最新</button>
      <button :class="{ active: sort === 'price,asc' }" @click="sort = 'price,asc'">价格升序</button>
      <button :class="{ active: sort === 'price,desc' }" @click="sort = 'price,desc'">价格降序</button>
    </div>
    <LoadingState v-if="loading" />
    <EmptyState v-else-if="products.length === 0" :text="keyword ? `该分类下未找到与'${keyword}'相关的商品` : '该分类暂无商品，切换其他分类看看吧。'" />
    <div v-else class="product-grid">
      <ProductCard v-for="p in products" :key="p.id" :product="p" />
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { productAPI, categoryAPI } from '../api/modules.js'
import ProductCard from '../components/common/ProductCard.vue'
import LoadingState from '../components/common/LoadingState.vue'
import EmptyState from '../components/common/EmptyState.vue'

const props = defineProps({ id: [String, Number] })
const activeId = ref(Number(props.id) || 0)
const sort = ref('created_at,desc')
const keyword = ref('')
const products = ref([])
const categories = ref([])
const loading = ref(false)

const fetchCategories = async () => {
  try { const res = await categoryAPI.getEnabled(); categories.value = res.data || [] } catch (e) {}
}
const fetchProducts = async () => {
  loading.value = true
  try {
    let res
    if (keyword.value.trim()) {
      // 在分类下搜索：用搜索接口 + categoryId
      res = await productAPI.search({ keyword: keyword.value.trim(), categoryId: activeId.value || undefined, sort: sort.value })
    } else {
      res = await productAPI.getList({ categoryId: activeId.value || undefined, sort: sort.value })
    }
    products.value = res.data?.content || []
  } catch (e) { products.value = [] } finally { loading.value = false }
}
const selectCat = (id) => { activeId.value = id }
const doSearch = () => { fetchProducts() }
const clearSearch = () => { keyword.value = ''; fetchProducts() }
watch([activeId, sort], fetchProducts)
onMounted(() => { fetchCategories(); fetchProducts() })
</script>

<style scoped>
.category-page h2 { margin-bottom: 16px; }
.search-bar { display: flex; gap: 8px; margin-bottom: 16px; }
.search-bar input { flex: 1; padding: 8px 12px; border: 1px solid var(--border); border-radius: 4px; font-size: 14px; }
.search-bar input:focus { border-color: var(--primary); outline: none; }
.search-bar button { padding: 8px 16px; background: var(--primary); color: #fff; border: none; border-radius: 4px; cursor: pointer; }
.btn-clear { background: var(--card-bg) !important; color: var(--text-secondary) !important; border: 1px solid var(--border) !important; }
.cat-nav { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 16px; }
.cat-nav button { padding: 6px 16px; border: 1px solid var(--border); background: var(--card-bg); border-radius: 16px; font-size: 14px; cursor: pointer; }
.cat-nav button.active { background: var(--primary); color: #fff; border-color: var(--primary); }
.sort-bar { display: flex; gap: 8px; margin-bottom: 16px; }
.sort-bar button { padding: 4px 12px; border: 1px solid var(--border); background: var(--card-bg); border-radius: 4px; font-size: 13px; cursor: pointer; }
.sort-bar button.active { background: var(--primary); color: #fff; border-color: var(--primary); }
.product-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 16px; }
</style>
