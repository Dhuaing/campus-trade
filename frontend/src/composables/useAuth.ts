import { ref, computed } from 'vue'
import { getMe, getMessages, login as apiLogin, register as apiRegister, type UserInfo } from '@/api/request'
import { useWs } from '@/composables/useWs'

const user = ref<UserInfo | null>(null)
const messageUnread = ref(0)
const { ensureConnected, disconnect } = useWs()

export function useAuth() {
  const isLoggedIn = computed(() => user.value !== null)

  function setToken(token: string) {
    localStorage.setItem('token', token)
  }

  function clearToken() {
    localStorage.removeItem('token')
    user.value = null
    messageUnread.value = 0
    disconnect()
  }

  async function login(username: string, password: string) {
    const res = await apiLogin({ username, password })
    setToken(res.token)
    user.value = res.user
    ensureConnected()
    await refreshUnread()
    return res.user
  }

  async function register(input: { username: string; password: string; nickname?: string; studentId?: string }) {
    return apiRegister(input)
  }

  function logout() {
    clearToken()
  }

  async function restore() {
    if (!localStorage.getItem('token')) {
      user.value = null
      return
    }
    try {
      user.value = await getMe()
      await refreshUnread()
    } catch {
      clearToken()
    }
  }

  async function refreshUnread() {
    if (!localStorage.getItem('token')) return
    try {
      const res = await getMessages()
      messageUnread.value = res.unread
    } catch {
      messageUnread.value = 0
    }
  }

  return { user, isLoggedIn, messageUnread, login, register, logout, restore, refreshUnread }
}
