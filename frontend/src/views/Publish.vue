<template>
  <div class="publish">
    <h1 class="page-title">发布闲置</h1>

    <form class="form" @submit.prevent="submit">
      <div class="field">
        <label>标题 <span class="req">*</span></label>
        <input v-model.trim="form.title" maxlength="100" placeholder="例如：高等数学（第七版）同济大学" />
      </div>

      <div class="field">
        <label>分类</label>
        <select v-model="form.category">
          <option value="">未分类</option>
          <option v-for="c in categories" :key="c" :value="c">{{ c }}</option>
        </select>
      </div>

      <div class="row">
        <div class="field">
          <label>现价（元） <span class="req">*</span></label>
          <input v-model.trim="form.price" type="number" step="0.01" min="0.01" placeholder="0.00" />
        </div>
        <div class="field">
          <label>原价（元，选填）</label>
          <input v-model.trim="form.originalPrice" type="number" step="0.01" min="0.01" placeholder="0.00" />
        </div>
      </div>

      <div class="field">
        <label>描述</label>
        <textarea v-model.trim="form.description" rows="5" maxlength="2000" placeholder="成色、购买渠道、转让原因等"></textarea>
      </div>

      <div class="field">
        <label>封面图（选填，≤500KB）</label>
        <div class="upload-area">
          <div v-if="form.coverImage" class="preview">
            <img :src="form.coverImage" alt="预览" />
            <button type="button" class="remove-btn" @click="removeImage">×</button>
          </div>
          <label v-else class="upload-btn">
            <input type="file" accept="image/*" @change="onImageChange" hidden />
            <span>📷 点击选择图片</span>
          </label>
        </div>
      </div>

      <div v-if="error" class="error-tip">{{ error }}</div>

      <button class="btn primary" type="submit" :disabled="submitting">
        {{ submitting ? '发布中...' : '发布' }}
      </button>
    </form>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { createProduct } from '@/api/request'

const router = useRouter()
const categories = ['教材书籍', '数码电子', '生活用品', '运动户外']

const form = reactive({
  title: '',
  category: '',
  price: '',
  originalPrice: '',
  description: '',
  coverImage: ''
})
const submitting = ref(false)
const error = ref('')

const MAX_IMAGE_SIZE = 500 * 1024 // 500KB

function onImageChange(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    error.value = '请选择图片文件'
    return
  }
  if (file.size > MAX_IMAGE_SIZE) {
    error.value = `图片不能超过 ${MAX_IMAGE_SIZE / 1024}KB`
    return
  }
  error.value = ''
  const reader = new FileReader()
  reader.onload = () => {
    form.coverImage = reader.result as string
  }
  reader.readAsDataURL(file)
}

function removeImage() {
  form.coverImage = ''
}

async function submit() {
  error.value = ''
  if (!form.title) {
    error.value = '请填写标题'
    return
  }
  const price = Number(form.price)
  if (!form.price || Number.isNaN(price) || price <= 0) {
    error.value = '请填写正确的现价'
    return
  }
  let originalPrice: number | undefined
  if (form.originalPrice) {
    originalPrice = Number(form.originalPrice)
    if (Number.isNaN(originalPrice) || originalPrice <= 0) {
      error.value = '原价格式不正确'
      return
    }
  }

  submitting.value = true
  try {
    const created = await createProduct({
      title: form.title,
      price,
      originalPrice,
      category: form.category || undefined,
      description: form.description || undefined,
      coverImage: form.coverImage || undefined
    })
    // 发布后进入待审核状态，审核通过才会在首页展示
    router.push(`/product/${created.id}`)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '发布失败'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.page-title { font-size: 20px; margin-bottom: 16px; }
.form {
  background: var(--color-card);
  border-radius: var(--radius);
  padding: 20px;
  max-width: 640px;
}
.field { margin-bottom: 16px; }
.field label { display: block; font-size: 14px; color: var(--color-text-sub); margin-bottom: 6px; }
.req { color: var(--color-price); }
.field input,
.field select,
.field textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e3e5e8;
  border-radius: 8px;
  font-size: 14px;
  font-family: inherit;
  box-sizing: border-box;
  background: #fff;
}
.field textarea { resize: vertical; }
.row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.error-tip {
  color: var(--color-price);
  font-size: 13px;
  margin-bottom: 12px;
}
.btn.primary {
  width: 100%;
  padding: 12px;
  border: none;
  border-radius: 999px;
  background: var(--color-primary);
  color: #fff;
  font-size: 15px;
  cursor: pointer;
}
.btn.primary:disabled { opacity: 0.6; }

.upload-area { margin-top: 6px; }
.upload-btn {
  display: flex; align-items: center; justify-content: center;
  width: 120px; height: 120px;
  border: 2px dashed #e3e5e8; border-radius: 8px;
  color: var(--color-text-sub); font-size: 13px; cursor: pointer;
}
.upload-btn:hover { border-color: var(--color-primary); color: var(--color-primary); }
.preview { position: relative; width: 120px; height: 120px; }
.preview img { width: 100%; height: 100%; object-fit: cover; border-radius: 8px; }
.remove-btn {
  position: absolute; top: -8px; right: -8px;
  width: 24px; height: 24px; border-radius: 50%;
  background: #ff5252; color: #fff; border: none;
  font-size: 16px; line-height: 1; cursor: pointer;
}
</style>
