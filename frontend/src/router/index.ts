import { createRouter, createWebHistory } from 'vue-router'
import Home from '@/views/Home.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'home', component: Home },
    {
      path: '/product/:id',
      name: 'product-detail',
      component: () => import('@/views/ProductDetail.vue')
    },
    {
      path: '/publish',
      name: 'publish',
      component: () => import('@/views/Publish.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/messages',
      name: 'messages',
      component: () => import('@/views/Messages.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/chat/:userId',
      name: 'chat',
      component: () => import('@/views/Chat.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/my',
      name: 'my',
      component: () => import('@/views/My.vue')
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/Login.vue')
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/Register.vue')
    }
  ]
})

router.beforeEach((to, _from, next) => {
  if (to.meta.requiresAuth && !localStorage.getItem('token')) {
    next({ name: 'login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
})

export default router
