<template>
  <div class="category-page">
    <h2>商品分类</h2>
    <div class="cat-nav">
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
    <EmptyState v-else-if="products.length === 0" text="该分类暂无商品，切换其他分类看看吧。" />
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
const products = ref([])
const categories = ref([])
const loading = ref(false)

const fetchCategories = async () => {
  try { const res = await categoryAPI.getEnabled(); categories.value = res.data || [] } catch (e) {}
}
const fetchProducts = async () => {
  loading.value = true
  try {
    const params = { sort: sort.value }
    if (activeId.value) params.categoryId = activeId.value
    const res = await productAPI.getList(params)
    products.value = res.data?.content || []
  } catch (e) { products.value = [] } finally { loading.value = false }
}
const selectCat = (id) => { activeId.value = id }
watch([activeId, sort], fetchProducts)
onMounted(() => { fetchCategories(); fetchProducts() })
</script>

<style scoped>
.category-page h2 { margin-bottom: 16px; }
.cat-nav { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 16px; }
.cat-nav button { padding: 6px 16px; border: 1px solid #e8e8e8; background: #fff; border-radius: 16px; font-size: 14px; }
.cat-nav button.active { background: #1890ff; color: #fff; border-color: #1890ff; }
.sort-bar { display: flex; gap: 8px; margin-bottom: 16px; }
.sort-bar button { padding: 4px 12px; border: 1px solid #e8e8e8; background: #fff; border-radius: 4px; font-size: 13px; }
.sort-bar button.active { background: #1890ff; color: #fff; border-color: #1890ff; }
.product-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
@media (max-width: 900px) { .product-grid { grid-template-columns: repeat(2, 1fr); } }
</style>
