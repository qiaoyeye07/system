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
  background: #fff; border-radius: 8px; padding: 24px; width: 400px; box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}
.confirm-dialog h4 { margin-bottom: 12px; font-size: 16px; }
.confirm-dialog p { color: #666; margin-bottom: 24px; font-size: 14px; }
.confirm-actions { display: flex; justify-content: flex-end; gap: 12px; }
.btn-cancel, .btn-confirm {
  padding: 8px 20px; border-radius: 4px; border: 1px solid #d9d9d9; font-size: 14px;
  background: #fff; color: #333;
}
.btn-confirm { background: #1890ff; color: #fff; border-color: #1890ff; }
.btn-confirm:hover { background: #40a9ff; }
.btn-cancel:hover { border-color: #1890ff; color: #1890ff; }
</style>
