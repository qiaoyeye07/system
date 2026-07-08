<template>
  <div class="loading-state">
    <div class="spinner"></div>
    <p>加载中...</p>
  </div>
</template>

<style scoped>
.loading-state { text-align: center; padding: 60px 20px; color: #999; }
.spinner {
  width: 40px; height: 40px; margin: 0 auto 16px;
  border: 3px solid #f0f0f0; border-top-color: #1890ff; border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
</style>
