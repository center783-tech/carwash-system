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
          <el-tag :type="rowStatusTagType(row)">{{ rowStatusText(row) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="付款">
        <template #default="{ row }">
          <el-tag :type="paymentTagType(row.paymentStatus)">{{ paymentText(row) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="付款方式">
        <template #default="{ row }">
          <el-button v-if="row.status === 'PENDING'" type="primary" link @click="handlePay(row.id, 'CREDIT_CARD')">
            信用卡付款
          </el-button>
          <el-button v-if="row.status === 'PENDING'" type="primary" link @click="handlePay(row.id, 'ATM')">
            ATM 轉帳
          </el-button>
          <el-button
              v-if="row.status === 'PENDING' && row.paymentMethod !== 'CASH'"
              type="primary" link @click="handlePay(row.id, 'CASH')"
          >
            現場付現
          </el-button>
        </template>
      </el-table-column>
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button
              v-if="row.status === 'PENDING' || row.status === 'CONFIRMED'"
              type="danger" link
              @click="handleCancel(row)"
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
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listReservationsByMember, cancelReservation } from '@/api/reservation'
import { payByNewebPay, queryPendingNewebPayTrades, chooseCashPayment } from '@/api/payment'
import { useAuthStore } from '@/store/auth'

const router = useRouter()
const route = useRoute()
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
// 已確認且已付款的預約，狀態直接顯示「已付款」
const isPaidConfirmed = (row) => row.status === 'CONFIRMED' && row.paymentStatus === 'PAID'
const rowStatusText = (row) => (isPaidConfirmed(row) ? '已付款' : statusText(row.status))
const rowStatusTagType = (row) => (isPaidConfirmed(row) ? 'success' : statusTagType(row.status))

const paymentMethodText = { CREDIT_CARD: '信用卡', ATM: 'ATM 轉帳', CASH: '現金', NEWEBPAY: '藍新' }
const paymentTagMap = { PAID: 'success', REFUNDED: 'warning', UNPAID: 'info' }
const paymentTagType = (s) => paymentTagMap[s] || 'info'
function paymentText(row) {
  if (row.paymentStatus === 'PAID') {
    const method = paymentMethodText[row.paymentMethod] || row.paymentMethod
    return method ? `已付款（${method}）` : '已付款'
  }
  if (row.paymentStatus === 'REFUNDED') return '已退款'
  return row.paymentMethod === 'CASH' ? '未付款（現場付現）' : '未付款'
}

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

async function handleCancel(row) {
  const paidByCard = row.paymentStatus === 'PAID' && row.paymentMethod === 'CREDIT_CARD'
  const paidOther = row.paymentStatus === 'PAID' && !paidByCard
  let message = '確定要取消這筆預約嗎？'
  if (paidByCard) message = '確定要取消這筆預約嗎？取消後會自動退款至原信用卡。'
  if (paidOther) message = '確定要取消這筆預約嗎？此筆款項無法線上退款，請聯絡店家處理退款。'
  await ElMessageBox.confirm(message, '取消預約', { type: 'warning' })
  await cancelReservation(row.id)
  ElMessage.success(paidByCard ? '已取消預約，款項將退回原信用卡' : '已取消預約')
  fetchData()
}

async function handlePay(id, method) {
  if (method === 'CASH') {
    await chooseCashPayment(id)
    ElMessage.success('已選擇現場付現，請於到場時付款')
    fetchData()
    return
  }
  await payByNewebPay(id, method)
}

onMounted(async () => {
  // 從藍新刷卡頁導回：NotifyURL 可能還沒到，先補查逾時未付款的交易再載入列表
  if (route.query.payment === 'newebpay') {
    try {
      await queryPendingNewebPayTrades()
    } catch {
      // 補查失敗不影響列表顯示
    }
    ElMessage.info('已回到本站，付款結果以下方預約狀態為準（已付款會顯示「已確認」）')
  }
  fetchData()
})
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
