<template>
  <div class="empty-state">
    <p>{{ text }}</p>
    <button v-if="actionText" class="btn-action" @click="$emit('action')">{{ actionText }}</button>
  </div>
</template>

<script setup>
defineProps({
  text: { type: String, default: '暂无数据' },
  actionText: { type: String, default: '' }
})
defineEmits(['action'])
</script>

<style scoped>
.empty-state { text-align: center; padding: 60px 20px; color: var(--text-muted); }
.empty-state p { margin-bottom: 16px; font-size: 14px; }
.btn-action { padding: 8px 24px; background: var(--primary); color: #fff; border: none; border-radius: 4px; font-size: 14px; }
</style>
