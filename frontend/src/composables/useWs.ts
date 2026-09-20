// 全局单例 WebSocket：登录后连接 /ws，断线自动重连，消息分发给订阅者
import { ref } from 'vue'

interface WsPayload {
  type: string
  message?: {
    id: number
    fromUserId: number
    fromUserNickname: string
    toUserId: number
    content: string
    isRead: boolean
    createdAt: string
  }
  [key: string]: unknown
}

const socket = ref<WebSocket | null>(null)
let retryTimer: number | null = null
let started = false
const handlers = new Set<(data: WsPayload) => void>()

function wsUrl(): string | null {
  const token = localStorage.getItem('token')
  if (!token) return null
  const proto = location.protocol === 'https:' ? 'wss' : 'ws'
  const base = import.meta.env.VITE_API_BASE_URL ?? ''
  const host = base ? base.replace(/^https?:\/\//, '').replace(/\/+$/, '') : location.host
  return `${proto}://${host}/ws?token=${encodeURIComponent(token)}`
}

function connect() {
  const url = wsUrl()
  if (!url) return
  if (socket.value && (socket.value.readyState === WebSocket.OPEN || socket.value.readyState === WebSocket.CONNECTING)) {
    return
  }
  try {
    const s = new WebSocket(url)
    socket.value = s
    s.onmessage = ev => {
      try {
        const data = JSON.parse(ev.data) as WsPayload
        handlers.forEach(h => h(data))
      } catch {
        // 忽略无法解析的帧
      }
    }
    s.onclose = () => {
      if (socket.value === s) socket.value = null
      scheduleReconnect()
    }
    s.onerror = () => {
      try { s.close() } catch { /* noop */ }
    }
  } catch {
    scheduleReconnect()
  }
}

function scheduleReconnect() {
  if (retryTimer !== null) return
  retryTimer = window.setTimeout(() => {
    retryTimer = null
    if (localStorage.getItem('token')) connect()
  }, 3000)
}

export function useWs() {
  /** 登录后调用：确保连接建立 */
  function ensureConnected() {
    started = true
    if (localStorage.getItem('token')) connect()
  }

  /** 退出登录时调用：断开并停止重连 */
  function disconnect() {
    started = false
    if (retryTimer !== null) {
      clearTimeout(retryTimer)
      retryTimer = null
    }
    if (socket.value) {
      try { socket.value.close() } catch { /* noop */ }
      socket.value = null
    }
  }

  /** 订阅服务端推送，返回取消订阅函数 */
  function onMessage(fn: (data: WsPayload) => void): () => void {
    handlers.add(fn)
    return () => handlers.delete(fn)
  }

  return { ensureConnected, disconnect, onMessage }
}
