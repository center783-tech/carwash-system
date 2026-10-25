<template>
  <header ref="navEl" class="nav">
    <router-link to="/" class="nav__brand">洗享快</router-link>

    <div class="nav__spacer" />

    <span v-if="authStore.isLoggedIn" class="nav__user">{{ authStore.username }}</span>
    <button type="button" class="nav__menu-btn" aria-label="開啟選單" @click="menuOpen = true">
      <span class="nav__menu-icon" />
      選單
    </button>

    <el-drawer v-model="menuOpen" direction="rtl" :size="drawerSize" :with-header="false" class="nav-drawer">
      <div class="menu">
        <div class="menu__top">
          <strong>選單</strong>
          <button type="button" class="menu__close" aria-label="關閉選單" @click="menuOpen = false">✕</button>
        </div>

        <nav class="menu__list" @click="menuOpen = false">
          <router-link to="/services">服務項目</router-link>
          <router-link v-if="authStore.isLoggedIn" to="/reservations">我的預約</router-link>
          <router-link v-if="authStore.isLoggedIn" to="/vehicles">我的車輛</router-link>
          <router-link v-if="authStore.isLoggedIn" to="/coupons">優惠券</router-link>
          <router-link to="/#about">關於我們</router-link>
          <router-link v-if="authStore.isLoggedIn" to="/profile">個人資料</router-link>
        </nav>

        <div class="menu__auth">
          <template v-if="authStore.isLoggedIn">
            <el-button size="large" class="menu__btn" @click="handleLogout">登出</el-button>
          </template>
          <template v-else>
            <el-button size="large" class="menu__btn" @click="go('/login')">登入</el-button>
            <el-button size="large" type="primary" class="menu__btn" @click="go('/register')">立即註冊</el-button>
          </template>
        </div>
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

// 手機上選單佔大部分寬度，桌機固定 320px
const drawerSize = computed(() => (window.innerWidth <= 760 ? '78%' : '320px'))

function go(path) {
  menuOpen.value = false
  router.push(path)
}

function handleLogout() {
  menuOpen.value = false
  authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.nav {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px var(--nav-right, 40px) 16px var(--nav-left, 40px);
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

.nav__spacer {
  flex: 1;
}

.nav__user {
  color: var(--ink);
  font-weight: 500;
  max-width: 40vw;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.nav__menu-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 44px;
  padding: 0 16px;
  font: inherit;
  font-size: 15px;
  color: var(--ink);
  background: #fff;
  border: 1px solid var(--paper-dim);
  border-radius: 999px;
  cursor: pointer;
}

/* 三條線的選單圖示 */
.nav__menu-icon,
.nav__menu-icon::before,
.nav__menu-icon::after {
  display: block;
  width: 16px;
  height: 2px;
  background: currentColor;
  border-radius: 2px;
}
.nav__menu-icon {
  position: relative;
}
.nav__menu-icon::before,
.nav__menu-icon::after {
  content: '';
  position: absolute;
  left: 0;
}
.nav__menu-icon::before {
  top: -5px;
}
.nav__menu-icon::after {
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

/* 每個選項至少 52px 高，手指好點 */
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

.menu__list a.router-link-exact-active {
  color: var(--rinse-deep);
  font-weight: 600;
}

.menu__auth {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-top: 16px;
}

.menu__btn {
  width: 100%;
  margin: 0 !important;
}

@media (max-width: 760px) {
  .nav {
    padding: 12px var(--nav-right, 16px) 12px var(--nav-left, 16px);
  }
}
</style>
