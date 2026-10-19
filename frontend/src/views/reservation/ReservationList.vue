<template>
  <div class="page-container">
    <h2>我的預約</h2>

    <el-table :data="reservations" v-loading="loading">
      <el-table-column prop="serviceName" label="服務項目" />
      <el-table-column prop="plateNumber" label="車牌" />
      <el-table-column prop="reservationDate" label="日期" />
      <el-table-column prop="timeSlotStart" label="時段" />
      <el-table-column prop="totalAmount" label="金額" />
      <el-table-column label="狀態">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button
              v-if="row.status === 'PENDING' || row.status === 'CONFIRMED'"
              type="danger" link
              @click="handleCancel(row.id)"
          >
            取消預約
          </el-button>
          <router-link v-if="row.status === 'COMPLETED'" :to="`/reviews?reservationId=${row.id}`">
            去評價
          </router-link>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
        style="margin-top: 16px"
        :current-page="page + 1"
        :page-size="size"
        :total="total"
        @current-change="(p) => { page = p - 1; fetchData() }"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listReservationsByMember, cancelReservation } from '@/api/reservation'
import { useAuthStore } from '@/store/auth'

const authStore = useAuthStore()
const reservations = ref([])
const loading = ref(false)
const page = ref(0)
const size = ref(10)
const total = ref(0)

const statusMap = {
  PENDING: { text: '待確認', type: 'info' },
  CONFIRMED: { text: '已確認', type: 'warning' },
  IN_PROGRESS: { text: '服務中', type: 'warning' },
  COMPLETED: { text: '已完成', type: 'success' },
  CANCELLED: { text: '已取消', type: 'danger' }
}
const statusText = (s) => statusMap[s]?.text || s
const statusTagType = (s) => statusMap[s]?.type || 'info'

async function fetchData() {
  loading.value = true
  try {
    const result = await listReservationsByMember(authStore.userId, page.value, size.value)
    reservations.value = result.content
    total.value = result.totalElements
  } finally {
    loading.value = false
  }
}

async function handleCancel(id) {
  await ElMessageBox.confirm('確定要取消這筆預約嗎？', '取消預約', { type: 'warning' })
  await cancelReservation(id)
  ElMessage.success('已取消預約')
  fetchData()
}

onMounted(fetchData)
</script>

<style scoped>
.page-container {
  max-width: 1000px;
  margin: 24px auto;
  padding: 0 16px;
}
</style>
