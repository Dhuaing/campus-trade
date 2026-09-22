<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-select
        v-model="action"
        placeholder="全部动作"
        clearable
        style="width: 200px"
        @change="onFilter"
      >
        <el-option
          v-for="a in actions"
          :key="a.value"
          :label="a.label"
          :value="a.value"
        />
      </el-select>
    </div>

    <el-table :data="rows" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="adminUsername" label="操作人" width="120" />
      <el-table-column label="动作" width="150">
        <template #default="{ row }">
          <el-tag size="small">{{ actionText(row.action) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="目标" width="150">
        <template #default="{ row }">{{ row.targetType }} #{{ row.targetId ?? '-' }}</template>
      </el-table-column>
      <el-table-column prop="detail" label="详情" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">{{ row.detail || '-' }}</template>
      </el-table-column>
      <el-table-column prop="ip" label="IP" width="140">
        <template #default="{ row }">{{ row.ip || '-' }}</template>
      </el-table-column>
      <el-table-column prop="createdAt" label="时间" min-width="170" />
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
import { ElMessage } from 'element-plus'
import { getAuditLogs, type AuditLogRow } from '@/api/admin'

const actions = [
  { value: 'USER_BAN', label: '封禁用户' },
  { value: 'USER_UNBAN', label: '解封用户' },
  { value: 'PRODUCT_APPROVE', label: '商品过审' },
  { value: 'PRODUCT_REJECT', label: '商品驳回' },
  { value: 'PRODUCT_REMOVE', label: '强制下架' }
]

const action = ref('')
const rows = ref<AuditLogRow[]>([])
const loading = ref(false)
const page = ref(1)
const size = 20
const total = ref(0)

const ACTION_TEXT: Record<string, string> = {
  USER_BAN: '封禁用户',
  USER_UNBAN: '解封用户',
  PRODUCT_APPROVE: '商品过审',
  PRODUCT_REJECT: '商品驳回',
  PRODUCT_REMOVE: '强制下架'
}

function actionText(a: string) {
  return ACTION_TEXT[a] ?? a
}

async function load() {
  loading.value = true
  try {
    const result = await getAuditLogs(action.value, page.value - 1, size)
    rows.value = result.content
    total.value = result.totalElements
  } catch (e) {
    ElMessage.error((e as Error).message)
  } finally {
    loading.value = false
  }
}

function onFilter() {
  page.value = 1
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; gap: 12px; margin-bottom: 16px; }
.pager { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
