<template>
  <div class="star-rating">
    <span
      v-for="i in 5"
      :key="i"
      class="star"
      :class="{ filled: i <= modelValue, readonly }"
      @click="selectScore(i)"
    >
      ★
    </span>
    <span v-if="showText" class="score-text">{{ modelValue }} 星</span>
  </div>
</template>

<script setup>
const props = defineProps({
  modelValue: { type: Number, default: 0 },
  showText: { type: Boolean, default: false },
  readonly: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue'])

const selectScore = (score) => {
  if (props.readonly) return
  emit('update:modelValue', score)
}
</script>

<style scoped>
.star-rating { display: inline-flex; align-items: center; gap: 4px; }
.star { font-size: 20px; color: var(--border); cursor: pointer; transition: color 0.2s; }
.star.readonly { cursor: default; }
.star.filled { color: #fadb14; }
.score-text { margin-left: 8px; font-size: 14px; color: var(--text-secondary); }
</style>
