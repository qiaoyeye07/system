<template>
  <div class="error-state">
    <p>{{ message }}</p>
    <button class="btn-retry" @click="$emit('retry')">重新加载</button>
  </div>
</template>

<script setup>
defineProps({
  message: { type: String, default: '加载失败，请检查网络连接后重试。' }
})
defineEmits(['retry'])
</script>

<style scoped>
.error-state { text-align: center; padding: 60px 20px; color: #999; }
.error-state p { margin-bottom: 16px; font-size: 14px; }
.btn-retry { padding: 8px 24px; background: #1890ff; color: #fff; border: none; border-radius: 4px; font-size: 14px; }
</style>
