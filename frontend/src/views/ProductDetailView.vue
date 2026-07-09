<template>
  <div class="detail-page">
    <LoadingState v-if="loading" />
    <ErrorState v-else-if="error" :message="error" @retry="fetchProduct" />
    <template v-else-if="product">
      <button class="btn-back" @click="$router.back()">← 返回</button>
      <div class="detail-layout">
        <div class="detail-images">
          <img v-if="imageList.length" :src="imageList[currentImage]" :alt="product.title" />
          <div v-else class="no-image">暂无图片</div>
          <div v-if="imageList.length > 1" class="image-dots">
            <span v-for="(img, i) in imageList" :key="i" :class="{ active: i === currentImage }" @click="currentImage = i"></span>
          </div>
        </div>
        <div class="detail-info">
          <h2>{{ product.title }}</h2>
          <p class="price">¥{{ product.price?.toFixed(2) }}</p>
          <div class="info-grid">
            <span>成色：{{ conditionMap[product.condition] }}</span>
            <span>交易方式：{{ tradeTypeMap[product.tradeType] || product.tradeType }}</span>
            <span>交易模式：{{ tradeModeMap[product.tradeMode] || product.tradeMode }}</span>
            <span>所在地：{{ product.location }}</span>
            <span>分类：{{ product.categoryName }}</span>
            <span>发布时间：{{ product.createdAt }}</span>
          </div>
          <div v-if="product.tags" class="tags">
            <span v-for="tag in product.tags.split(',')" :key="tag" class="tag">{{ tag.trim() }}</span>
          </div>
          <div class="seller-info">
            <router-link :to="`/user/${product.sellerId}`">
              卖家：{{ product.sellerName }} ★ {{ (product.sellerRating || 0).toFixed(1) }}
            </router-link>
          </div>
          <div class="description">
            <h4>商品描述</h4>
            <p>{{ product.description }}</p>
          </div>
          <div class="actions">
            <button v-if="!isOwner && !isAdmin" @click="contactSeller">联系卖家</button>
            <button v-if="canPurchase && !isAdmin" class="btn-primary" @click="buyNow">立即购买</button>
            <button v-if="canSwap && !isAdmin" class="btn-swap" @click="startSwap">发起交换</button>
            <button v-if="!isOwner && !isAdmin" class="btn-report" @click="showReport = true">举报</button>
            <button v-if="isOwner && product.status === 'ACTIVE'" class="btn-danger" @click="confirmOffShelf">下架商品</button>
            <button v-if="isAdmin && !isOwner" class="btn-contact" @click="contactSeller">联系卖家</button>
            <button v-if="isAdmin && !isOwner && product.status === 'ACTIVE'" class="btn-danger" @click="showAdminOff = true">管理员下架</button>
          </div>
        </div>
      </div>
    </template>

    <!-- 举报弹窗 -->
    <div v-if="showReport" class="modal-overlay" @click.self="showReport = false">
      <div class="modal-card">
        <h3>提交举报</h3>
        <div class="form-group">
          <label>举报原因 *</label>
          <select v-model="reportForm.reason">
            <option value="">请选择</option>
            <option value="FAKE_DESC">虚假描述</option>
            <option value="PROHIBITED">违禁品</option>
            <option value="COPYRIGHT">侵权/盗图</option>
            <option value="FAKE_PRICE">虚假价格</option>
            <option value="OTHER">其他违规</option>
          </select>
        </div>
        <div class="form-group">
          <label>补充描述</label>
          <textarea v-model="reportForm.description" rows="3" placeholder="可选，补充说明"></textarea>
        </div>
        <div class="modal-actions">
          <button class="btn-cancel" @click="showReport = false">取消</button>
          <button class="btn-primary" @click="submitReport">提交举报</button>
        </div>
      </div>
    </div>

    <ConfirmDialog :visible="confirmVisible" title="下架商品" message="下架后商品不再公开展示，可重新上架。确认下架吗？"
      @confirm="handleOffShelf" @cancel="confirmVisible = false" />

    <!-- 管理员下架弹窗 -->
    <div v-if="showAdminOff" class="modal-overlay" @click.self="showAdminOff = false">
      <div class="modal-card">
        <h3>管理员下架</h3>
        <p class="modal-desc">下架商品：{{ product.title }}</p>
        <div class="form-group">
          <label>下架原因</label>
          <textarea v-model="adminOffReason" rows="3" placeholder="说明下架原因"></textarea>
        </div>
        <div class="modal-actions">
          <button class="btn-cancel" @click="showAdminOff = false">取消</button>
          <button class="btn-primary" @click="handleAdminOff">确认下架</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { productAPI, reportAPI, adminAPI } from '../api/modules.js'
import LoadingState from '../components/common/LoadingState.vue'
import ErrorState from '../components/common/ErrorState.vue'
import ConfirmDialog from '../components/common/ConfirmDialog.vue'

const props = defineProps({ id: [String, Number] })
const router = useRouter()
const product = ref(null)
const loading = ref(true)
const error = ref('')
const currentImage = ref(0)
const showReport = ref(false)
const confirmVisible = ref(false)
const showAdminOff = ref(false)
const adminOffReason = ref('')
const reportForm = ref({ reason: '', description: '' })

const conditionMap = { NEW: '全新', LIKE_NEW: '几乎全新', USED: '有使用痕迹' }
const tradeTypeMap = { PICKUP: '自提', EXPRESS: '快递', BOTH: '自提 / 快递' }
const tradeModeMap = { SELL: '仅出售', SWAP: '仅交换', BOTH: '出售 / 交换' }

const userStr = localStorage.getItem('user')
const currentUser = userStr ? JSON.parse(userStr) : null
const isOwner = computed(() => currentUser?.id === product.value?.sellerId)
const isAdmin = computed(() => currentUser?.role === 'ADMIN')
const canPurchase = computed(() => !isOwner.value && product.value?.status === 'ACTIVE' && product.value?.tradeMode !== 'SWAP')
const canSwap = computed(() => !isOwner.value && product.value?.status === 'ACTIVE' && (product.value?.tradeMode === 'SWAP' || product.value?.tradeMode === 'BOTH'))
const imageList = computed(() => {
  if (!product.value?.images) return []
  return product.value.images.split(',').filter(Boolean)
})

const fetchProduct = async () => {
  loading.value = true
  error.value = ''
  try {
    const res = await productAPI.getDetail(props.id)
    product.value = res.data
  } catch (e) {
    error.value = e?.message || '加载商品失败'
  } finally {
    loading.value = false
  }
}

const contactSeller = () => router.push({
  path: '/chat',
  query: {
    contactId: product.value?.sellerId,
    contactName: product.value?.sellerName,
    productId: props.id
  }
})
const buyNow = async () => {
  try {
    const { orderAPI } = await import('../api/modules.js')
    const res = await orderAPI.create({ productId: props.id })
    router.push(`/order/${res.data.id}`)
  } catch (e) { alert(e?.message || '下单失败') }
}
const startSwap = () => router.push(`/swap/propose/${props.id}`)
const confirmOffShelf = () => { confirmVisible.value = true }
const handleOffShelf = async () => {
  try {
    await productAPI.offShelf(props.id)
    confirmVisible.value = false
    fetchProduct()
  } catch (e) { alert(e?.message || '操作失败') }
}
const submitReport = async () => {
  if (!reportForm.value.reason) { alert('请选择举报原因'); return }
  try {
    await reportAPI.submit({ targetType: 'PRODUCT', targetId: Number(props.id), reason: reportForm.value.reason, description: reportForm.value.description })
    alert('举报已提交')
    showReport.value = false
  } catch (e) { alert(e?.message || '提交失败') }
}
const handleAdminOff = async () => {
  try {
    const reason = adminOffReason.value.trim() || undefined
    await adminAPI.offProduct(props.id, reason ? { reason } : {})
    alert('已下架该商品')
    showAdminOff.value = false
    fetchProduct()
  } catch (e) { alert(e?.message || '操作失败') }
}

onMounted(fetchProduct)
</script>

<style scoped>
.detail-page { padding: 0 0 40px; }
.btn-back { background: none; border: none; color: #1890ff; font-size: 14px; margin-bottom: 16px; padding: 0; }
.detail-layout { display: grid; grid-template-columns: 1fr 1fr; gap: 32px; }
.detail-images { background: #fff; border-radius: 8px; padding: 16px; }
.detail-images img, .no-image { width: 100%; height: 400px; object-fit: contain; background: #f5f5f5; border-radius: 4px; }
.no-image { display: flex; align-items: center; justify-content: center; color: #999; }
.image-dots { display: flex; gap: 8px; justify-content: center; margin-top: 12px; }
.image-dots span { width: 8px; height: 8px; border-radius: 50%; background: #d9d9d9; cursor: pointer; }
.image-dots span.active { background: #1890ff; }
.price { color: #ff4d4f; font-size: 28px; font-weight: bold; margin: 12px 0; }
.info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; font-size: 14px; color: #666; margin-bottom: 12px; }
.tags { display: flex; gap: 6px; margin-bottom: 12px; }
.tag { padding: 2px 8px; background: #f0f0f0; border-radius: 4px; font-size: 12px; color: #666; }
.seller-info { margin-bottom: 16px; font-size: 14px; }
.description { background: #fff; border-radius: 8px; padding: 16px; margin-bottom: 20px; }
.description h4 { margin-bottom: 8px; }
.description p { color: #666; font-size: 14px; white-space: pre-wrap; }
.actions { display: flex; gap: 12px; flex-wrap: wrap; }
.actions button { padding: 10px 24px; border-radius: 4px; font-size: 14px; border: 1px solid #d9d9d9; background: #fff; }
.btn-primary { background: #1890ff !important; color: #fff !important; border-color: #1890ff !important; }
.btn-swap { background: #52c41a !important; color: #fff !important; border-color: #52c41a !important; }
.btn-report { color: #999 !important; }
.btn-danger { color: #ff4d4f !important; border-color: #ff4d4f !important; }

.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.45); z-index: 1000; display: flex; align-items: center; justify-content: center; }
.modal-card { background: #fff; border-radius: 8px; padding: 24px; width: 480px; }
.modal-card h3 { margin-bottom: 16px; }
.modal-desc { color: #666; font-size: 14px; margin-bottom: 12px; }
.form-group { margin-bottom: 16px; }
.form-group label { display: block; margin-bottom: 6px; font-size: 14px; }
.form-group select, .form-group textarea { width: 100%; padding: 8px 12px; border: 1px solid #d9d9d9; border-radius: 4px; font-size: 14px; }
.modal-actions { display: flex; justify-content: flex-end; gap: 12px; }
.btn-cancel { padding: 8px 20px; border: 1px solid #d9d9d9; background: #fff; border-radius: 4px; }
.btn-primary { padding: 8px 20px; background: #1890ff; color: #fff; border: none; border-radius: 4px; }
@media (max-width: 768px) { .detail-layout { grid-template-columns: 1fr; } }
</style>
