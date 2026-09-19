<template>
  <div class="auth-page">
    <div class="card">
      <h1 class="title">注册</h1>
      <form @submit.prevent="submit">
        <div class="field">
          <label>用户名 <span class="req">*</span></label>
          <input v-model.trim="form.username" maxlength="50" placeholder="登录用的用户名" />
        </div>
        <div class="field">
          <label>昵称</label>
          <input v-model.trim="form.nickname" maxlength="50" placeholder="显示给其他同学的名字" />
        </div>
        <div class="field">
          <label>学号</label>
          <input v-model.trim="form.studentId" maxlength="30" placeholder="选填，用于学号认证" />
        </div>
        <div class="field">
          <label>密码 <span class="req">*</span></label>
          <input v-model="form.password" type="password" maxlength="50" placeholder="至少 6 位" />
        </div>
        <div v-if="error" class="error-tip">{{ error }}</div>
        <button class="btn primary" type="submit" :disabled="loading">
          {{ loading ? '注册中...' : '注册' }}
        </button>
      </form>
      <p class="switch">
        已有账号？<router-link to="/login">去登录</router-link>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'

const router = useRouter()
const { register, login } = useAuth()
const form = reactive({ username: '', password: '', nickname: '', studentId: '' })
const loading = ref(false)
const error = ref('')

async function submit() {
  error.value = ''
  if (!form.username) { error.value = '请填写用户名'; return }
  if (!form.password || form.password.length < 6) { error.value = '密码至少 6 位'; return }
  loading.value = true
  try {
    await register({
      username: form.username,
      password: form.password,
      nickname: form.nickname || undefined,
      studentId: form.studentId || undefined
    })
    // 注册成功后自动登录
    await login(form.username, form.password)
    router.push('/')
  } catch (e) {
    error.value = e instanceof Error ? e.message : '注册失败'
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
.req { color: var(--color-price); }
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
