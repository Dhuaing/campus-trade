<template>
  <div class="detail">
    <router-link to="/" class="back">← 返回列表</router-link>

    <div v-if="loading" class="tip">加载中...</div>
    <div v-else-if="error" class="tip error">{{ error }}</div>

    <div v-else-if="product" class="panel">
      <div class="gallery">
        <img v-if="product.coverImage" :src="product.coverImage" :alt="product.title" />
        <div v-else class="placeholder">📦</div>
      </div>
      <div class="content">
        <h1 class="title">{{ product.title }}</h1>
        <div class="price-box">
          <span class="price">¥{{ product.price.toFixed(2) }}</span>
          <span v-if="product.originalPrice" class="origin">
            原价 ¥{{ product.originalPrice.toFixed(2) }}
          </span>
        </div>
        <p class="desc">{{ product.description }}</p>
        <div class="seller">
          <div class="avatar">👤</div>
          <div>
            <div class="seller-name">校园卖家</div>
            <div class="seller-sub">已通过学号认证</div>
          </div>
        </div>
        <div class="actions">
          <button class="btn ghost">联系卖家</button>
          <button class="btn primary">立即下单</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getProduct, type Product } from '@/api/request'

const route = useRoute()
const product = ref<Product | null>(null)
const loading = ref(true)
const error = ref('')

onMounted(async () => {
  const id = Number(route.params.id)
  try {
    product.value = await getProduct(id)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.back { display: inline-block; margin-bottom: 12px; color: var(--color-text-sub); text-decoration: none; }
.panel {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  background: var(--color-card);
  border-radius: var(--radius);
  padding: 20px;
}
.gallery { background: #f0f1f3; border-radius: var(--radius); aspect-ratio: 1/1; }
.gallery img { width: 100%; height: 100%; object-fit: cover; border-radius: var(--radius); }
.placeholder {
  display: flex; align-items: center; justify-content: center;
  height: 100%; font-size: 80px;
}
.title { font-size: 20px; line-height: 1.4; }
.price-box { margin: 16px 0; display: flex; align-items: baseline; gap: 12px; }
.price { color: var(--color-price); font-size: 28px; font-weight: 700; }
.origin { color: var(--color-text-sub); text-decoration: line-through; font-size: 13px; }
.desc { color: var(--color-text); line-height: 1.7; margin-bottom: 20px; }
.seller { display: flex; gap: 12px; align-items: center; padding: 12px 0; border-top: 1px solid #f0f1f3; }
.avatar {
  width: 44px; height: 44px; border-radius: 50%;
  background: #e8f5e9; display: flex; align-items: center; justify-content: center; font-size: 22px;
}
.seller-name { font-weight: 500; }
.seller-sub { font-size: 12px; color: var(--color-text-sub); }
.actions { display: flex; gap: 12px; margin-top: 20px; }
.btn { flex: 1; padding: 12px; border-radius: 999px; font-size: 15px; border: none; }
.btn.primary { background: var(--color-primary); color: #fff; }
.btn.ghost { background: #f0f1f3; color: var(--color-text); }
.tip { text-align: center; color: var(--color-text-sub); padding: 40px; }
.error { color: var(--color-price); }

@media (max-width: 720px) {
  .panel { grid-template-columns: 1fr; }
}
</style>
