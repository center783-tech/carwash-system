<template>
  <header class="admin-nav">
    <router-link to="/admin/dashboard" class="admin-nav__brand">洗享快．後台</router-link>

    <nav class="admin-nav__links">
      <router-link to="/admin/dashboard">總覽</router-link>
      <router-link to="/admin/reservations">預約管理</router-link>
      <router-link to="/admin/services">服務項目</router-link>
      <router-link to="/admin/employees">員工管理</router-link>
      <router-link to="/admin/zones">車位管理</router-link>
      <router-link to="/admin/coupons">優惠券</router-link>
    </nav>

    <el-button text class="admin-nav__logout" @click="handleLogout">登出</el-button>
  </header>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/store/auth'

const router = useRouter()
const authStore = useAuthStore()

function handleLogout() {
  authStore.logout()
  router.push('/admin/login')
}
</script>

<style scoped>
.admin-nav {
  display: flex;
  align-items: center;
  gap: 32px;
  padding: 16px 40px;
  background: var(--ink);
  position: sticky;
  top: 0;
  z-index: 100;
}

.admin-nav__brand {
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 700;
  color: var(--paper);
  text-decoration: none;
  white-space: nowrap;
}

.admin-nav__links {
  display: flex;
  gap: 24px;
  flex: 1;
}

.admin-nav__links a {
  color: #b7c4c9;
  text-decoration: none;
  font-size: 14px;
  transition: color 0.15s;
}

.admin-nav__links a:hover,
.admin-nav__links a.router-link-active {
  color: var(--paper);
}

.admin-nav__logout {
  color: #b7c4c9 !important;
}

@media (max-width: 760px) {
  .admin-nav {
    flex-wrap: wrap;
    padding: 14px 20px;
  }
  .admin-nav__links {
    order: 3;
    width: 100%;
    gap: 16px;
  }
}
</style>
