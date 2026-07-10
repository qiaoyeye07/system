<template>
  <div class="profile-page">
    <LoadingState v-if="loading" />
    <ErrorState v-else-if="error" :message="error" @retry="fetchProfile" />
    <template v-else-if="profile">
      <button class="btn-back" @click="$router.back()">← 返回</button>
      <h2>{{ profile.username }} 的主页</h2>

      <div class="profile-card">
        <div class="avatar">{{ profile.username?.charAt(0) }}</div>
        <div class="info-row"><strong>{{ profile.username }}</strong></div>
        <div class="info-row"><span>注册于 {{ profile.createdAt?.slice(0, 10) }}</span></div>
        <div class="info-row star-row">
          <span>★ {{ profile.avgScore?.toFixed(1) || '暂无' }}</span>
          <span class="count">（{{ profile.ratingCount || 0 }} 条评价）</span>
        </div>
      </div>

      <div class="rating-section">
        <h3>收到的评价（{{ ratings.length }}）</h3>
        <div v-if="ratings.length" class="rating-list">
          <div v-for="rating in ratings" :key="rating.id" class="rating-item" @click="goToOrder(rating.orderId)" style="cursor:pointer">
            <div class="rating-header">
              <strong>{{ rating.raterName || '匿名用户' }}</strong>
              <span>{{ formatDate(rating.createdAt) }}</span>
            </div>
            <StarRating :model-value="rating.score" :show-text="true" readonly />
            <p v-if="rating.comment" class="rating-comment">{{ rating.comment }}</p>
            <div class="rating-order">订单：{{ rating.orderNo || '-' }} → 查看详情</div>
          </div>
        </div>
        <EmptyState v-else text="暂无评价" />
      </div>

      <div class="actions" v-if="!isSelf">
        <button class="btn-report" @click="showReport = true">举报用户</button>
      </div>

      <h3 class="section-title">在售商品（{{ profile.activeProductCount || 0 }} 件）</h3>
      <div v-if="activeProducts.length" class="product-grid">
        <ProductCard v-for="p in activeProducts" :key="p.id" :product="p" />
      </div>
      <EmptyState v-else text="暂无在售商品" />

      <div v-if="showReport" class="modal-overlay" @click.self="showReport = false">
        <div class="modal-card">
          <h4>举报用户</h4>
          <div class="form-group">
            <label>举报原因 *</label>
            <select v-model="reportForm.reason">
              <option value="">请选择</option>
              <option value="FRAUD">欺诈行为</option>
              <option value="HARASS">骚扰行为</option>
              <option value="MALICIOUS">恶意交易</option>
              <option value="FAKE_ID">虚假身份</option>
              <option value="OTHER">其他违规</option>
            </select>
          </div>
          <div class="form-group">
            <label>补充描述</label>
            <textarea v-model="reportForm.description" rows="3"></textarea>
          </div>
          <div class="modal-actions">
            <button class="btn-cancel" @click="showReport = false">取消</button>
            <button class="btn-primary" @click="submitReport">提交举报</button>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { userAPI, reportAPI } from '../api/modules.js'
import { useUserStore } from '../store/user.js'
import ProductCard from '../components/common/ProductCard.vue'
import LoadingState from '../components/common/LoadingState.vue'
import ErrorState from '../components/common/ErrorState.vue'
import EmptyState from '../components/common/EmptyState.vue'
import StarRating from '../components/common/StarRating.vue'

const props = defineProps({ id: [String, Number] })
const router = useRouter()
const store = useUserStore()
const profile = ref(null)
const activeProducts = ref([])
const ratings = ref([])
const loading = ref(true)
const error = ref('')
const showReport = ref(false)
const reportForm = ref({ reason: '', description: '' })

const currentUser = store.getCurrentUser()
const isSelf = computed(() => currentUser?.id == props.id)

const fetchProfile = async () => {
  loading.value = true
  error.value = ''
  // Pre-fill from store if viewing own profile
  if (isSelf.value && currentUser) {
    profile.value = { ...currentUser, avgScore: '--', ratingCount: 0 }
  }
  try {
    const [profileRes, productsRes, ratingsRes] = await Promise.all([
      userAPI.getProfile(props.id),
      userAPI.getProducts(props.id, { size: 50 }),
      userAPI.getRatings(props.id)
    ])
    profile.value = profileRes.data
    activeProducts.value = productsRes.data?.content || []
    ratings.value = ratingsRes.data || []
  } catch (e) {
    error.value = e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

const formatDate = (value) => {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 16)
}

const goToOrder = (orderId) => {
  if (orderId) router.push(`/order/${orderId}`)
}

const submitReport = async () => {
  if (!reportForm.value.reason) {
    alert('请选择举报原因')
    return
  }
  try {
    await reportAPI.submit({
      targetType: 'USER',
      targetId: Number(props.id),
      reason: reportForm.value.reason,
      description: reportForm.value.description
    })
    alert('举报已提交')
    showReport.value = false
  } catch (e) {
    alert(e?.message || '提交失败')
  }
}

onMounted(fetchProfile)
</script>

<style scoped>
.profile-page { max-width: 900px; margin: 0 auto; }
.btn-back { background: none; border: none; color: var(--text-secondary); font-size: 14px; cursor: pointer; padding: 0 0 12px 0; }
.btn-back:hover { color: var(--text); }
.profile-card { background: var(--card-bg); border-radius: 12px; padding: 32px 24px; margin-bottom: 16px; text-align: center; }
.avatar { width: 64px; height: 64px; border-radius: 50%; background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; font-size: 28px; font-weight: 700; display: flex; align-items: center; justify-content: center; margin: 0 auto 12px; }
.info-row { margin-bottom: 6px; font-size: 14px; color: var(--text-secondary); }
.info-row strong { color: var(--text); font-size: 18px; }
.star-row { color: var(--warning); font-weight: 600; font-size: 16px; }
.star-row .count { color: var(--text-muted); font-weight: 400; font-size: 13px; }
.rating-section { background: var(--card-bg); border-radius: 8px; padding: 20px; margin-bottom: 16px; }
.rating-section h3 { margin-bottom: 12px; font-size: 18px; }
.rating-list { display: grid; gap: 12px; }
.rating-item { border: 1px solid var(--bg); border-radius: 8px; padding: 12px; }
.rating-header { display: flex; justify-content: space-between; gap: 12px; margin-bottom: 8px; font-size: 14px; }
.rating-header span { color: var(--text-muted); }
.rating-comment { margin: 8px 0 0; color: var(--text); line-height: 1.6; word-break: break-word; }
.rating-order { margin-top: 6px; font-size: 13px; color: var(--text-muted); }
.actions { margin-bottom: 16px; }
.section-title { margin-top: 24px; }
.btn-report { padding: 6px 16px; border: 1px solid var(--danger); color: var(--danger); background: var(--card-bg); border-radius: 4px; }
.product-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 16px; margin-top: 12px; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.45); z-index: 1000; display: flex; align-items: center; justify-content: center; }
.modal-card { background: var(--card-bg); border-radius: 8px; padding: 24px; width: 480px; max-width: calc(100vw - 32px); }
.modal-card h4 { margin-bottom: 16px; }
.form-group { margin-bottom: 16px; }
.form-group label { display: block; margin-bottom: 6px; font-size: 14px; }
.form-group select, .form-group textarea { width: 100%; padding: 8px 12px; border: 1px solid var(--border); border-radius: 4px; font-size: 14px; }
.modal-actions { display: flex; justify-content: flex-end; gap: 12px; }
.btn-cancel { padding: 8px 20px; border: 1px solid var(--border); background: var(--card-bg); border-radius: 4px; }
.btn-primary { padding: 8px 20px; background: var(--primary); color: #fff; border: none; border-radius: 4px; }
</style>
