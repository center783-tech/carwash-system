<template>
  <header ref="navEl" class="admin-nav">
    <router-link to="/admin/dashboard" class="admin-nav__brand">洗享快．後台</router-link>

    <div class="admin-nav__spacer" />

    <button type="button" class="admin-nav__menu-btn" aria-label="開啟選單" @click="menuOpen = true">
      <span class="admin-nav__menu-icon" />
      選單
    </button>

    <el-drawer v-model="menuOpen" direction="rtl" :size="drawerSize" :with-header="false">
      <div class="menu">
        <div class="menu__top">
          <strong>後台選單</strong>
          <button type="button" class="menu__close" aria-label="關閉選單" @click="menuOpen = false">✕</button>
        </div>

        <nav class="menu__list" @click="menuOpen = false">
          <router-link to="/admin/reservations">預約管理</router-link>
          <router-link to="/admin/services">服務項目</router-link>
          <router-link to="/admin/employees">員工管理</router-link>
          <router-link to="/admin/zones">車位管理</router-link>
          <router-link to="/admin/closed-dates">休息日</router-link>
          <router-link to="/admin/coupons">優惠券</router-link>
        </nav>

        <el-button size="large" class="menu__btn" @click="handleLogout">登出</el-button>
      </div>
    </el-drawer>
  </header>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import { useAlignNavWithPage } from '@/composables/useAlignNavWithPage'

const router = useRouter()
const authStore = useAuthStore()
const menuOpen = ref(false)
const navEl = ref(null)
useAlignNavWithPage(navEl)

const drawerSize = computed(() => (window.innerWidth <= 760 ? '78%' : '320px'))

function handleLogout() {
  menuOpen.value = false
  authStore.logout()
  router.push('/admin/login')
}
</script>

<style scoped>
.admin-nav {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px var(--nav-right, 40px) 16px var(--nav-left, 40px);
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

.admin-nav__spacer {
  flex: 1;
}

.admin-nav__menu-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 44px;
  padding: 0 16px;
  font: inherit;
  font-size: 15px;
  color: var(--paper);
  background: transparent;
  border: 1px solid #b7c4c9;
  border-radius: 999px;
  cursor: pointer;
}

.admin-nav__menu-icon,
.admin-nav__menu-icon::before,
.admin-nav__menu-icon::after {
  display: block;
  width: 16px;
  height: 2px;
  background: currentColor;
  border-radius: 2px;
}
.admin-nav__menu-icon {
  position: relative;
}
.admin-nav__menu-icon::before,
.admin-nav__menu-icon::after {
  content: '';
  position: absolute;
  left: 0;
}
.admin-nav__menu-icon::before {
  top: -5px;
}
.admin-nav__menu-icon::after {
  top: 5px;
}

.menu {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.menu__top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 18px;
  margin-bottom: 8px;
}

.menu__close {
  width: 44px;
  height: 44px;
  font-size: 18px;
  color: var(--ink);
  background: none;
  border: none;
  cursor: pointer;
}

.menu__list {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.menu__list a {
  display: flex;
  align-items: center;
  min-height: 52px;
  padding: 0 8px;
  font-size: 17px;
  color: var(--ink);
  text-decoration: none;
  border-bottom: 1px solid var(--paper-dim);
}

.menu__list a.router-link-active {
  color: var(--rinse-deep);
  font-weight: 600;
}

.menu__btn {
  width: 100%;
  margin-top: 16px;
}

@media (max-width: 760px) {
  .admin-nav {
    padding: 12px var(--nav-right, 16px) 12px var(--nav-left, 16px);
  }
}
</style>
