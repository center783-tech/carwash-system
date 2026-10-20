<template>
  <div>
  <AdminNav />
  <div class="page-container">
    <div class="header-row">
      <h2>預約管理</h2>
      <el-date-picker v-model="selectedDate" type="date" placeholder="選擇日期" @change="fetchData" />
    </div>

    <el-table :data="reservations" v-loading="loading">
      <el-table-column prop="memberName" label="會員" />
      <el-table-column prop="plateNumber" label="車牌" />
      <el-table-column prop="serviceName" label="服務項目" />
      <el-table-column prop="employeeName" label="指派員工" />
      <el-table-column prop="timeSlotStart" label="時段" />
      <el-table-column prop="totalAmount" label="金額" />
      <el-table-column label="狀態">
        <template #default="{ row }"><el-tag>{{ row.status }}</el-tag></template>
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
  </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import AdminNav from '@/components/AdminNav.vue'
import { listReservationsByDate, confirmReservation, completeReservation } from '@/api/reservation'

const reservations = ref([])
const loading = ref(false)
const selectedDate = ref(new Date())

function formatDate(date) {
  return new Date(date).toISOString().slice(0, 10)
}

async function fetchData() {
  loading.value = true
  try {
    reservations.value = await listReservationsByDate(formatDate(selectedDate.value))
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
