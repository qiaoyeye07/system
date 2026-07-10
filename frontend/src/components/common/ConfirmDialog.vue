<template>
  <div v-if="visible" class="confirm-overlay" @click.self="$emit('cancel')">
    <div class="confirm-dialog">
      <h4>{{ title }}</h4>
      <p>{{ message }}</p>
      <div class="confirm-actions">
        <button class="btn-cancel" @click="$emit('cancel')">取消</button>
        <button class="btn-confirm" @click="$emit('confirm')">确认</button>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  visible: { type: Boolean, default: false },
  title: { type: String, default: '确认操作' },
  message: { type: String, default: '确定要执行此操作吗？' }
})
defineEmits(['confirm', 'cancel'])
</script>

<style scoped>
.confirm-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.45); z-index: 1000;
  display: flex; align-items: center; justify-content: center;
}
.confirm-dialog {
  background: var(--card-bg); border-radius: 8px; padding: 24px; width: 400px; box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}
.confirm-dialog h4 { margin-bottom: 12px; font-size: 16px; }
.confirm-dialog p { color: var(--text-secondary); margin-bottom: 24px; font-size: 14px; }
.confirm-actions { display: flex; justify-content: flex-end; gap: 12px; }
.btn-cancel, .btn-confirm {
  padding: 8px 20px; border-radius: 4px; border: 1px solid var(--border); font-size: 14px;
  background: var(--card-bg); color: var(--text);
}
.btn-confirm { background: var(--primary); color: #fff; border-color: var(--primary); }
.btn-confirm:hover { background: var(--primary-hover); }
.btn-cancel:hover { border-color: var(--primary); color: var(--primary); }
</style>
