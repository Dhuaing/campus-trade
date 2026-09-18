<template>
  <div class="app">
    <header class="topbar">
      <div class="container topbar-inner">
        <router-link to="/" class="logo">校园二手</router-link>
        <nav class="nav">
          <router-link to="/">首页</router-link>
          <a href="javascript:;">发布</a>
          <a href="javascript:;">消息</a>
          <a href="javascript:;">我的</a>
        </nav>
      </div>
    </header>
    <main class="container">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
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
