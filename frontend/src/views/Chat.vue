<template>
  <div class="chat">
    <div class="header">
      <router-link to="/messages" class="back">← 返回</router-link>
      <span class="name">{{ otherName }}</span>
    </div>

    <div class="messages" ref="messagesEl">
      <div v-if="loading" class="tip">加载中...</div>
      <div v-else-if="error" class="tip error">{{ error }}</div>
      <div v-else-if="list.length === 0" class="tip">还没有消息，发一条开始聊天吧</div>
      <template v-else>
        <div
          v-for="m in list"
          :key="m.id"
          class="bubble-row"
          :class="m.fromUserId === myId ? 'mine' : 'theirs'"
        >
          <div class="bubble">{{ m.content }}</div>
        </div>
      </template>
    </div>

    <div class="composer">
      <textarea
        v-model="draft"
        rows="1"
        placeholder="输入消息..."
        @keydown.enter.exact.prevent="send"
      />
      <button class="btn primary" :disabled="!draft.trim() || sending" @click="send">
        {{ sending ? '发送中...' : '发送' }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { getConversation, sendMessage, markMessageRead, type ConversationMessage } from '@/api/request'
import { useAuth } from '@/composables/useAuth'
import { useWs } from '@/composables/useWs'

const route = useRoute()
const { user, refreshUnread } = useAuth()
const otherId = Number(route.params.userId)
const myId = ref<number>(user.value?.id ?? 0)
const otherName = ref('')
const list = ref<ConversationMessage[]>([])
const draft = ref('')
const loading = ref(true)
const error = ref('')
const sending = ref(false)
const messagesEl = ref<HTMLElement | null>(null)

onMounted(async () => {
  myId.value = user.value?.id ?? 0
  await load()
})

// 实时接收对方消息：追加气泡、滚动到底、标记已读
const offWs = onMessage(async data => {
  if (data.type !== 'message' || !data.message) return
  const m = data.message
  if (m.fromUserId !== otherId) return
  list.value.push({
    id: m.id,
    fromUserId: m.fromUserId,
    fromUserNickname: m.fromUserNickname,
    toUserId: m.toUserId,
    content: m.content,
    isRead: true,
    createdAt: m.createdAt
  })
  await scrollBottom()
  try {
    await markMessageRead(m.id)
  } catch { /* 已读失败不阻塞 */ }
  refreshUnread()
})

onUnmounted(() => offWs())

async function load() {
  loading.value = true
  error.value = ''
  try {
    const msgs = await getConversation(otherId)
    list.value = msgs
    if (msgs.length > 0) {
      const firstOther = msgs.find(m => m.fromUserId !== myId.value)
      otherName.value = firstOther?.fromUserNickname ?? ''
    }
    await scrollBottom()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

async function send() {
  const text = draft.value.trim()
  if (!text) return
  sending.value = true
  try {
    await sendMessage({ toUserId: otherId, content: text })
    draft.value = ''
    await load()
    await refreshUnread()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '发送失败'
  } finally {
    sending.value = false
  }
}

async function scrollBottom() {
  await nextTick()
  if (messagesEl.value) {
    messagesEl.value.scrollTop = messagesEl.value.scrollHeight
  }
}
</script>

<style scoped>
.chat {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 120px);
  background: var(--color-card);
  border-radius: var(--radius);
  overflow: hidden;
}
.header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid #f0f1f3;
}
.back { color: var(--color-text-sub); text-decoration: none; font-size: 14px; }
.name { font-weight: 600; font-size: 15px; }

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.tip { text-align: center; color: var(--color-text-sub); padding: 40px; }
.error { color: var(--color-price); }
.bubble-row { display: flex; }
.bubble-row.mine { justify-content: flex-end; }
.bubble-row.theirs { justify-content: flex-start; }
.bubble {
  max-width: 70%;
  padding: 10px 14px;
  border-radius: 16px;
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
}
.mine .bubble { background: var(--color-primary); color: #fff; border-bottom-right-radius: 4px; }
.theirs .bubble { background: #f0f1f3; color: var(--color-text); border-bottom-left-radius: 4px; }

.composer {
  display: flex;
  gap: 10px;
  padding: 12px 16px;
  border-top: 1px solid #f0f1f3;
  background: #fff;
}
.composer textarea {
  flex: 1;
  padding: 10px 12px;
  border: 1px solid #e3e5e8;
  border-radius: 8px;
  font-size: 14px;
  resize: none;
  max-height: 100px;
  font-family: inherit;
}
.btn {
  padding: 0 20px;
  border-radius: 8px;
  border: none;
  background: var(--color-primary);
  color: #fff;
  font-size: 14px;
  cursor: pointer;
}
.btn:disabled { opacity: 0.6; cursor: not-allowed; }
</style>
