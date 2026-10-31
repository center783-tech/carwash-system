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

    <el-tabs v-model="category" class="category-tabs">
      <el-tab-pane v-for="c in categories" :key="c.key" :name="c.key">
        <template #label>{{ c.label }}<span class="cat-count">{{ categoryCount(c.key) }}</span></template>
      </el-tab-pane>
    </el-tabs>

    <el-table :data="filteredReservations" v-loading="loading" class="desktop-only">
      <el-table-column label="編號" width="80">
        <template #default="{ row }"><span class="order-no">{{ orderNo(row) }}</span></template>
      </el-table-column>
      <el-table-column prop="reservationDate" label="日期" width="110" />
      <el-table-column prop="memberName" label="會員" />
      <el-table-column prop="plateNumber" label="車牌" />
      <el-table-column prop="zoneName" label="車位區" />
      <el-table-column prop="serviceName" label="服務項目" />
      <el-table-column label="指派員工">
        <template #default="{ row }">
          <span v-if="row.employeeName">{{ row.employeeName }}</span>
          <span v-else class="unassigned">未指派</span>
          <el-button v-if="canAssign(row)" link type="primary" @click="openAssign(row)">
            {{ row.employeeName ? '改派' : '指派' }}
          </el-button>
        </template>
      </el-table-column>
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
          <el-button v-if="row.status === 'CONFIRMED'" link type="success" @click="handleComplete(row)">
            標記完成
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 手機版：精簡卡片 -->
    <div class="mobile-only" v-loading="loading">
      <div v-for="row in filteredReservations" :key="row.id" class="rcard">
        <div class="rcard-head">
          <span class="order-no">{{ orderNo(row) }}</span>
          <span class="rcard-time">{{ row.reservationDate.slice(5) }} {{ row.timeSlotStart }}</span>
          <el-tag size="small" :type="rowStatusTagType(row)">{{ rowStatusText(row) }}</el-tag>
        </div>
        <div class="rcard-main">{{ row.plateNumber }}・{{ row.serviceName }}</div>
        <div class="rcard-sub">
          {{ row.memberName }}・NT$ {{ row.totalAmount }}・{{ paymentText(row) }}
        </div>
        <div class="rcard-sub">
          員工：<span v-if="row.employeeName">{{ row.employeeName }}</span><span v-else class="unassigned">未指派</span>
          <el-button v-if="canAssign(row)" link type="primary" @click="openAssign(row)">
            {{ row.employeeName ? '改派' : '指派' }}
          </el-button>
        </div>
        <div class="rcard-actions">
          <el-button v-if="row.status === 'PENDING'" size="small" type="primary" @click="handleConfirm(row.id)">確認</el-button>
          <el-button v-if="canCollectCash(row)" size="small" type="warning" @click="handleCollectCash(row)">現場收款</el-button>
          <el-button v-if="row.status === 'CONFIRMED'" size="small" type="success" @click="handleComplete(row)">標記完成</el-button>
        </div>
      </div>
    </div>

    <el-empty v-if="!loading && filteredReservations.length === 0" description="沒有符合的預約紀錄" />

    <!-- 指派員工：只列出此時段可指派的人（在職、會此服務、當天有班、時段不衝突） -->
    <el-dialog v-model="assignVisible" title="指派員工" width="420px">
      <p v-if="assignTarget" class="assign-info">
        {{ assignTarget.reservationDate }} {{ assignTarget.timeSlotStart }}・{{ assignTarget.serviceName }}
      </p>
      <el-select v-model="assignEmployeeId" placeholder="請選擇員工" clearable style="width: 100%"
                 :loading="assignLoading" no-data-text="這個時段沒有可指派的員工">
        <el-option v-for="e in assignable" :key="e.id" :value="e.id"
                   :label="e.title ? `${e.name}（${e.title}）` : e.name" />
      </el-select>
      <p class="assign-hint">清空選項後儲存，會取消目前的指派。</p>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="assigning" @click="handleAssign">儲存</el-button>
      </template>
    </el-dialog>
  </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminNav from '@/components/AdminNav.vue'
import {
  listReservationsByDateRange, confirmReservation, completeReservation,
  assignEmployee, listAssignableEmployees
} from '@/api/reservation'
import { collectCashPayment } from '@/api/payment'

const reservations = ref([])
const loading = ref(false)

// 預約編號：星期字母（週一=A、週二=B…週日=G）＋ 當天流水號（001 起，依建立順序）
const WEEK_LETTERS = ['G', 'A', 'B', 'C', 'D', 'E', 'F'] // Date.getDay(): 0=週日
const orderNoMap = computed(() => {
  const byDate = {}
  for (const r of reservations.value) (byDate[r.reservationDate] ||= []).push(r)
  const map = {}
  for (const [date, list] of Object.entries(byDate)) {
    const [y, m, d] = date.split('-').map(Number)
    const letter = WEEK_LETTERS[new Date(y, m - 1, d).getDay()]
    ;[...list].sort((a, b) => a.id - b.id).forEach((r, i) => {
      map[r.id] = letter + String(i + 1).padStart(3, '0')
    })
  }
  return map
})
// 分類：依預約狀態／付款狀況篩選（已確認且已付款的歸在「已付款」）
const categories = [
  { key: 'ALL', label: '全部' },
  { key: 'PENDING', label: '待確認' },
  { key: 'PAID', label: '已付款' },
  { key: 'UNPAID', label: '未付款' },
  { key: 'COMPLETED', label: '已完成' },
  { key: 'CANCELLED', label: '已取消' }
]
const category = ref('ALL')
const inCategory = (row, key) => {
  switch (key) {
    case 'PENDING': return row.status === 'PENDING'
    case 'PAID': return row.status !== 'CANCELLED' && row.status !== 'COMPLETED' && row.paymentStatus === 'PAID'
    case 'UNPAID': return ['PENDING', 'CONFIRMED', 'IN_PROGRESS'].includes(row.status) && row.paymentStatus !== 'PAID'
    case 'COMPLETED': return row.status === 'COMPLETED'
    case 'CANCELLED': return row.status === 'CANCELLED'
    default: return true
  }
}
const categoryCount = (key) => reservations.value.filter((r) => inCategory(r, key)).length
const filteredReservations = computed(() => reservations.value.filter((r) => inCategory(r, category.value)))

const orderNo = (row) => orderNoMap.value[row.id] || ''

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

// 指派員工
const canAssign = (row) => ['PENDING', 'CONFIRMED', 'IN_PROGRESS'].includes(row.status)
const assignVisible = ref(false)
const assignTarget = ref(null)
const assignEmployeeId = ref(null)
const assignable = ref([])
const assignLoading = ref(false)
const assigning = ref(false)

async function openAssign(row) {
  assignTarget.value = row
  assignEmployeeId.value = row.employeeId ?? null
  assignable.value = []
  assignVisible.value = true
  assignLoading.value = true
  try {
    assignable.value = await listAssignableEmployees(row.id)
  } finally {
    assignLoading.value = false
  }
}

async function handleAssign() {
  assigning.value = true
  try {
    await assignEmployee(assignTarget.value.id, assignEmployeeId.value ?? null)
    ElMessage.success(assignEmployeeId.value ? '已指派員工' : '已取消指派')
    assignVisible.value = false
    fetchData()
  } finally {
    assigning.value = false
  }
}

async function handleConfirm(id) {
  await confirmReservation(id)
  ElMessage.success('已確認預約')
  fetchData()
}

// 現場付現且尚未收款的預約：標記完成前先確認收款，收款後一併標記完成
async function handleComplete(row) {
  // 不是今天的預約：先提醒，確定才繼續
  if (row.reservationDate !== toDateString(new Date())) {
    await ElMessageBox.confirm(`這不是今天的預約（${row.reservationDate}），確定要改成已完成？`, '非當天預約', {
      confirmButtonText: '確定完成',
      cancelButtonText: '取消',
      type: 'warning'
    })
  }
  if (row.paymentMethod === 'CASH' && row.paymentStatus !== 'PAID') {
    await ElMessageBox.confirm(`這筆是現場收款，尚未收款。確認已向會員收取現金 NT$ ${row.totalAmount}？收款後將一併標記完成。`, '現場收款', {
      confirmButtonText: '確認收款並完成',
      cancelButtonText: '取消'
    })
    await collectCashPayment(row.id)
  }
  await completeReservation(row.id)
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
.unassigned {
  color: var(--ink-text-secondary);
}
.assign-info {
  margin: 0 0 12px;
  font-size: 14px;
}
.assign-hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: var(--ink-text-secondary);
}
.category-tabs :deep(.el-tabs__header) { margin-bottom: 12px; }
.cat-count {
  margin-left: 4px;
  font-size: 12px;
  color: var(--ink-text-secondary);
}
.order-no {
  font-weight: 600;
  font-family: monospace;
  color: var(--el-color-primary);
}
.mobile-only { display: none; }
.rcard {
  padding: 10px 12px;
  margin-bottom: 8px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
}
.rcard-head { display: flex; align-items: center; gap: 8px; }
.rcard-time { flex: 1; font-size: 13px; color: var(--ink-text-secondary); }
.rcard-main { margin-top: 6px; font-size: 15px; font-weight: 500; }
.rcard-sub { margin-top: 2px; font-size: 12px; color: var(--ink-text-secondary); }
.rcard-actions { margin-top: 8px; display: flex; gap: 8px; flex-wrap: wrap; }
.rcard-actions:empty { display: none; }
@media (max-width: 768px) {
  .desktop-only { display: none; }
  .mobile-only { display: block; }
  .page-container { margin: 12px auto; }
  .filter-label { width: auto; }
}
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
