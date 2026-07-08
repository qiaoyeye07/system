<template>
  <div class="star-rating">
    <span v-for="i in 5" :key="i" class="star" :class="{ filled: i <= modelValue }" @click="$emit('update:modelValue', i)">
      ★
    </span>
    <span v-if="showText" class="score-text">{{ modelValue }} 星</span>
  </div>
</template>

<script setup>
defineProps({
  modelValue: { type: Number, default: 0 },
  showText: { type: Boolean, default: false }
})
defineEmits(['update:modelValue'])
</script>

<style scoped>
.star-rating { display: inline-flex; align-items: center; gap: 4px; }
.star { font-size: 20px; color: #d9d9d9; cursor: pointer; transition: color 0.2s; }
.star.filled { color: #fadb14; }
.score-text { margin-left: 8px; font-size: 14px; color: #666; }
</style>
