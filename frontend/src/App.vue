<template>
  <div class="app">
    <header class="topbar">
      <div class="container topbar-inner">
        <router-link to="/" class="logo">校园二手</router-link>
        <nav class="nav">
          <router-link to="/">首页</router-link>
          <router-link to="/publish">发布</router-link>
          <router-link to="/messages" class="msg-link">
            消息
            <span v-if="messageUnread > 0" class="badge">{{ messageUnread > 99 ? '99+' : messageUnread }}</span>
          </router-link>
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
import { onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { useWs } from '@/composables/useWs'

const { restore, messageUnread, isLoggedIn } = useAuth()
const { ensureConnected, disconnect, onMessage } = useWs()
const route = useRoute()

onMounted(() => {
  restore()
})

watch(isLoggedIn, v => {
  if (v) ensureConnected()
  else disconnect()
})

onMessage(data => {
  if (data.type !== 'message' || !data.message) return
  const m = data.message
  // 正在与对方聊天时由 Chat.vue 负责展示并标记已读，不计未读
  if (route.name === 'chat' && Number(route.params.userId) === m.fromUserId) return
  messageUnread.value++
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
.msg-link { position: relative; }
.badge {
  position: absolute;
  top: -8px;
  right: -14px;
  background: var(--color-price);
  color: #fff;
  font-size: 11px;
  min-width: 18px;
  height: 18px;
  border-radius: 9px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 5px;
  line-height: 1;
}
</style>
