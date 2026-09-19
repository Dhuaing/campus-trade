import { ref, computed } from 'vue'
import { getMe, login as apiLogin, register as apiRegister, type UserInfo } from '@/api/request'

const user = ref<UserInfo | null>(null)

export function useAuth() {
  const isLoggedIn = computed(() => user.value !== null)

  function setToken(token: string) {
    localStorage.setItem('token', token)
  }

  function clearToken() {
    localStorage.removeItem('token')
    user.value = null
  }

  async function login(username: string, password: string) {
    const res = await apiLogin({ username, password })
    setToken(res.token)
    user.value = res.user
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
    } catch {
      clearToken()
    }
  }

  return { user, isLoggedIn, login, register, logout, restore }
}
