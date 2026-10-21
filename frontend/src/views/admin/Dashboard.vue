<template>
  <div>
    <AdminNav />

    <div class="page-container">
      <h2>管理總覽</h2>
      <el-row :gutter="16">
        <el-col :span="6">
          <el-card>
            <template #header>今日預約</template>
            <div class="stat-number">{{ todayReservationCount ?? '--' }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card>
            <template #header>上架服務項目數</template>
            <div class="stat-number">{{ activeServiceCount ?? '--' }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card>
            <template #header>在職員工數</template>
            <div class="stat-number">{{ activeEmployeeCount ?? '--' }}</div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AdminNav from '@/components/AdminNav.vue'
import { listReservationsByDate } from '@/api/reservation'
import { listServices } from '@/api/service'
import { listEmployees } from '@/api/employee'

const todayReservationCount = ref(null)
const activeServiceCount = ref(null)
const activeEmployeeCount = ref(null)

function todayString() {
  return new Date().toISOString().slice(0, 10)
}

async function fetchStats() {
  // 三支各自失敗不互相影響，任何一支失敗該卡片維持顯示 '--'
  listReservationsByDate(todayString())
      .then((list) => { todayReservationCount.value = list.length })
      .catch(() => {})

  listServices()
      .then((list) => { activeServiceCount.value = list.length })
      .catch(() => {})

  listEmployees()
      .then((list) => {
        // status === 1 代表在職；listEmployees() 回傳的是全部員工，離職的也在裡面
        activeEmployeeCount.value = list.filter((e) => e.status === 1).length
      })
      .catch(() => {})
}

onMounted(fetchStats)
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
