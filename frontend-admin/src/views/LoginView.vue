<template>
  <div class="login-wrap">
    <el-card class="login-card">
      <h2 class="title">校园二手交易平台 · 管理后台</h2>
      <el-form :model="form" label-position="top" @keyup.enter="onLogin">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="管理员账号" autofocus />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="密码" />
        </el-form-item>
        <el-button type="primary" class="login-btn" :loading="loading" @click="onLogin">
          登 录
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/admin'
import { saveSession, type AdminUser } from '@/stores/auth'

const router = useRouter()
const form = reactive({ username: '', password: '' })
const loading = ref(false)

async function onLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const { token, user } = await login(form)
    const full: AdminUser = {
      id: user.id,
      username: user.username,
      nickname: user.nickname,
      roles: (user as any).roles ?? [],
      permissions: (user as any).permissions ?? []
    }
    if (!full.roles.includes('ADMIN')) {
      ElMessage.error('该账号不是管理员')
      return
    }
    saveSession(token, full)
    ElMessage.success('登录成功')
    router.push('/users')
  } catch (e) {
    ElMessage.error((e as Error).message)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrap {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f2d3d 0%, #2b4a6f 100%);
}
.login-card { width: 380px; padding: 12px 8px; }
.title { text-align: center; margin-bottom: 24px; font-size: 18px; color: #303133; }
.login-btn { width: 100%; }
</style>
