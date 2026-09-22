<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="搜索用户名/昵称"
        clearable
        style="width: 240px"
        @keyup.enter="onSearch"
        @clear="onSearch"
      />
      <el-button type="primary" @click="onSearch">搜索</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="用户名" width="140" />
      <el-table-column prop="nickname" label="昵称" width="140" />
      <el-table-column prop="studentId" label="学号" width="140">
        <template #default="{ row }">{{ row.studentId || '-' }}</template>
      </el-table-column>
      <el-table-column label="角色" width="140">
        <template #default="{ row }">
          <el-tag v-for="r in row.roles" :key="r" size="small" :type="r === 'ADMIN' ? 'danger' : 'info'">
            {{ r }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'BANNED' ? 'danger' : 'success'" size="small">
            {{ row.status === 'BANNED' ? '已封禁' : '正常' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="注册时间" min-width="170" />
      <el-table-column v-if="hasPerm('user:ban')" label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status !== 'BANNED'"
            type="danger"
            size="small"
            :disabled="row.id === user?.id"
            @click="onBan(row)"
          >
            封禁
          </el-button>
          <el-button v-else type="success" size="small" @click="onUnban(row)">解封</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="load"
      />
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUsers, banUser, unbanUser, type AdminUserRow } from '@/api/admin'
import { getUser, hasPerm } from '@/stores/auth'

const user = getUser()
const keyword = ref('')
const rows = ref<AdminUserRow[]>([])
const loading = ref(false)
const page = ref(1)
const size = 20
const total = ref(0)

async function load() {
  loading.value = true
  try {
    const result = await getUsers(keyword.value.trim(), page.value - 1, size)
    rows.value = result.content
    total.value = result.totalElements
  } catch (e) {
    ElMessage.error((e as Error).message)
  } finally {
    loading.value = false
  }
}

function onSearch() {
  page.value = 1
  load()
}

async function onBan(row: AdminUserRow) {
  try {
    await ElMessageBox.confirm(
      `确定封禁用户「${row.username}」？封禁后其无法登录、发言与下单，在线连接立即断开。`,
      '封禁确认',
      { type: 'warning', confirmButtonText: '封禁', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await banUser(row.id)
    ElMessage.success('已封禁')
    load()
  } catch (e) {
    ElMessage.error((e as Error).message)
  }
}

async function onUnban(row: AdminUserRow) {
  try {
    await unbanUser(row.id)
    ElMessage.success('已解封')
    load()
  } catch (e) {
    ElMessage.error((e as Error).message)
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; gap: 12px; margin-bottom: 16px; }
.pager { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
