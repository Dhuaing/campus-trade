<template>
  <div class="messages">
    <h1 class="page-title">我的消息</h1>

    <div v-if="!isLoggedIn" class="tip">
      请先<router-link to="/login">登录</router-link>后查看消息
    </div>

    <template v-else>
      <div v-if="loading" class="tip">加载中...</div>
      <div v-else-if="error" class="tip error">{{ error }}</div>
      <div v-else-if="list.length === 0" class="tip">暂无消息</div>

      <div v-else class="list">
        <div v-for="m in list" :key="m.id" class="msg-item" @click="onRead(m)">
          <div class="avatar">👤</div>
          <div class="body">
            <div class="head">
              <span class="from">{{ m.fromUser.nickname }}</span>
              <span v-if="!m.isRead" class="dot">未读</span>
            </div>
            <div v-if="m.productTitle" class="product">关于：{{ m.productTitle }}</div>
            <div class="content">{{ m.content }}</div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMessages, markMessageRead, type Message } from '@/api/request'
import { useAuth } from '@/composables/useAuth'

const router = useRouter()
const { isLoggedIn, refreshUnread } = useAuth()
const list = ref<Message[]>([])
const loading = ref(true)
const error = ref('')

onMounted(async () => {
  if (!isLoggedIn.value) { loading.value = false; return }
  try {
    const res = await getMessages()
    list.value = res.messages
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
})

async function onRead(m: Message) {
  if (!m.isRead) {
    try {
      await markMessageRead(m.id)
      m.isRead = true
      await refreshUnread()
    } catch {
      // 忽略已读失败
    }
  }
  router.push({ name: 'chat', params: { userId: m.fromUser.id } })
}
</script>

<style scoped>
.page-title { font-size: 20px; margin-bottom: 16px; }
.list { display: flex; flex-direction: column; gap: 10px; }
.msg-item {
  display: flex; gap: 12px;
  background: var(--color-card);
  border-radius: var(--radius);
  padding: 14px;
  cursor: pointer;
  transition: background 0.15s;
}
.msg-item:hover { background: #f7f8f9; }
.avatar {
  width: 40px; height: 40px; border-radius: 50%;
  background: #e8f5e9; display: flex; align-items: center; justify-content: center;
  font-size: 20px; flex-shrink: 0;
}
.body { flex: 1; }
.head { display: flex; align-items: center; gap: 8px; }
.from { font-weight: 600; font-size: 14px; }
.dot {
  font-size: 11px; background: var(--color-primary); color: #fff;
  padding: 1px 8px; border-radius: 999px;
}
.product { font-size: 12px; color: var(--color-text-sub); margin: 4px 0; }
.content { font-size: 14px; line-height: 1.5; }
.tip { text-align: center; color: var(--color-text-sub); padding: 40px; }
.tip a { color: var(--color-primary); text-decoration: none; }
.error { color: var(--color-price); }
</style>
