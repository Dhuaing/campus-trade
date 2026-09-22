<template>
  <div class="my">
    <div class="card profile">
      <div class="avatar">👤</div>
      <div class="info">
        <div class="nickname">{{ user?.nickname || '未登录' }}</div>
        <div v-if="user" class="meta">@{{ user.username }} · 学号 {{ user.studentId || '未认证' }}</div>
      </div>
      <router-link v-if="!isLoggedIn" to="/login" class="btn primary">登录</router-link>
      <button v-else class="btn ghost" @click="onLogout">退出登录</button>
    </div>

    <div class="grid">
      <router-link to="/publish" class="action-card">
        <div class="icon">📢</div>
        <div class="label">发布闲置</div>
      </router-link>
      <router-link to="/messages" class="action-card">
        <div class="icon">💬</div>
        <div class="label">我的消息</div>
      </router-link>
      <router-link to="/orders" class="action-card">
        <div class="icon">🧾</div>
        <div class="label">我的订单</div>
      </router-link>
      <router-link to="/" class="action-card">
        <div class="icon">📦</div>
        <div class="label">浏览商品</div>
      </router-link>
    </div>

    <div v-if="isLoggedIn" class="mine">
      <h3 class="section-title">我的发布</h3>
      <div v-if="loading" class="empty">加载中...</div>
      <div v-else-if="myProducts.length === 0" class="empty">还没有发布商品</div>
      <div v-else class="product-list">
        <div v-for="p in myProducts" :key="p.id" class="product-row">
          <div class="p-info">
            <div class="p-title">{{ p.title }}</div>
            <div class="p-price">¥{{ p.price }}</div>
            <div v-if="p.status === 'REJECTED' && p.auditRemark" class="p-remark">驳回原因：{{ p.auditRemark }}</div>
          </div>
          <span class="p-status" :class="'st-' + p.status">{{ statusText(p.status) }}</span>
          <button v-if="p.status === 'ON_SALE'" class="btn warn" @click="onSold(p)">已售</button>
          <button v-if="p.status === 'ON_SALE'" class="btn danger" @click="onDelete(p)">下架</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { getMyProducts, deleteProduct, markProductSold, type Product } from '@/api/request'

const router = useRouter()
const { user, isLoggedIn, logout } = useAuth()

const myProducts = ref<Product[]>([])
const loading = ref(false)

function onLogout() {
  logout()
  router.push('/')
}

function statusText(s: string) {
  const map: Record<string, string> = {
    ON_SALE: '在售',
    SOLD: '已售',
    REMOVED: '已下架',
    PENDING_REVIEW: '待审核',
    REJECTED: '已驳回'
  }
  return map[s] ?? s
}

async function loadMine() {
  if (!isLoggedIn.value) return
  loading.value = true
  try {
    myProducts.value = await getMyProducts()
  } catch {
    myProducts.value = []
  } finally {
    loading.value = false
  }
}

async function onSold(p: Product) {
  if (!confirm(`确认「${p.title}」已售出？`)) return
  try {
    await markProductSold(p.id)
    p.status = 'SOLD'
  } catch (e) {
    alert((e as Error).message)
  }
}

async function onDelete(p: Product) {
  if (!confirm(`确定下架「${p.title}」？`)) return
  try {
    await deleteProduct(p.id)
    myProducts.value = myProducts.value.filter(x => x.id !== p.id)
  } catch (e) {
    alert((e as Error).message)
  }
}

onMounted(loadMine)
</script>

<style scoped>
.profile {
  display: flex; align-items: center; gap: 16px;
  background: var(--color-card);
  border-radius: var(--radius);
  padding: 20px;
  margin-bottom: 16px;
}
.avatar {
  width: 56px; height: 56px; border-radius: 50%;
  background: #e8f5e9; display: flex; align-items: center; justify-content: center;
  font-size: 28px;
}
.info { flex: 1; }
.nickname { font-size: 18px; font-weight: 600; }
.meta { font-size: 13px; color: var(--color-text-sub); margin-top: 4px; }
.btn { padding: 10px 18px; border-radius: 999px; font-size: 14px; border: none; text-decoration: none; cursor: pointer; }
.btn.primary { background: var(--color-primary); color: #fff; }
.btn.ghost { background: #f0f1f3; color: var(--color-text); }
.btn.danger { background: #ff5252; color: #fff; padding: 6px 14px; font-size: 13px; }
.btn.warn { background: #ffa726; color: #fff; padding: 6px 14px; font-size: 13px; }

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 12px;
  margin-bottom: 24px;
}
.action-card {
  background: var(--color-card);
  border-radius: var(--radius);
  padding: 24px 16px;
  text-align: center;
  text-decoration: none;
  color: inherit;
  transition: transform 0.15s;
}
.action-card:hover { transform: translateY(-2px); }
.icon { font-size: 32px; margin-bottom: 8px; }
.label { font-size: 14px; color: var(--color-text); }

.mine {
  background: var(--color-card);
  border-radius: var(--radius);
  padding: 20px;
}
.section-title { font-size: 16px; font-weight: 600; margin: 0 0 12px; }
.empty { color: var(--color-text-sub); font-size: 14px; padding: 12px 0; }
.product-list { display: flex; flex-direction: column; gap: 10px; }
.product-row {
  display: flex; align-items: center; gap: 12px;
  padding: 12px; border-radius: var(--radius);
  background: var(--color-bg);
}
.p-info { flex: 1; min-width: 0; }
.p-title { font-size: 14px; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.p-price { font-size: 13px; color: var(--color-primary); margin-top: 2px; }
.p-remark { font-size: 12px; color: #c62828; margin-top: 2px; }
.p-status { font-size: 12px; padding: 2px 8px; border-radius: 999px; }
.st-ON_SALE { background: #e8f5e9; color: #2e7d32; }
.st-SOLD { background: #fff3e0; color: #e65100; }
.st-REMOVED { background: #f5f5f5; color: #9e9e9e; }
.st-PENDING_REVIEW { background: #e3f2fd; color: #1565c0; }
.st-REJECTED { background: #ffebee; color: #c62828; }
</style>
