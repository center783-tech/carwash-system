import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/store/auth'

const routes = [
  // ------- 會員端 -------
  { path: '/', name: 'Home', component: () => import('@/views/Home.vue') },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/member/Login.vue'),
    meta: { hideNavBar: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/member/Register.vue'),
    meta: { hideNavBar: true }
  },
  {
    path: '/services',
    name: 'ServiceList',
    component: () => import('@/views/service/ServiceList.vue')
  },
  {
    path: '/reservations/create',
    name: 'CreateReservation',
    component: () => import('@/views/reservation/CreateReservation.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/reservations',
    name: 'ReservationList',
    component: () => import('@/views/reservation/ReservationList.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/member/Profile.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/vehicles',
    name: 'VehicleList',
    component: () => import('@/views/member/VehicleList.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/coupons',
    name: 'CouponList',
    component: () => import('@/views/coupon/CouponList.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/reviews',
    name: 'ReviewList',
    component: () => import('@/views/review/ReviewList.vue'),
    meta: { requiresAuth: true }
  },

  // ------- 後台管理端（都有自己的導覽列，不顯示會員端 NavBar） -------
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('@/views/admin/AdminLogin.vue'),
    meta: { hideNavBar: true }
  },
  {
    path: '/admin/dashboard',
    name: 'AdminDashboard',
    component: () => import('@/views/admin/Dashboard.vue'),
    meta: { requiresAuth: true, requiresAdmin: true, hideNavBar: true }
  },
  {
    path: '/admin/services',
    name: 'AdminServiceManage',
    component: () => import('@/views/admin/ServiceManage.vue'),
    meta: { requiresAuth: true, requiresAdmin: true, hideNavBar: true }
  },
  {
    path: '/admin/employees',
    name: 'AdminEmployeeManage',
    component: () => import('@/views/admin/EmployeeManage.vue'),
    meta: { requiresAuth: true, requiresAdmin: true, hideNavBar: true }
  },
  {
    path: '/admin/reservations',
    name: 'AdminReservationManage',
    component: () => import('@/views/admin/ReservationManage.vue'),
    meta: { requiresAuth: true, requiresAdmin: true, hideNavBar: true }
  },
  {
    path: '/admin/coupons',
    name: 'AdminCouponManage',
    component: () => import('@/views/admin/CouponManage.vue'),
    meta: { requiresAuth: true, requiresAdmin: true, hideNavBar: true }
  }
]

const router = createRouter({
  history: createWebHistory('/carwash/'),
  routes
})

router.beforeEach((to) => {
  const authStore = useAuthStore()

  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    return { name: to.meta.requiresAdmin ? 'AdminLogin' : 'Login', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresAdmin && !authStore.isAdmin) {
    return { name: 'AdminLogin' }
  }
  return true
})

export default router
