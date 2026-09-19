<template>
  <div class="auth-page">
    <div class="card">
      <h1 class="title">登录</h1>
      <form @submit.prevent="submit">
        <div class="field">
          <label>用户名</label>
          <input v-model.trim="form.username" placeholder="请输入用户名" />
        </div>
        <div class="field">
          <label>密码</label>
          <input v-model="form.password" type="password" placeholder="请输入密码" />
        </div>
        <div v-if="error" class="error-tip">{{ error }}</div>
        <button class="btn primary" type="submit" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>
      <p class="switch">
        还没有账号？<router-link to="/register">去注册</router-link>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'

const router = useRouter()
const { login } = useAuth()
const form = reactive({ username: '', password: '' })
const loading = ref(false)
const error = ref('')

async function submit() {
  error.value = ''
  if (!form.username || !form.password) {
    error.value = '请填写用户名和密码'
    return
  }
  loading.value = true
  try {
    await login(form.username, form.password)
    const redirect = (router.currentRoute.value.query.redirect as string) || '/'
    router.push(redirect)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page { display: flex; justify-content: center; padding: 40px 0; }
.card {
  width: 100%; max-width: 400px;
  background: var(--color-card);
  border-radius: var(--radius);
  padding: 28px;
}
.title { font-size: 20px; margin-bottom: 20px; text-align: center; }
.field { margin-bottom: 16px; }
.field label { display: block; font-size: 14px; color: var(--color-text-sub); margin-bottom: 6px; }
.field input {
  width: 100%; padding: 10px 12px;
  border: 1px solid #e3e5e8; border-radius: 8px;
  font-size: 14px; box-sizing: border-box;
}
.error-tip { color: var(--color-price); font-size: 13px; margin-bottom: 12px; }
.btn.primary {
  width: 100%; padding: 12px;
  border: none; border-radius: 999px;
  background: var(--color-primary); color: #fff;
  font-size: 15px; cursor: pointer;
}
.btn.primary:disabled { opacity: 0.6; }
.switch { text-align: center; margin-top: 16px; color: var(--color-text-sub); font-size: 13px; }
.switch a { color: var(--color-primary); text-decoration: none; }
</style>
