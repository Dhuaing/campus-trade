<template>
  <div class="app">
    <header class="topbar">
      <div class="container topbar-inner">
        <router-link to="/" class="logo">校园二手</router-link>
        <nav class="nav">
          <router-link to="/">首页</router-link>
          <router-link to="/publish">发布</router-link>
          <router-link to="/messages">消息</router-link>
          <router-link to="/my">我的</router-link>
        </nav>
      </div>
    </header>
    <main class="container">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useAuth } from '@/composables/useAuth'

const { restore } = useAuth()

onMounted(() => {
  restore()
})
</script>

<style scoped>
.topbar {
  background: var(--color-card);
  border-bottom: 1px solid #eceef1;
  position: sticky;
  top: 0;
  z-index: 10;
}
.topbar-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
}
.logo {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-primary);
  text-decoration: none;
}
.nav { display: flex; gap: 24px; }
.nav a {
  color: var(--color-text);
  text-decoration: none;
  font-size: 15px;
}
.nav a.router-link-active { color: var(--color-primary); }
</style>
