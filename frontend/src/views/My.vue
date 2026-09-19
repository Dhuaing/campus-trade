<template>
  <div class="my">
    <div class="card profile">
      <div class="avatar">👤</div>
      <div class="info">
        <div class="nickname">{{ user?.nickname || '未登录' }}</div>
        <div v-if="user" class="meta">@{{ user.username }} · 学号 {{ user.studentId || '未认证' }}</div>
      </div>
      <router-link v-if="!isLoggedIn" to="/login" class="btn primary">登录</router-link>
      <button v-else class="btn ghost" @click="onLogout">退出登录</button>
    </div>

    <div class="grid">
      <router-link to="/publish" class="action-card">
        <div class="icon">📢</div>
        <div class="label">发布闲置</div>
      </router-link>
      <router-link to="/messages" class="action-card">
        <div class="icon">💬</div>
        <div class="label">我的消息</div>
      </router-link>
      <router-link to="/" class="action-card">
        <div class="icon">📦</div>
        <div class="label">浏览商品</div>
      </router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'

const router = useRouter()
const { user, isLoggedIn, logout } = useAuth()

function onLogout() {
  logout()
  router.push('/')
}
</script>

<style scoped>
.profile {
  display: flex; align-items: center; gap: 16px;
  background: var(--color-card);
  border-radius: var(--radius);
  padding: 20px;
  margin-bottom: 16px;
}
.avatar {
  width: 56px; height: 56px; border-radius: 50%;
  background: #e8f5e9; display: flex; align-items: center; justify-content: center;
  font-size: 28px;
}
.info { flex: 1; }
.nickname { font-size: 18px; font-weight: 600; }
.meta { font-size: 13px; color: var(--color-text-sub); margin-top: 4px; }
.btn { padding: 10px 18px; border-radius: 999px; font-size: 14px; border: none; text-decoration: none; }
.btn.primary { background: var(--color-primary); color: #fff; }
.btn.ghost { background: #f0f1f3; color: var(--color-text); }

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 12px;
}
.action-card {
  background: var(--color-card);
  border-radius: var(--radius);
  padding: 24px 16px;
  text-align: center;
  text-decoration: none;
  color: inherit;
  transition: transform 0.15s;
}
.action-card:hover { transform: translateY(-2px); }
.icon { font-size: 32px; margin-bottom: 8px; }
.label { font-size: 14px; color: var(--color-text); }
</style>
