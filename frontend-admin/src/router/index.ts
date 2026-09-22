import { createRouter, createWebHashHistory } from 'vue-router'
import { getToken, isAdmin } from '@/stores/auth'

const router = createRouter({
  // hash 模式：由后端静态托管无需 SPA 回退配置
  history: createWebHashHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue')
    },
    {
      path: '/',
      component: () => import('@/views/LayoutView.vue'),
      children: [
        {
          path: '',
          redirect: '/users'
        },
        {
          path: 'users',
          name: 'users',
          component: () => import('@/views/UsersView.vue')
        },
        {
          path: 'products',
          name: 'products',
          component: () => import('@/views/ProductsView.vue')
        },
        {
          path: 'audit-logs',
          name: 'audit-logs',
          component: () => import('@/views/AuditLogsView.vue')
        }
      ]
    }
  ]
})

router.beforeEach((to, _from, next) => {
  if (to.name === 'login') {
    next()
    return
  }
  // 未登录跳登录；已登录但非管理员（token 失效/串号）强制重新登录
  if (!getToken() || !isAdmin()) {
    next({ name: 'login' })
    return
  }
  next()
})

export default router
