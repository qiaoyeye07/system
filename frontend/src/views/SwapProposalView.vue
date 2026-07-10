<template>
  <div class="swap-propose-page">
    <h2>发起以物易物</h2>
    <div class="target-card">
      <h4>目标商品</h4>
      <p>{{ targetProduct?.title }} — ¥{{ targetProduct?.price?.toFixed(2) }}</p>
      <p>卖家：{{ targetProduct?.seller?.username }}</p>
    </div>
    <LoadingState v-if="loading" />
    <div v-else-if="myProducts.length === 0">
      <EmptyState text="您暂无在售商品可交换，请先发布商品。" actionText="发布商品" @action="$router.push('/publish')" />
    </div>
    <div v-else>
      <h4 style="margin-bottom:12px">选择我的交换商品</h4>
      <div class="product-list">
        <div v-for="p in myProducts" :key="p.id" class="product-option" :class="{ selected: selectedId === p.id }" @click="selectedId = p.id">
          <img v-if="p.firstImage" :src="p.firstImage" class="thumb" />
          <span>{{ p.title }}</span>
          <span class="price">¥{{ p.price?.toFixed(2) }}</span>
        </div>
      </div>
      <div class="form-group" style="margin-top:16px">
        <label>意向说明（可选）</label>
        <textarea v-model="note" rows="3" maxlength="500" placeholder="描述交换意向..."></textarea>
      </div>
      <div class="actions">
        <button class="btn-cancel" @click="$router.back()">取消</button>
        <button class="btn-primary" :disabled="!selectedId" @click="submitSwap">发起交换</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { productAPI, swapAPI } from '../api/modules.js'
import LoadingState from '../components/common/LoadingState.vue'
import EmptyState from '../components/common/EmptyState.vue'

const props = defineProps({ productId: [String, Number] })
const router = useRouter()
const targetProduct = ref(null)
const myProducts = ref([])
const selectedId = ref(null)
const note = ref('')
const loading = ref(true)

const fetchData = async () => {
  try {
    const [targetRes, myRes] = await Promise.all([
      productAPI.getDetail(props.productId),
      productAPI.getMyProducts({ status: 'ACTIVE', size: 100 })
    ])
    targetProduct.value = targetRes.data
    myProducts.value = (myRes.data?.content || []).filter(p => p.id != props.productId)
  } catch (e) {} finally { loading.value = false }
}

const submitSwap = async () => {
  try {
    const res = await swapAPI.propose({ productId: Number(props.productId), swapProductId: selectedId.value, note: note.value })
    router.push(`/swap/${res.data.id}`)
  } catch (e) { alert(e?.message || '发起失败') }
}

onMounted(fetchData)
</script>

<style scoped>
.swap-propose-page { max-width: 700px; margin: 0 auto; }
.target-card { background: var(--card-bg); border-radius: 8px; padding: 16px; margin-bottom: 20px; }
.target-card h4 { margin-bottom: 8px; }
.product-list { display: flex; flex-direction: column; gap: 8px; }
.product-option { display: flex; align-items: center; gap: 12px; padding: 12px; border: 2px solid var(--border); border-radius: 8px; cursor: pointer; }
.product-option.selected { border-color: var(--primary); background: rgba(139,157,131,0.1); }
.thumb { width: 48px; height: 48px; object-fit: cover; border-radius: 4px; }
.price { color: var(--danger); font-weight: bold; margin-left: auto; }
.form-group { margin-bottom: 16px; }
.form-group label { display: block; margin-bottom: 6px; font-size: 14px; }
.form-group textarea { width: 100%; padding: 8px 12px; border: 1px solid var(--border); border-radius: 4px; font-size: 14px; resize: vertical; }
.actions { display: flex; gap: 12px; }
.btn-primary { padding: 10px 32px; background: var(--primary); color: #fff; border: none; border-radius: 4px; }
.btn-primary:disabled { background: #c5cfc0; cursor: not-allowed; }
.btn-cancel { padding: 10px 32px; border: 1px solid var(--border); background: var(--card-bg); border-radius: 4px; }
</style>
