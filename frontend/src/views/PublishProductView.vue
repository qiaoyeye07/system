<template>
  <div class="publish-page">
    <h2>发布商品</h2>
    <div v-if="msg" class="msg-tip" :class="msgType">{{ msg }}</div>
    <form @submit.prevent="handlePublish" class="publish-form">
      <div class="form-group">
        <label>商品分类 <span class="required">*</span></label>
        <select v-model="form.categoryId" required>
          <option value="">请选择分类</option>
          <option v-for="cat in categories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
        </select>
      </div>
      <div class="form-group">
        <label>标题 <span class="required">*</span></label>
        <input v-model="form.title" type="text" maxlength="100" placeholder="1-100个字符" required />
      </div>
      <div class="form-group">
        <label>价格 <span class="required">*</span></label>
        <input v-model.number="form.price" type="number" step="0.01" min="0.01" max="999999.99" placeholder="0.00" required />
      </div>
      <div class="form-group">
        <label>描述 <span class="required">*</span></label>
        <textarea v-model="form.description" rows="5" maxlength="4000" placeholder="详细描述商品情况" required></textarea>
      </div>
      <div class="form-group">
        <label>成色 <span class="required">*</span></label>
        <div class="radio-group">
          <label><input type="radio" v-model="form.condition" value="NEW" /> 全新</label>
          <label><input type="radio" v-model="form.condition" value="LIKE_NEW" /> 几乎全新</label>
          <label><input type="radio" v-model="form.condition" value="USED" /> 有使用痕迹</label>
        </div>
      </div>
      <div class="form-group">
        <label>交易方式 <span class="required">*</span></label>
        <div class="radio-group">
          <label><input type="radio" v-model="form.tradeType" value="PICKUP" /> 自提</label>
          <label><input type="radio" v-model="form.tradeType" value="EXPRESS" /> 快递</label>
          <label><input type="radio" v-model="form.tradeType" value="BOTH" /> 两者均可</label>
        </div>
      </div>
      <div class="form-group">
        <label>交易模式 <span class="required">*</span></label>
        <div class="radio-group">
          <label><input type="radio" v-model="form.tradeMode" value="SELL" /> 仅出售</label>
          <label><input type="radio" v-model="form.tradeMode" value="SWAP" /> 仅交换</label>
          <label><input type="radio" v-model="form.tradeMode" value="BOTH" /> 两者均可</label>
        </div>
      </div>
      <div class="form-group">
        <label>所在地 <span class="required">*</span></label>
        <input v-model="form.location" type="text" maxlength="50" placeholder="如：北京市朝阳区" required />
      </div>
      <div class="form-group">
        <label>商品图片（最多3张，每张≤5MB）</label>
        <input type="file" accept="image/jpeg,image/png,image/gif" multiple @change="handleFiles" />
        <div v-if="previews.length" class="previews">
          <img v-for="(url, i) in previews" :key="i" :src="url" class="preview-img" />
        </div>
      </div>
      <div class="form-group">
        <label>标签（可选，逗号分隔，最多5个）</label>
        <input v-model="form.tags" type="text" placeholder="如：苹果,手机" />
      </div>
      <div class="form-actions">
        <button type="button" class="btn-cancel" @click="$router.push('/')">取消</button>
        <button type="submit" class="btn-primary" :disabled="loading">发布商品</button>
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { productAPI, categoryAPI } from '../api/modules.js'

const router = useRouter()
const categories = ref([])
const loading = ref(false)
const msg = ref('')
const msgType = ref('success')
const previews = ref([])
const files = ref([])

const form = reactive({
  categoryId: '', title: '', price: '', description: '',
  condition: 'LIKE_NEW', tradeType: 'BOTH', tradeMode: 'SELL',
  location: '', tags: ''
})

const fetchCategories = async () => {
  try {
    const res = await categoryAPI.getEnabled()
    categories.value = res.data || []
  } catch (e) { /* ignore */ }
}

const handleFiles = (e) => {
  files.value = Array.from(e.target.files).slice(0, 3)
  previews.value = files.value.map(f => URL.createObjectURL(f))
}

const handlePublish = async () => {
  loading.value = true
  msg.value = ''
  try {
    const fd = new FormData()
    Object.entries(form).forEach(([k, v]) => { if (v) fd.append(k, v) })
    files.value.forEach(f => fd.append('images', f))
    const res = await productAPI.create(fd)
    msg.value = '商品发布成功，当前状态为在售'
    msgType.value = 'success'
    setTimeout(() => router.push(`/product/${res.data.id}`), 1000)
  } catch (e) {
    msg.value = e?.message || '发布失败'
    msgType.value = 'error'
  } finally {
    loading.value = false
  }
}

onMounted(fetchCategories)
</script>

<style scoped>
.publish-page { background: #fff; border-radius: 8px; padding: 24px; max-width: 700px; margin: 0 auto; }
h2 { margin-bottom: 20px; }
.msg-tip { padding: 8px 12px; border-radius: 4px; margin-bottom: 16px; font-size: 14px; }
.msg-tip.success { background: #f6ffed; color: #52c41a; border: 1px solid #b7eb8f; }
.msg-tip.error { background: #fff2f0; color: #ff4d4f; border: 1px solid #ffccc7; }
.form-group { margin-bottom: 20px; }
.form-group label { display: block; margin-bottom: 6px; font-size: 14px; color: #333; }
.required { color: #ff4d4f; }
.form-group input[type="text"], .form-group input[type="number"],
.form-group select, .form-group textarea {
  width: 100%; padding: 8px 12px; border: 1px solid #d9d9d9; border-radius: 4px; font-size: 14px;
}
.form-group textarea { resize: vertical; }
.radio-group { display: flex; gap: 20px; }
.radio-group label { display: flex; align-items: center; gap: 4px; font-size: 14px; }
.previews { display: flex; gap: 8px; margin-top: 8px; }
.preview-img { width: 100px; height: 100px; object-fit: cover; border-radius: 4px; }
.form-actions { display: flex; gap: 12px; }
.btn-primary { padding: 10px 32px; background: #1890ff; color: #fff; border: none; border-radius: 4px; font-size: 15px; }
.btn-primary:disabled { background: #91d5ff; }
.btn-cancel { padding: 10px 32px; border: 1px solid #d9d9d9; background: #fff; border-radius: 4px; font-size: 15px; }
</style>
