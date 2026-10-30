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
    path: '/forgot-username',
    name: 'ForgotUsername',
    component: () => import('@/views/member/ForgotAccount.vue'),
    meta: { hideNavBar: true }
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: () => import('@/views/member/ForgotAccount.vue'),
    meta: { hideNavBar: true }
  },
  {
    path: '/recover-username',
    name: 'RecoverUsername',
    component: () => import('@/views/member/RecoverUsername.vue'),
    meta: { hideNavBar: true }
  },
  {
    path: '/reset-password',
    name: 'ResetPassword',
    component: () => import('@/views/member/ResetPassword.vue'),
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
  },
  {
    path: '/admin/zones',
    name: 'AdminZoneManage',
    component: () => import('@/views/admin/ZoneManage.vue'),
    meta: { requiresAuth: true, requiresAdmin: true, hideNavBar: true }
  },
  {
    path: '/admin/reports',
    name: 'AdminReportManage',
    component: () => import('@/views/admin/ReportManage.vue'),
    meta: { requiresAuth: true, requiresAdmin: true, adminOnly: true, hideNavBar: true }
  },
  {
    path: '/admin/schedule',
    name: 'AdminMySchedule',
    component: () => import('@/views/admin/MySchedule.vue'),
    meta: { requiresAuth: true, requiresBackoffice: true, hideNavBar: true }
  },
  {
    path: '/admin/accounts',
    name: 'AdminAccountManage',
    component: () => import('@/views/admin/AccountManage.vue'),
    meta: { requiresAuth: true, requiresAdmin: true, adminOnly: true, hideNavBar: true }
  },
  {
    path: '/admin/closed-dates',
    name: 'AdminClosedDateManage',
    component: () => import('@/views/admin/ClosedDateManage.vue'),
    meta: { requiresAuth: true, requiresAdmin: true, hideNavBar: true }
  }
]

const router = createRouter({
  history: createWebHistory('/carwash/'),
  routes
})

router.beforeEach((to) => {
  const authStore = useAuthStore()

  const isBackRoute = to.meta.requiresAdmin || to.meta.requiresBackoffice
  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    return { name: isBackRoute ? 'AdminLogin' : 'Login', query: { redirect: to.fullPath } }
  }
  // 一般員工誤闖其他後台頁面，帶回總班表（後端也會擋）
  if (to.meta.requiresAdmin && authStore.role === 'EMPLOYEE') {
    return { name: 'AdminMySchedule' }
  }
  if (to.meta.requiresAdmin && !authStore.isAdmin) {
    return { name: 'AdminLogin' }
  }
  if (to.meta.requiresBackoffice && !authStore.isBackoffice) {
    return { name: 'AdminLogin' }
  }
  // 財務報表只有 ADMIN 能看，STAFF 回到總覽（後端也會擋，這裡只是讓畫面不要停在 403）
  if (to.meta.adminOnly && authStore.role !== 'ADMIN') {
    return { name: 'AdminDashboard' }
  }
  return true
})

export default router
