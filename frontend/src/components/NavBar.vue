<template>
  <header class="nav">
    <router-link to="/" class="nav__brand">洗享快</router-link>

    <nav class="nav__links">
      <router-link to="/services">服務項目</router-link>
      <router-link v-if="authStore.isLoggedIn" to="/reservations">我的預約</router-link>
      <router-link v-if="authStore.isLoggedIn" to="/vehicles">我的車輛</router-link>
      <router-link v-if="authStore.isLoggedIn" to="/coupons">優惠券</router-link>
      <router-link to="/#about">關於我們</router-link>
    </nav>

    <div class="nav__auth">
      <template v-if="authStore.isLoggedIn">
        <router-link to="/profile" class="nav__user">{{ authStore.username }}</router-link>
        <el-button text @click="handleLogout">登出</el-button>
      </template>
      <template v-else>
        <router-link to="/login"><el-button text>登入</el-button></router-link>
        <router-link to="/register"><el-button type="primary" round>立即註冊</el-button></router-link>
      </template>
    </div>
  </header>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/store/auth'

const router = useRouter()
const authStore = useAuthStore()

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.nav {
  display: flex;
  align-items: center;
  gap: 32px;
  padding: 16px 40px;
  background: var(--paper);
  border-bottom: 1px solid var(--paper-dim);
  position: sticky;
  top: 0;
  z-index: 100;
}

.nav__brand {
  font-family: var(--font-display);
  font-size: 22px;
  font-weight: 700;
  color: var(--ink);
  text-decoration: none;
  letter-spacing: 0.02em;
}

.nav__links {
  display: flex;
  gap: 28px;
  flex: 1;
}

.nav__links a {
  color: var(--ink);
  text-decoration: none;
  font-size: 15px;
  opacity: 0.75;
  transition: opacity 0.15s;
}

.nav__links a:hover,
.nav__links a.router-link-active {
  opacity: 1;
  color: var(--rinse-deep);
}

.nav__auth {
  display: flex;
  align-items: center;
  gap: 12px;
}

.nav__user {
  color: var(--ink);
  text-decoration: none;
  font-weight: 500;
  margin-right: 4px;
}

@media (max-width: 760px) {
  .nav {
    padding: 14px 20px;
    gap: 16px;
    flex-wrap: wrap;
  }
  .nav__links {
    order: 3;
    width: 100%;
    gap: 18px;
    flex-wrap: wrap;
  }
}
</style>
