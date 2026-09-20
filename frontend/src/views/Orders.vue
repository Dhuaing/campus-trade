<template>
  <div class="orders">
    <div class="tabs">
      <button class="tab" :class="{ active: tab === 'bought' }" @click="tab = 'bought'">
        我买到的 <span v-if="bought.length" class="count">{{ bought.length }}</span>
      </button>
      <button class="tab" :class="{ active: tab === 'sold' }" @click="tab = 'sold'">
        我卖出的 <span v-if="sold.length" class="count">{{ sold.length }}</span>
      </button>
    </div>

    <div v-if="loading" class="empty">加载中...</div>
    <div v-else-if="current.length === 0" class="empty">
      {{ tab === 'bought' ? '还没有买入记录' : '还没有卖出记录' }}
    </div>

    <div v-else class="list">
      <div v-for="o in current" :key="o.id" class="order-row">
        <div class="thumb">
          <img v-if="o.product?.coverImage" :src="o.product.coverImage" :alt="o.product.title" />
          <div v-else class="placeholder">📦</div>
        </div>
        <div class="info">
          <div class="title">{{ o.product?.title }}</div>
          <div class="meta">
            <span class="price">¥{{ o.product?.price }}</span>
            <span class="counterpart">
              {{ tab === 'bought' ? '卖家' : '买家' }}：{{ counterpart(o).nickname }}
            </span>
          </div>
        </div>
        <span class="status" :class="'st-' + o.status">{{ statusText(o.status) }}</span>
        <div class="ops">
          <button
            v-if="tab === 'sold' && o.status === 'PENDING'"
            class="btn primary"
            @click="onComplete(o)"
          >确认完成</button>
          <button
            v-if="o.status === 'PENDING'"
            class="btn danger"
            @click="onCancel(o)"
          >取消订单</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getMyOrders, completeOrder, cancelOrder, type OrderItem } from '@/api/request'

const tab = ref<'bought' | 'sold'>('bought')
const bought = ref<OrderItem[]>([])
const sold = ref<OrderItem[]>([])
const loading = ref(true)

const current = computed(() => (tab.value === 'bought' ? bought.value : sold.value))

function counterpart(o: OrderItem) {
  return tab.value === 'bought' ? o.seller : o.buyer
}

function statusText(s: string) {
  return s === 'PENDING' ? '待确认' : s === 'COMPLETED' ? '已完成' : '已取消'
}

async function load() {
  loading.value = true
  try {
    const data = await getMyOrders()
    bought.value = data.bought
    sold.value = data.sold
  } catch {
    bought.value = []
    sold.value = []
  } finally {
    loading.value = false
  }
}

async function onComplete(o: OrderItem) {
  if (!confirm(`确认「${o.product?.title}」交易已完成？`)) return
  try {
    await completeOrder(o.id)
    o.status = 'COMPLETED'
  } catch (e) {
    alert((e as Error).message)
  }
}

async function onCancel(o: OrderItem) {
  if (!confirm(`确定取消「${o.product?.title}」的订单？商品将恢复在售。`)) return
  try {
    await cancelOrder(o.id)
    o.status = 'CANCELLED'
  } catch (e) {
    alert((e as Error).message)
  }
}

onMounted(load)
</script>

<style scoped>
.orders { max-width: 720px; margin: 0 auto; }
.tabs {
  display: flex; gap: 8px; margin-bottom: 16px;
  background: var(--color-card); border-radius: var(--radius); padding: 6px;
}
.tab {
  flex: 1; padding: 10px; border: none; border-radius: 999px;
  background: transparent; font-size: 15px; cursor: pointer; color: var(--color-text);
}
.tab.active { background: var(--color-primary); color: #fff; }
.count { font-size: 12px; opacity: 0.85; }
.empty { text-align: center; color: var(--color-text-sub); padding: 40px 0; }
.list { display: flex; flex-direction: column; gap: 12px; }
.order-row {
  display: flex; align-items: center; gap: 14px;
  background: var(--color-card); border-radius: var(--radius); padding: 12px;
}
.thumb {
  width: 72px; height: 72px; border-radius: 10px; overflow: hidden;
  background: #f0f1f3; flex-shrink: 0;
}
.thumb img { width: 100%; height: 100%; object-fit: cover; }
.placeholder {
  display: flex; align-items: center; justify-content: center;
  height: 100%; font-size: 32px;
}
.info { flex: 1; min-width: 0; }
.title {
  font-size: 15px; font-weight: 500;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.meta { display: flex; gap: 12px; margin-top: 6px; font-size: 13px; }
.price { color: var(--color-primary); font-weight: 600; }
.counterpart { color: var(--color-text-sub); }
.status { font-size: 12px; padding: 3px 10px; border-radius: 999px; flex-shrink: 0; }
.st-PENDING { background: #fff3e0; color: #e65100; }
.st-COMPLETED { background: #e8f5e9; color: #2e7d32; }
.st-CANCELLED { background: #f5f5f5; color: #9e9e9e; }
.ops { display: flex; gap: 8px; flex-shrink: 0; }
.btn { padding: 8px 14px; border-radius: 999px; font-size: 13px; border: none; cursor: pointer; }
.btn.primary { background: var(--color-primary); color: #fff; }
.btn.danger { background: #f0f1f3; color: var(--color-text); }

@media (max-width: 600px) {
  .order-row { flex-wrap: wrap; }
  .ops { width: 100%; justify-content: flex-end; }
}
</style>
