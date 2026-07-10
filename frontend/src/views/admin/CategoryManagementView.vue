<template>
  <div class="admin-page">
    <h2>分类管理</h2>
    <div class="add-form">
      <input v-model="newName" type="text" placeholder="分类名称" maxlength="50" />
      <button class="btn-primary" @click="handleAdd">新增分类</button>
    </div>
    <div class="table">
      <div class="table-header"><span>分类名称</span><span>状态</span><span>创建时间</span><span>操作</span></div>
      <div v-for="c in categories" :key="c.id" class="table-row">
        <span>{{ c.name }}</span>
        <span :class="c.enabled ? 'text-success' : 'text-muted'">{{ c.enabled ? '启用' : '停用' }}</span>
        <span>{{ c.createdAt?.slice(0, 10) }}</span>
        <span class="actions">
          <button @click="editCat(c)">编辑</button>
          <button v-if="c.enabled" class="btn-warn" @click="toggleStatus(c)">停用</button>
          <button v-else @click="toggleStatus(c)">启用</button>
        </span>
      </div>
    </div>
    <!-- 编辑弹窗 -->
    <div v-if="editingCat" class="modal-overlay" @click.self="editingCat = null">
      <div class="modal-card">
        <h4>编辑分类</h4>
        <div class="form-group"><label>分类名称</label><input v-model="editName" type="text" /></div>
        <div class="modal-actions">
          <button class="btn-cancel" @click="editingCat = null">取消</button>
          <button class="btn-primary" @click="handleEdit">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { categoryAPI } from '../../api/modules.js'

const categories = ref([])
const newName = ref('')
const editingCat = ref(null)
const editName = ref('')

const fetchAll = async () => {
  try { const res = await categoryAPI.getAll(); categories.value = res.data || [] } catch (e) {}
}
const handleAdd = async () => {
  if (!newName.value.trim()) return
  try { await categoryAPI.create({ name: newName.value.trim() }); newName.value = ''; fetchAll() } catch (e) { alert(e?.message || '添加失败') }
}
const editCat = (c) => { editingCat.value = c; editName.value = c.name }
const handleEdit = async () => {
  try { await categoryAPI.update(editingCat.value.id, { name: editName.value }); editingCat.value = null; fetchAll() } catch (e) { alert(e?.message || '编辑失败') }
}
const toggleStatus = async (c) => {
  try { await categoryAPI.toggleStatus(c.id); fetchAll() } catch (e) { alert(e?.message || '操作失败') }
}

onMounted(fetchAll)
</script>

<style scoped>
.admin-page h2 { margin-bottom: 16px; }
.add-form { display: flex; gap: 12px; margin-bottom: 16px; }
.add-form input { padding: 8px 12px; border: 1px solid var(--border); border-radius: 4px; font-size: 14px; width: 250px; }
.btn-primary { padding: 8px 16px; background: var(--primary); color: #fff; border: none; border-radius: 4px; }
.table { background: var(--card-bg); border-radius: 8px; }
.table-header, .table-row { display: grid; grid-template-columns: 1fr 80px 120px 160px; gap: 12px; align-items: center; padding: 12px 16px; font-size: 14px; }
.table-header { font-weight: 500; border-bottom: 1px solid var(--bg); color: var(--text-secondary); }
.table-row { border-bottom: 1px solid var(--bg); }
.text-success { color: var(--success); }
.text-muted { color: var(--text-muted); }
.actions { display: flex; gap: 8px; }
.actions button { padding: 4px 12px; border: 1px solid var(--border); background: var(--card-bg); border-radius: 4px; font-size: 12px; }
.btn-warn { color: var(--warning); border-color: var(--warning) !important; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.45); z-index: 1000; display: flex; align-items: center; justify-content: center; }
.modal-card { background: var(--card-bg); border-radius: 8px; padding: 24px; width: 400px; }
.modal-card h4 { margin-bottom: 16px; }
.form-group { margin-bottom: 16px; }
.form-group label { display: block; margin-bottom: 6px; font-size: 14px; }
.form-group input { width: 100%; padding: 8px 12px; border: 1px solid var(--border); border-radius: 4px; font-size: 14px; }
.modal-actions { display: flex; justify-content: flex-end; gap: 12px; }
.btn-cancel { padding: 8px 20px; border: 1px solid var(--border); background: var(--card-bg); border-radius: 4px; }
@media (max-width: 600px) { .action-buttons,.actions { flex-direction: column; }
  .detail-layout,.admin-page { padding: 8px; }
  button { width: 100%; }
}
</style>
