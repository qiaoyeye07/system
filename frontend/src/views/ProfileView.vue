<template>
  <div class="profile-page">
    <LoadingState v-if="loading" />
    <ErrorState v-else-if="error" :message="error" @retry="fetchProfile" />
    <template v-else-if="profile">
      <h2>{{ profile.username }} 的主页</h2>
      <div class="profile-card">
        <div class="info-row"><span>用户名：</span><strong>{{ profile.username }}</strong></div>
        <div class="info-row"><span>注册时间：</span>{{ profile.createdAt?.slice(0, 10) }}</div>
        <div class="info-row"><span>信誉：</span><strong>★ {{ profile.avgScore?.toFixed(1) || '暂无' }}（{{ profile.ratingCount || 0 }}条评价）</strong></div>
      </div>
      <div class="actions" v-if="!isSelf">
        <button class="btn-report" @click="showReport = true">举报用户</button>
      </div>
      <h3 style="margin-top:24px">在售商品（{{ profile.activeProductCount || 0 }}件）</h3>
      <div v-if="activeProducts.length" class="product-grid">
        <ProductCard v-for="p in activeProducts" :key="p.id" :product="p" />
      </div>
      <EmptyState v-else text="暂无在售商品" />

      <!-- 举报弹窗 -->
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
import { userAPI, reportAPI } from '../api/modules.js'
import ProductCard from '../components/common/ProductCard.vue'
import LoadingState from '../components/common/LoadingState.vue'
import ErrorState from '../components/common/ErrorState.vue'
import EmptyState from '../components/common/EmptyState.vue'

const props = defineProps({ id: [String, Number] })
const profile = ref(null)
const activeProducts = ref([])
const loading = ref(true)
const error = ref('')
const showReport = ref(false)
const reportForm = ref({ reason: '', description: '' })

const userStr = localStorage.getItem('user')
const currentUser = userStr ? JSON.parse(userStr) : null
const isSelf = computed(() => currentUser?.id == props.id)

const fetchProfile = async () => {
  loading.value = true
  error.value = ''
  try {
    const [profileRes, productsRes] = await Promise.all([
      userAPI.getProfile(props.id),
      userAPI.getProducts(props.id, { size: 50 })
    ])
    profile.value = profileRes.data
    activeProducts.value = productsRes.data?.content || []
  } catch (e) { error.value = e?.message || '加载失败' } finally { loading.value = false }
}

const submitReport = async () => {
  if (!reportForm.value.reason) { alert('请选择举报原因'); return }
  try {
    await reportAPI.submit({ targetType: 'USER', targetId: Number(props.id), reason: reportForm.value.reason, description: reportForm.value.description })
    alert('举报已提交'); showReport.value = false
  } catch (e) { alert(e?.message || '提交失败') }
}

onMounted(fetchProfile)
</script>

<style scoped>
.profile-page { max-width: 900px; margin: 0 auto; }
.profile-card { background: #fff; border-radius: 8px; padding: 20px; margin-bottom: 16px; }
.info-row { margin-bottom: 8px; font-size: 14px; color: #666; }
.info-row strong { color: #333; }
.actions { margin-bottom: 16px; }
.btn-report { padding: 6px 16px; border: 1px solid #ff4d4f; color: #ff4d4f; background: #fff; border-radius: 4px; }
.product-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-top: 12px; }
@media (max-width: 900px) { .product-grid { grid-template-columns: repeat(2, 1fr); } }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.45); z-index: 1000; display: flex; align-items: center; justify-content: center; }
.modal-card { background: #fff; border-radius: 8px; padding: 24px; width: 480px; }
.modal-card h4 { margin-bottom: 16px; }
.form-group { margin-bottom: 16px; }
.form-group label { display: block; margin-bottom: 6px; font-size: 14px; }
.form-group select, .form-group textarea { width: 100%; padding: 8px 12px; border: 1px solid #d9d9d9; border-radius: 4px; font-size: 14px; }
.modal-actions { display: flex; justify-content: flex-end; gap: 12px; }
.btn-cancel { padding: 8px 20px; border: 1px solid #d9d9d9; background: #fff; border-radius: 4px; }
.btn-primary { padding: 8px 20px; background: #1890ff; color: #fff; border: none; border-radius: 4px; }
</style>
