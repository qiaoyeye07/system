<template>
  <nav class="app-nav">
    <template v-if="user?.role === 'ADMIN'">
      <router-link to="/">首页</router-link>
      <router-link to="/chat">聊天</router-link>
      <router-link to="/admin/users">用户管理</router-link>
      <router-link to="/admin/categories">分类管理</router-link>
      <router-link to="/admin/disputes">纠纷处理</router-link>
      <router-link to="/admin/reports">举报处理</router-link>
    </template>
    <template v-else>
      <router-link to="/">首页</router-link>
      <router-link to="/publish">发布商品</router-link>
      <router-link to="/my-products">我的商品</router-link>
      <router-link to="/my-orders">我的订单</router-link>
      <router-link to="/chat">聊天</router-link>
      <router-link :to="`/user/${user?.id}`">个人主页</router-link>
      <router-link to="/my-reports">我的举报</router-link>
    </template>
  </nav>
</template>

<script setup>
const userStr = localStorage.getItem('user')
const user = userStr ? JSON.parse(userStr) : null
</script>

<style scoped>
.app-nav {
  display: flex;
  gap: 0;
  padding: 0 24px;
  height: 44px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  align-items: center;
}
.app-nav a {
  padding: 0 16px;
  height: 44px;
  line-height: 44px;
  color: #333;
  text-decoration: none;
  font-size: 14px;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
}
.app-nav a:hover, .app-nav a.router-link-active {
  color: #1890ff;
  border-bottom-color: #1890ff;
}
</style>
