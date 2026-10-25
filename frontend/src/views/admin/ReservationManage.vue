<template>
  <div>
  <AdminNav />
  <div class="page-container">
    <div class="header-row">
      <h2>預約管理</h2>
    </div>

    <div class="filter-panel">
      <div class="filter-row">
        <span class="filter-label">快速選擇</span>
        <el-radio-group v-model="quickRange" @change="onQuickChange">
          <el-radio-button value="today">今天</el-radio-button>
          <el-radio-button value="week">未來一週</el-radio-button>
        </el-radio-group>
      </div>
      <div class="filter-row">
        <span class="filter-label">自訂日期</span>
        <el-date-picker
            v-model="dateRange"
            type="daterange"
            unlink-panels
            range-separator="至"
            start-placeholder="開始日期"
            end-placeholder="結束日期"
            value-format="YYYY-MM-DD"
            :editable="false"
            @change="onCustomChange"
        />
      </div>
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
          <el-tag :type="rowStatusTagType(row)">{{ rowStatusText(row) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="付款">
        <template #default="{ row }">
          <el-tag :type="paymentTagType(row.paymentStatus)">{{ paymentText(row) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button v-if="row.status === 'PENDING'" link type="primary" @click="handleConfirm(row.id)">
            確認
          </el-button>
          <el-button
              v-if="canCollectCash(row)"
              link type="warning" @click="handleCollectCash(row)"
          >
            現場收款
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
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminNav from '@/components/AdminNav.vue'
import { listReservationsByDateRange, confirmReservation, completeReservation } from '@/api/reservation'
import { collectCashPayment } from '@/api/payment'

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

// 日期一律用「本地時區」組字串；toISOString() 是 UTC，台灣凌晨 0~8 點會變成前一天
function toDateString(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function rangeFor(type) {
  const start = new Date()
  const end = new Date()
  if (type === 'week') end.setDate(end.getDate() + 6) // 今天起算一週（含今天共 7 天）
  return [toDateString(start), toDateString(end)]
}

// 快速選擇：今天／未來一週；選了就同步帶入下方的自訂日期欄位
// 自訂日期：自己選起訖日，這時快速選擇不再亮起
const quickRange = ref('week')
const dateRange = ref(rangeFor('week'))

function onQuickChange(type) {
  dateRange.value = rangeFor(type)
  fetchData()
}

function onCustomChange(value) {
  quickRange.value = null
  if (!value) return
  fetchData()
}

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

// 還沒付款、也沒被取消/完成的預約才能現場收款
const canCollectCash = (row) =>
  row.paymentStatus !== 'PAID' && ['PENDING', 'CONFIRMED', 'IN_PROGRESS'].includes(row.status)

async function handleCollectCash(row) {
  await ElMessageBox.confirm(`確認已向會員收取現金 NT$ ${row.totalAmount}？收款後預約會自動確認。`, '現場收款', {
    confirmButtonText: '已收款',
    cancelButtonText: '取消'
  })
  await collectCashPayment(row.id)
  ElMessage.success('已收款')
  fetchData()
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
.filter-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin: 8px 0 16px;
}
.filter-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.filter-label {
  width: 64px;
  font-size: 13px;
  color: var(--ink-text-secondary);
}
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
