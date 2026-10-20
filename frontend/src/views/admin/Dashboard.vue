<template>
  <div>
    <AdminNav />

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
import AdminNav from '@/components/AdminNav.vue'
</script>

<style scoped>
.page-container {
  max-width: 1100px;
  margin: 24px auto;
  padding: 0 16px;
}
.stat-number {
  font-family: var(--font-display);
  font-size: 28px;
  font-weight: 700;
  color: var(--rinse-deep);
}
</style>
