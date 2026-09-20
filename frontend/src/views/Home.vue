<template>
  <div class="home">
    <section class="banner">
      <h1>校园闲置，轻松流转</h1>
      <p>学号认证 · 校内面交 · 安全省心</p>
    </section>

    <div class="filters">
      <span
        v-for="c in categories"
        :key="c"
        class="chip"
        :class="{ active: activeCategory === c }"
        @click="activeCategory = c"
      >{{ c }}</span>
    </div>

    <div class="search-bar">
      <input
        v-model="keyword"
        class="search-input"
        type="search"
        placeholder="搜索商品名称或描述"
        @keyup.enter="search"
      />
      <button class="search-btn" @click="search">搜索</button>
    </div>

    <div v-if="loading" class="tip">加载中...</div>
    <div v-else-if="error" class="tip error">{{ error }}</div>
    <div v-else-if="filtered.length === 0" class="tip">暂无商品</div>

    <div v-else class="grid">
      <router-link
        v-for="item in filtered"
        :key="item.id"
        :to="`/product/${item.id}`"
        class="card"
      >
        <div class="cover">
          <img v-if="item.coverImage" :src="item.coverImage" :alt="item.title" />
          <div v-else class="cover-placeholder">📦</div>
        </div>
        <div class="info">
          <h3 class="title">{{ item.title }}</h3>
          <div class="meta">
            <span class="price">¥{{ item.price.toFixed(2) }}</span>
            <span v-if="item.category" class="tag">{{ item.category }}</span>
          </div>
        </div>
      </router-link>
    </div>

    <div ref="sentinel" class="sentinel">
      <span v-if="loadingMore">正在加载更多...</span>
      <span v-else-if="!loading && filtered.length > 0 && !hasMore" class="end">— 没有更多了 —</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { getProducts, type Product } from '@/api/request'

const PAGE_SIZE = 20

const categories = ['全部', '教材书籍', '数码电子', '生活用品', '运动户外']
const activeCategory = ref('全部')

const products = ref<Product[]>([])
const loading = ref(true)
const loadingMore = ref(false)
const error = ref('')
const keyword = ref('')
const page = ref(0)
const hasMore = ref(false)
const sentinel = ref<HTMLElement | null>(null)
let observer: IntersectionObserver | null = null

const filtered = computed(() =>
  activeCategory.value === '全部'
    ? products.value
    : products.value.filter(p => p.category === activeCategory.value)
)

const load = async (reset: boolean) => {
  if (reset) {
    page.value = 0
    loading.value = true
  } else {
    if (loadingMore.value || !hasMore.value) return
    loadingMore.value = true
  }
  error.value = ''
  try {
    const data = await getProducts(keyword.value.trim() || undefined, page.value, PAGE_SIZE)
    products.value = reset ? data.content : [...products.value, ...data.content]
    hasMore.value = data.hasMore
    page.value += 1
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

const search = () => load(true)

onMounted(() => {
  load(true)
  observer = new IntersectionObserver(entries => {
    if (entries.some(e => e.isIntersecting)) load(false)
  }, { rootMargin: '400px' })
  if (sentinel.value) observer.observe(sentinel.value)
})

onUnmounted(() => {
  observer?.disconnect()
  observer = null
})
</script>

<style scoped>
.banner {
  background: linear-gradient(135deg, #2e7d32, #4caf50);
  color: #fff;
  border-radius: var(--radius);
  padding: 28px;
  margin-bottom: 16px;
}
.banner h1 { font-size: 24px; margin-bottom: 8px; }
.banner p { opacity: 0.9; }

.filters { display: flex; gap: 8px; margin-bottom: 16px; flex-wrap: wrap; }
.search-bar { display: flex; gap: 8px; margin-bottom: 16px; }
.search-input {
  flex: 1;
  padding: 10px 14px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  font-size: 14px;
}
.search-btn {
  padding: 10px 20px;
  background: var(--primary);
  color: #fff;
  border: none;
  border-radius: var(--radius);
  cursor: pointer;
  font-size: 14px;
}
.search-btn:hover { opacity: 0.9; }
.chip {
  padding: 6px 14px;
  background: var(--color-card);
  border-radius: 999px;
  font-size: 13px;
  color: var(--color-text-sub);
  cursor: pointer;
}
.chip.active { background: var(--color-primary); color: #fff; }

.grid {
  display:grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 12px;
}
.card {
  background: var(--color-card);
  border-radius: var(--radius);
  overflow: hidden;
  text-decoration: none;
  color: inherit;
  transition: transform 0.15s;
}
.card:hover { transform: translateY(-2px); }
.cover { aspect-ratio: 1 / 1; background: #f0f1f3; }
.cover img { width: 100%; height: 100%; object-fit: cover; }
.cover-placeholder {
  display: flex; align-items: center; justify-content: center;
  height: 100%; font-size: 40px;
}
.info { padding: 10px; }
.title {
  font-size: 14px; font-weight: 500; line-height: 1.4;
  display: -webkit-box; -webkit-line-clamp: 2;
  -webkit-box-orient: vertical; overflow: hidden; min-height: 39px;
}
.meta {
  display: flex; align-items: center; justify-content: space-between; margin-top: 8px;
}
.price { color: var(--color-price); font-weight: 700; }
.tag { font-size: 11px; color: var(--color-text-sub); }
.tip { text-align: center; color: var(--color-text-sub); padding: 40px; }
.error { color: var(--color-price); }
.sentinel { text-align: center; color: var(--color-text-sub); padding: 20px 0; font-size: 13px; min-height: 24px; }
</style>
