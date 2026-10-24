<template>
  <div>
  <AdminNav />
  <div class="page-container">
    <div class="header-row">
      <h2>預約管理</h2>
      <el-date-picker
          v-model="dateRange"
          type="daterange"
          unlink-panels
          range-separator="至"
          start-placeholder="開始日期"
          end-placeholder="結束日期"
          value-format="YYYY-MM-DD"
          @change="fetchData"
      />
    </div>

    <el-table :data="reservations" v-loading="loading">
      <el-table-column prop="reservationDate" label="日期" width="110" />
      <el-table-column prop="memberName" label="會員" />
      <el-table-column prop="plateNumber" label="車牌" />
      <el-table-column prop="zoneName" label="車位區" />
      <el-table-column prop="serviceName" label="服務項目" />
      <el-table-column prop="employeeName" label="指派員工" />
      <el-table-column prop="timeSlotStart" label="時段" />
      <el-table-column prop="totalAmount" label="金額" />
      <el-table-column label="狀態">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button v-if="row.status === 'PENDING'" link type="primary" @click="handleConfirm(row.id)">
            確認
          </el-button>
          <el-button v-if="row.status === 'CONFIRMED'" link type="success" @click="handleComplete(row.id)">
            標記完成
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && reservations.length === 0" description="這個期間沒有預約紀錄" />
  </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import AdminNav from '@/components/AdminNav.vue'
import { listReservationsByDateRange, confirmReservation, completeReservation } from '@/api/reservation'

const reservations = ref([])
const loading = ref(false)

const statusMap = {
  PENDING: { text: '待確認', type: 'info' },
  CONFIRMED: { text: '已確認', type: 'warning' },
  IN_PROGRESS: { text: '服務中', type: 'warning' },
  COMPLETED: { text: '已完成', type: 'success' },
  CANCELLED: { text: '已取消', type: 'danger' }
}
const statusText = (s) => statusMap[s]?.text || s
const statusTagType = (s) => statusMap[s]?.type || 'info'

function todayString() {
  return new Date().toISOString().slice(0, 10)
}

// 預設查「今天」這一天，使用者可以再自己調整成任意範圍
const dateRange = ref([todayString(), todayString()])

async function fetchData() {
  if (!dateRange.value || dateRange.value.length !== 2) return
  const [startDate, endDate] = dateRange.value
  loading.value = true
  try {
    reservations.value = await listReservationsByDateRange(startDate, endDate)
  } finally {
    loading.value = false
  }
}

async function handleConfirm(id) {
  await confirmReservation(id)
  ElMessage.success('已確認預約')
  fetchData()
}

async function handleComplete(id) {
  await completeReservation(id)
  ElMessage.success('已標記完成')
  fetchData()
}

onMounted(fetchData)
</script>

<style scoped>
.page-container {
  max-width: 1100px;
  margin: 24px auto;
  padding: 0 16px;
}
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
