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
        <div class="location-row">
          <select v-model="selectedProvince" @change="onProvinceChange" required>
            <option value="">请选择省份</option>
            <option v-for="p in regions" :key="p.name" :value="p.name">{{ p.name }}</option>
          </select>
          <select v-model="selectedCity" @change="onCityChange" :disabled="!selectedProvince" required>
            <option value="">请选择城市</option>
            <option v-for="c in availableCities" :key="c.name" :value="c.name">{{ c.name }}</option>
          </select>
          <select v-model="selectedDistrict" :disabled="!selectedCity" required>
            <option value="">请选择区县</option>
            <option v-for="d in availableDistricts" :key="d" :value="d">{{ d }}</option>
          </select>
        </div>
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
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { productAPI, categoryAPI } from '../api/modules.js'
import { regions } from '../data/regions.js'

const router = useRouter()
const categories = ref([])
const loading = ref(false)
const msg = ref('')
const msgType = ref('success')
const previews = ref([])
const files = ref([])

const selectedProvince = ref('')
const selectedCity = ref('')
const selectedDistrict = ref('')

const availableCities = computed(() => {
  if (!selectedProvince.value) return []
  const province = regions.find(p => p.name === selectedProvince.value)
  return province ? province.cities : []
})

const availableDistricts = computed(() => {
  if (!selectedCity.value) return []
  const city = availableCities.value.find(c => c.name === selectedCity.value)
  return city ? city.districts : []
})

const onProvinceChange = () => {
  selectedCity.value = ''
  selectedDistrict.value = ''
}
const onCityChange = () => {
  selectedDistrict.value = ''
}

// 自动同步到 form.location
watch([selectedProvince, selectedCity, selectedDistrict], () => {
  if (selectedProvince.value && selectedCity.value && selectedDistrict.value) {
    form.location = `${selectedProvince.value}${selectedCity.value}${selectedDistrict.value}`
  } else {
    form.location = ''
  }
})

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
.publish-page { background: var(--card-bg); border-radius: 12px; padding: 32px; max-width: 720px; margin: 0 auto; box-shadow: 0 1px 4px rgba(0,0,0,.04); }
h2 { margin-bottom: 28px; font-size: 22px; color: var(--text); }
.msg-tip { padding: 10px 14px; border-radius: 8px; margin-bottom: 20px; font-size: 14px; }
.msg-tip.success { background: rgba(122,154,126,0.1); color: var(--success); border: 1px solid rgba(122,154,126,0.3); }
.msg-tip.error { background: rgba(194,120,120,0.1); color: var(--danger); border: 1px solid rgba(194,120,120,0.2); }
.form-group { margin-bottom: 22px; }
.form-group label { display: block; margin-bottom: 8px; font-size: 14px; color: var(--text-secondary); font-weight: 500; }
.required { color: var(--danger); }
.form-group input[type="text"], .form-group input[type="number"],
.form-group select, .form-group textarea { width: 100%; padding: 10px 14px; border: 2px solid #eee; border-radius: 10px; font-size: 15px; transition: all .2s; }
.form-group input:focus, .form-group select:focus, .form-group textarea:focus { border-color: var(--primary, var(--primary)); outline: none; box-shadow: 0 0 0 3px rgba(22,119,255,.06); }
.form-group textarea { resize: vertical; }
.radio-group { display: flex; gap: 24px; }
.radio-group label { display: flex; align-items: center; gap: 6px; font-size: 14px; font-weight: 400; }
.location-row { display: flex; gap: 8px; }
.location-row select { flex: 1; padding: 10px 14px; border: 2px solid #eee; border-radius: 10px; font-size: 14px; }
.location-row select:disabled { background: var(--bg); color: var(--text-muted); }
.previews { display: flex; gap: 8px; margin-top: 8px; }
.preview-img { width: 100px; height: 100px; object-fit: cover; border-radius: 8px; }
.form-actions { display: flex; gap: 12px; margin-top: 8px; }
.btn-primary { padding: 12px 36px; background: var(--primary, var(--primary)); color: #fff; border: none; border-radius: 10px; font-size: 15px; font-weight: 600; }
.btn-primary:hover { background: var(--primary-hover); }
.btn-primary:disabled { background: #c5cfc0; cursor: not-allowed; }
.btn-cancel { padding: 12px 36px; border: 2px solid #eee; background: var(--card-bg); border-radius: 10px; font-size: 15px; color: var(--text-secondary); }
.btn-cancel:hover { border-color: #ccc; }
</style>
