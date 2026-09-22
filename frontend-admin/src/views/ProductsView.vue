<template>
  <el-card shadow="never">
    <el-tabs v-model="tab" @tab-change="onTabChange">
      <el-tab-pane label="待审核" name="PENDING_REVIEW" />
      <el-tab-pane label="在售" name="ON_SALE" />
      <el-tab-pane label="全部" name="" />
    </el-tabs>

    <el-table :data="rows" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
      <el-table-column label="价格" width="100">
        <template #default="{ row }">¥{{ row.price }}</template>
      </el-table-column>
      <el-table-column prop="category" label="分类" width="110">
        <template #default="{ row }">{{ row.category || '-' }}</template>
      </el-table-column>
      <el-table-column label="卖家" width="120">
        <template #default="{ row }">{{ row.creator?.nickname || row.creator?.username || '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="发布时间" min-width="170" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 'PENDING_REVIEW'">
            <el-button v-if="hasPerm('product:audit')" type="success" size="small" @click="onApprove(row)">
              通过
            </el-button>
            <el-button v-if="hasPerm('product:audit')" type="warning" size="small" @click="onReject(row)">
              驳回
            </el-button>
          </template>
          <el-button
            v-if="(row.status === 'ON_SALE' || row.status === 'PENDING_REVIEW') && hasPerm('product:remove')"
            type="danger"
            size="small"
            @click="onRemove(row)"
          >
            强制下架
          </el-button>
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
import {
  getProducts,
  approveProduct,
  rejectProduct,
  removeProduct,
  type AdminProductRow
} from '@/api/admin'
import { hasPerm } from '@/stores/auth'

const tab = ref('PENDING_REVIEW')
const rows = ref<AdminProductRow[]>([])
const loading = ref(false)
const page = ref(1)
const size = 20
const total = ref(0)

function statusText(s: string) {
  const map: Record<string, string> = {
    PENDING_REVIEW: '待审核',
    ON_SALE: '在售',
    SOLD: '已售',
    REJECTED: '已驳回',
    REMOVED: '已下架'
  }
  return map[s] ?? s
}

function statusType(s: string) {
  const map: Record<string, string> = {
    PENDING_REVIEW: 'warning',
    ON_SALE: 'success',
    SOLD: 'info',
    REJECTED: 'danger',
    REMOVED: 'info'
  }
  return map[s] ?? 'info'
}

async function load() {
  loading.value = true
  try {
    const result = await getProducts(tab.value, page.value - 1, size)
    rows.value = result.content
    total.value = result.totalElements
  } catch (e) {
    ElMessage.error((e as Error).message)
  } finally {
    loading.value = false
  }
}

function onTabChange() {
  page.value = 1
  load()
}

async function onApprove(row: AdminProductRow) {
  try {
    await ElMessageBox.confirm(`通过「${row.title}」的审核并上架？`, '审核确认', { type: 'info' })
  } catch {
    return
  }
  try {
    await approveProduct(row.id)
    ElMessage.success('已通过并上架')
    load()
  } catch (e) {
    ElMessage.error((e as Error).message)
  }
}

async function onReject(row: AdminProductRow) {
  let reason
  try {
    const { value } = await ElMessageBox.prompt('请输入驳回原因（将展示给卖家）', '驳回原因', {
      inputPlaceholder: '例如：图片模糊 / 疑似违禁品',
      confirmButtonText: '驳回',
      cancelButtonText: '取消',
      inputValidator: (v: string) => !!v?.trim() || '原因不能为空'
    })
    reason = value.trim()
  } catch {
    return
  }
  try {
    await rejectProduct(row.id, reason)
    ElMessage.success('已驳回')
    load()
  } catch (e) {
    ElMessage.error((e as Error).message)
  }
}

async function onRemove(row: AdminProductRow) {
  let reason
  try {
    const { value } = await ElMessageBox.prompt(
      `强制下架「${row.title}」的原因（将展示给卖家）`,
      '强制下架',
      {
        confirmButtonText: '下架',
        cancelButtonText: '取消',
        inputValidator: (v: string) => !!v?.trim() || '原因不能为空'
      }
    )
    reason = value.trim()
  } catch {
    return
  }
  try {
    await removeProduct(row.id, reason)
    ElMessage.success('已强制下架')
    load()
  } catch (e) {
    ElMessage.error((e as Error).message)
  }
}

onMounted(load)
</script>

<style scoped>
.pager { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
