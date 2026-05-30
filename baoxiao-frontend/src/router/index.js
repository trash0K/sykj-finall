import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/list'
  },
  {
    path: '/list',
    name: 'ReimList',
    component: () => import('../views/ReimList.vue')
  },
  {
    path: '/detail',
    name: 'ReimDetail',
    component: () => import('../views/ReimDetail.vue')
  },
  {
    path: '/detail/:id',
    name: 'ReimDetailEdit',
    component: () => import('../views/ReimDetail.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
