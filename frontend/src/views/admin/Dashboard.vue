<template>
  <div class="admin-layout">
    <el-menu mode="horizontal" :default-active="route.path" router class="admin-menu">
      <el-menu-item index="/admin/dashboard">總覽</el-menu-item>
      <el-menu-item index="/admin/reservations">預約管理</el-menu-item>
      <el-menu-item index="/admin/services">服務項目</el-menu-item>
      <el-menu-item index="/admin/employees">員工管理</el-menu-item>
      <el-menu-item index="/admin/coupons">優惠券</el-menu-item>
      <div class="menu-spacer" />
      <el-menu-item @click="handleLogout">登出</el-menu-item>
    </el-menu>

    <div class="page-container">
      <h2>管理總覽</h2>
      <el-row :gutter="16">
        <el-col :span="6">
          <el-card>
            <template #header>今日預約</template>
            <!-- TODO: 接 GET /api/reservations/by-date?date=today 顯示統計數字 -->
            <div class="stat-number">--</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card>
            <template #header>上架服務項目數</template>
            <div class="stat-number">--</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card>
            <template #header>在職員工數</template>
            <div class="stat-number">--</div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/store/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

function handleLogout() {
  authStore.logout()
  router.push('/admin/login')
}
</script>

<style scoped>
.admin-menu {
  display: flex;
}
.menu-spacer {
  flex-grow: 1;
}
.page-container {
  max-width: 1100px;
  margin: 24px auto;
  padding: 0 16px;
}
.stat-number {
  font-size: 28px;
  font-weight: bold;
}
</style>
