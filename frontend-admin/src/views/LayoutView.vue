<template>
  <el-container class="layout">
    <el-aside width="200px" class="aside">
      <div class="logo">🛡 后台管理</div>
      <el-menu
        :default-active="route.path"
        router
        background-color="#1f2d3d"
        text-color="#bfcbd9"
        active-text-color="#409eff"
      >
        <el-menu-item v-if="hasPerm('user:view')" index="/users">
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item v-if="hasPerm('product:audit')" index="/products">
          <span>商品审核</span>
        </el-menu-item>
        <el-menu-item v-if="hasPerm('auditlog:view')" index="/audit-logs">
          <span>审计日志</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="breadcrumb">校园二手交易平台 · 运营管理后台</div>
        <div class="user-box">
          <span class="username">{{ user?.nickname || user?.username }}</span>
          <el-button size="small" text type="danger" @click="onLogout">退出</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { getUser, hasPerm, clearSession } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const user = getUser()

function onLogout() {
  clearSession()
  router.push('/login')
}
</script>

<style scoped>
.layout { height: 100%; }
.aside { background: #1f2d3d; }
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
}
.aside :deep(.el-menu) { border-right: none; }
.header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
}
.breadcrumb { font-size: 14px; color: #606266; }
.user-box { display: flex; align-items: center; gap: 8px; }
.username { font-size: 14px; color: #303133; }
.main { padding: 16px; }
</style>
