<template>
  <div class="page-container">
    <h2>我的預約</h2>

    <!-- 狀態分類：勾選要顯示的類別 -->
    <div class="filter">
      <el-checkbox-group v-model="selectedFilters" @change="onFilterChange">
        <el-checkbox-button v-for="f in filters" :key="f.key" :value="f.key">{{ f.label }}</el-checkbox-button>
      </el-checkbox-group>
    </div>

    <!-- 換頁放在清單上方，手機不用滑到最底才能翻頁 -->
    <div v-if="total > 0" class="pager">
      <el-button :disabled="page <= 0 || loading" @click="goPage(page - 1)">‹ 上一頁</el-button>
      <span class="pager__info">第 {{ page + 1 }} / {{ totalPages }} 頁・共 {{ total }} 筆</span>
      <el-button :disabled="page + 1 >= totalPages || loading" @click="goPage(page + 1)">下一頁 ›</el-button>
    </div>

    <el-table :data="reservations" v-loading="loading" empty-text="目前沒有預約紀錄，點下方按鈕建立第一筆吧">
      <template #empty>
        <div class="empty-hint">
          <p>{{ selectedFilters.length ? '這個分類目前沒有預約紀錄' : '請至少勾選一個分類' }}</p>
          <el-button type="primary" @click="router.push('/reservations/create')">立即預約</el-button>
        </div>
      </template>
      <!-- 日期、時段放最前面（手機卡片的第一、二行），並用 highlight-cell 加大加深 -->
      <el-table-column prop="reservationDate" label="日期" class-name="highlight-cell" />
      <el-table-column label="時段" class-name="highlight-cell">
        <template #default="{ row }">{{ row.timeSlotStart?.slice(0, 5) }}</template>
      </el-table-column>
      <el-table-column prop="serviceName" label="服務項目" />
      <el-table-column prop="plateNumber" label="車牌" />
      <el-table-column prop="zoneName" label="車位區" />
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
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listReservationsByMember, cancelReservation } from '@/api/reservation'
import { useAuthStore } from '@/store/auth'

const router = useRouter()
const authStore = useAuthStore()
const reservations = ref([])
const loading = ref(false)
const page = ref(0)
const size = ref(10)
const total = ref(0)

// 分類 → 對應的後端狀態；「服務中」併入「已確認」，避免有預約在分類裡找不到
const filters = [
  { key: 'PENDING', label: '待確認', statuses: ['PENDING'] },
  { key: 'CONFIRMED', label: '已確認', statuses: ['CONFIRMED', 'IN_PROGRESS'] },
  { key: 'COMPLETED', label: '已完成', statuses: ['COMPLETED'] },
  { key: 'CANCELLED', label: '已取消', statuses: ['CANCELLED'] }
]
// 預設只看進行中的：待確認 + 已確認
const selectedFilters = ref(['PENDING', 'CONFIRMED'])

function onFilterChange() {
  page.value = 0
  fetchData()
}

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
  const statuses = filters.filter((f) => selectedFilters.value.includes(f.key)).flatMap((f) => f.statuses)
  if (statuses.length === 0) {
    // 什麼分類都沒勾：不查詢，直接清空
    reservations.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    const result = await listReservationsByMember(authStore.userId, page.value, size.value, statuses)
    reservations.value = result.content
    total.value = result.totalElements
  } finally {
    loading.value = false
  }
}

const totalPages = computed(() => Math.max(Math.ceil(total.value / size.value), 1))

function goPage(p) {
  page.value = p
  fetchData()
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
.filter {
  margin-bottom: 16px;
  overflow-x: auto;
}
.pager {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 16px;
}
.pager .el-button {
  min-height: 44px;
  margin: 0;
}
.pager__info {
  flex: 1;
  text-align: center;
  font-size: 13px;
  color: var(--ink-text-secondary);
}
.empty-hint {
  padding: 24px 0;
}
.empty-hint p {
  margin: 0 0 12px;
  color: var(--ink-text-secondary);
}
</style>
