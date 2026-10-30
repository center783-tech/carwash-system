<template>
  <div class="page-container">
    <h2>建立預約</h2>

    <el-steps :active="activeStep" finish-status="success" style="margin-bottom: 24px">
      <el-step title="選擇服務" />
      <el-step title="選擇日期時段" />
      <el-step title="選擇車輛" />
      <el-step title="確認送出" />
    </el-steps>

    <!-- Step 1: 選服務 -->
    <div v-if="activeStep === 0">
      <el-select v-model="form.serviceId" placeholder="請選擇服務項目" style="width: 100%" @change="onServiceSelected">
        <el-option v-for="s in services" :key="s.id" :label="`${s.bundle ? '【組合優惠】' : ''}${s.name}（NT$${s.price}）`" :value="s.id" />
      </el-select>
      <div class="step-actions">
        <el-button type="primary" :disabled="!form.serviceId" @click="activeStep = 1">下一步</el-button>
      </div>
    </div>

    <!-- Step 2: 選日期時段 -->
    <div v-else-if="activeStep === 1">
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 16px">
        預約需在 12 小時前完成，太早或太晚的時段不會顯示在選項裡。
      </el-alert>

      <div class="date-picker-block">
        <label>1. 選擇月份</label>
        <el-date-picker
            v-model="selectedMonth"
            type="month"
            placeholder="選擇月份"
            value-format="YYYY-MM"
            :clearable="false"
            :editable="false"
            :disabled-date="isMonthDisabled"
            @change="form.reservationDate = null; form.timeSlotStart = null; form.zoneId = null"
        />
      </div>

      <template v-if="selectedMonth">
        <el-alert
            :type="closedInMonth.length ? 'warning' : 'success'"
            :closable="false"
            show-icon
            style="margin: 12px 0"
        >
          <template v-if="closedInMonth.length">
            本月休息日（無法預約）：
            <span v-for="c in closedInMonth" :key="c.id" class="closed-tag">
              {{ Number(c.closedDate.slice(8)) }} 號{{ c.reason ? `（${c.reason}）` : '' }}
            </span>
          </template>
          <template v-else>本月沒有休息日</template>
        </el-alert>

        <div class="date-picker-block">
          <label>2. 選擇幾號</label>
          <div class="day-grid">
            <button
                v-for="d in monthDays"
                :key="d.date"
                type="button"
                class="day-cell"
                :class="{ 'day-cell--active': form.reservationDate === d.date, 'day-cell--closed': d.closed }"
                :disabled="d.disabled"
                :title="d.closed ? '休息日' : ''"
                @click="selectDay(d.date)"
            >
              {{ d.day }}
              <small v-if="d.closed">休</small>
            </button>
          </div>
        </div>
      </template>

      <div class="date-picker-block" style="margin-top: 12px">
        <label>3. 選擇時段</label>
      <el-time-select
          v-model="form.timeSlotStart"
          start="09:00" step="00:30" end="20:00"
          :min-time="minTimeForSelectedDate"
          placeholder="選擇時段"
          :disabled="!form.reservationDate"
          :editable="false"
      />
      </div>
      <div v-if="form.reservationDate && form.timeSlotStart" class="zone-picker">
        <label>選擇車位區</label>
        <div v-loading="zonesLoading" class="zone-list">
          <button
              v-for="z in zones"
              :key="z.id"
              type="button"
              class="zone-card"
              :class="{ 'zone-card--active': form.zoneId === z.id, 'zone-card--full': z.available === 0 }"
              :disabled="z.available === 0"
              @click="form.zoneId = z.id"
          >
            <strong>{{ z.name }}</strong>
            <span>{{ z.available === 0 ? '已額滿' : `剩 ${z.available} 個車位` }}</span>
          </button>
        </div>
        <p v-if="!zonesLoading && zones.length === 0" class="coupon-empty">目前沒有開放預約的車位區</p>
        <p v-else-if="!zonesLoading && zones.every((z) => z.available === 0)" class="zone-full-hint">
          此時段所有車位區都已額滿，請改選其他日期或時段
        </p>
      </div>
      <div class="step-actions">
        <el-button @click="activeStep = 0">上一步</el-button>
        <el-button type="primary" :disabled="!form.reservationDate || !form.timeSlotStart || !form.zoneId" @click="activeStep = 2">
          下一步
        </el-button>
      </div>
    </div>

    <!-- Step 3: 選車輛 -->
    <div v-else-if="activeStep === 2">
      <el-select v-model="form.vehicleId" placeholder="請選擇車輛" style="width: 100%">
        <el-option v-for="v in vehicles" :key="v.id" :label="`${v.plateNumber}（${v.brand} ${v.model}）`" :value="v.id" />
      </el-select>
      <el-empty v-if="vehicles.length === 0" description="尚未新增車輛，請先到「我的車輛」新增">
        <el-button type="primary" @click="router.push('/vehicles')">前往新增</el-button>
      </el-empty>
      <div class="step-actions">
        <el-button @click="activeStep = 1">上一步</el-button>
        <el-button type="primary" :disabled="!form.vehicleId" @click="activeStep = 3">下一步</el-button>
      </div>
    </div>

    <!-- Step 4: 確認送出 -->
    <div v-else-if="activeStep === 3">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="服務項目">{{ selectedService?.name }}</el-descriptions-item>
        <el-descriptions-item label="日期">{{ form.reservationDate }}</el-descriptions-item>
        <el-descriptions-item label="時段">{{ form.timeSlotStart }}</el-descriptions-item>
        <el-descriptions-item label="車位區">{{ selectedZone?.name }}</el-descriptions-item>
        <el-descriptions-item label="原價">
          <span :class="{ 'price-original--struck': selectedCoupon }">NT$ {{ selectedService?.price }}</span>
        </el-descriptions-item>
        <el-descriptions-item v-if="selectedCoupon" label="折扣後金額">
          <span class="price-tag">NT$ {{ estimatedTotal }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <div class="coupon-picker">
        <label>使用優惠券</label>
        <el-select v-model="form.couponId" placeholder="不使用優惠券" clearable popper-class="coupon-popper" style="width: 100%">
          <el-option
              v-for="c in usableCoupons"
              :key="c.id"
              :value="c.id"
              :label="couponLabel(c)"
              :disabled="!meetsThreshold(c)"
          >
            <div class="coupon-option">
              <div class="coupon-option__main">
                <strong>{{ c.couponCode }}</strong>
                <span>{{ c.discountType === 'FIXED' ? `折抵 NT$${c.discountValue}` : `${c.discountValue}% 折扣` }}</span>
              </div>
              <div class="coupon-option__sub">
                <span>{{ c.minAmount > 0 ? `最低消費 NT$${c.minAmount}` : '無最低消費' }}</span>
                <span>有效至 {{ c.endDate }}</span>
                <span v-if="!meetsThreshold(c)" class="coupon-option__warn">還差 NT$ {{ shortfall(c) }}</span>
              </div>
            </div>
          </el-option>
        </el-select>
        <p v-if="usableCoupons.length === 0" class="coupon-empty">目前沒有可使用的優惠券</p>
        <p v-else-if="usableCoupons.some((c) => !meetsThreshold(c))" class="coupon-empty">
          灰色選項代表這次的金額還未達該優惠券的最低消費門檻
        </p>
        <p v-if="selectedCoupon" class="coupon-threshold-hint">
          此優惠券最低消費：{{ selectedCoupon.minAmount > 0 ? `NT$ ${selectedCoupon.minAmount}` : '無門檻' }}
        </p>
        <p v-if="selectedCoupon" class="coupon-estimate-hint">
          折扣金額以實際送出結果為準，這裡顯示的是預估金額
        </p>
      </div>

      <el-input v-model="form.remark" placeholder="備註（選填）" style="margin-top: 16px" />

      <div class="step-actions">
        <el-button @click="activeStep = 2">上一步</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">送出預約</el-button>
      </div>
    </div>

    <el-dialog v-model="payDialogVisible" title="選擇付款方式" width="min(420px, 92vw)" :show-close="true" @closed="pickPayMethod(null)">
      <p class="pay-dialog__hint">預約已建立。線上付款成功後預約會自動確認；選「稍後再付」可之後到「我的預約」付款。</p>
      <div class="pay-dialog__actions">
        <el-button type="primary" size="large" @click="pickPayMethod('CREDIT_CARD')">信用卡付款</el-button>
        <el-button size="large" @click="pickPayMethod('CASH')">現場付現</el-button>
        <el-button size="large" text @click="pickPayMethod(null)">稍後再付</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listServices } from '@/api/service'
import { listVehicles } from '@/api/member'
import { listMyCoupons } from '@/api/coupon'
import { createReservation } from '@/api/reservation'
import { payByNewebPay, chooseCashPayment } from '@/api/payment'
import { getZoneAvailability, listClosedDates } from '@/api/zone'
import { useAuthStore } from '@/store/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const activeStep = ref(0)
const services = ref([])
const vehicles = ref([])
const myCoupons = ref([])
const submitting = ref(false)
const zones = ref([])
const zonesLoading = ref(false)
const closedDates = ref([])

const form = reactive({
  serviceId: route.query.serviceId ? Number(route.query.serviceId) : null,
  reservationDate: null,
  timeSlotStart: null,
  zoneId: null,
  vehicleId: null,
  couponId: null,
  remark: ''
})

const selectedZone = computed(() => zones.value.find((z) => z.id === form.zoneId))
const selectedService = computed(() => services.value.find((s) => s.id === form.serviceId))
const usableCoupons = computed(() => myCoupons.value.filter((c) => c.isUsed === 0 && c.redeemRemainingSeconds == null))
const selectedCoupon = computed(() => usableCoupons.value.find((c) => c.id === form.couponId))

// 前端先估算一次折扣後金額，公式跟後端 ReservationServiceImpl.applyDiscount() 一致：
// FIXED：原價 - 折扣金額（不能小於 0）；PERCENTAGE：原價 x 折扣率（discountValue 以 100 為基準，例如 90 代表 9 折）
// 這裡只是給使用者送出前的「預估」，實際金額仍以後端算出來、寫進訂單的結果為準
const estimatedTotal = computed(() => {
  const price = selectedService.value?.price
  const coupon = selectedCoupon.value
  if (price == null || !coupon) return null

  if (coupon.discountType === 'FIXED') {
    return Math.max(price - coupon.discountValue, 0)
  }
  return Math.round(price * (coupon.discountValue / 100) * 10) / 10
})

function meetsThreshold(c) {
  const price = selectedService.value?.price
  if (price == null) return true
  return price >= (c.minAmount || 0)
}

function shortfall(c) {
  const price = selectedService.value?.price ?? 0
  return Math.max((c.minAmount || 0) - price, 0)
}

function couponLabel(c) {
  const discount = c.discountType === 'FIXED' ? `折抵 NT$${c.discountValue}` : `${c.discountValue}% 折扣`
  const threshold = c.minAmount > 0 ? `，滿NT$${c.minAmount}可用` : ''
  return `${c.couponCode}（${discount}${threshold}，有效至 ${c.endDate}）`
}

function onServiceSelected() {
  // 預留：選完服務後可以依服務時長限制可選時段
}

// 把 Date 物件轉成「本地時區」的 YYYY-MM-DD 字串，不能用 toISOString()（那是 UTC，
// 台灣是 UTC+8，用 toISOString() 會把日期往前推一天，這正是先前那個 10/24 變 10/23 的 bug）
function toDateString(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// 「現在 + 12 小時」是最早可以預約的時間點，後端 ReservationServiceImpl 也會做同樣的檢查，
// 這裡只是提前在畫面上把不可能選到的日期/時段擋掉，避免使用者選到一半才被後端打回票
function earliestAllowed() {
  const d = new Date()
  d.setHours(d.getHours() + 12)
  return d
}

// 營業時間最晚到 20:00，如果連當天 20:00 都不滿 12 小時，這天就整天不能選
function isDateDisabled(date) {
  if (closedDates.value.some((c) => c.closedDate === toDateString(date))) return true
  const dayEnd = new Date(date)
  dayEnd.setHours(20, 0, 0, 0)
  return dayEnd < earliestAllowed()
}

const selectedMonth = ref(null) // 'YYYY-MM'

// 月份選擇器：整個月都已過期（最後一天 20:00 不滿 12 小時）就不能選
function isMonthDisabled(date) {
  const lastDay = new Date(date.getFullYear(), date.getMonth() + 1, 0, 20, 0, 0, 0)
  return lastDay < earliestAllowed()
}

const closedInMonth = computed(() =>
    closedDates.value.filter((c) => selectedMonth.value && c.closedDate.startsWith(selectedMonth.value))
)

const monthDays = computed(() => {
  if (!selectedMonth.value) return []
  const [y, m] = selectedMonth.value.split('-').map(Number)
  const count = new Date(y, m, 0).getDate()
  const closedSet = new Set(closedInMonth.value.map((c) => c.closedDate))
  return Array.from({ length: count }, (_, i) => {
    const date = `${selectedMonth.value}-${String(i + 1).padStart(2, '0')}`
    const closed = closedSet.has(date)
    return { day: i + 1, date, closed, disabled: closed || isDateDisabled(new Date(y, m - 1, i + 1)) }
  })
})

function selectDay(date) {
  form.reservationDate = date
  form.timeSlotStart = null
  form.zoneId = null
}

// 選到的那天如果剛好是「12 小時門檻」落在的那一天，時段選擇要限制最早可選時間；
// 之後的日期因為一整天都滿足 12 小時，不用再限制。
// form.reservationDate 現在已經是 value-format="YYYY-MM-DD" 給的字串，直接比對字串，
// 不要再包一層 new Date() 去解析，避免跟上面同樣的時區問題。
const minTimeForSelectedDate = computed(() => {
  if (!form.reservationDate) return undefined

  const earliest = earliestAllowed()
  if (form.reservationDate !== toDateString(earliest)) return undefined

  // 無條件進位到下一個 30 分鐘刻度，確保顯示出來的最早時段一定滿足 12 小時（不會因為無條件捨去而少算）
  let hours = earliest.getHours()
  let minutes = earliest.getMinutes()
  if (minutes === 0) {
    // 剛好整點不用進位
  } else if (minutes <= 30) {
    minutes = 30
  } else {
    minutes = 0
    hours += 1
  }
  return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}`
})

// 選好服務、日期、時段後，查各車位區此時段的剩餘車位；沒有空位的區不能選
watch(
    () => [form.serviceId, form.reservationDate, form.timeSlotStart],
    async ([serviceId, date, start]) => {
      zones.value = []
      if (!serviceId || !date || !start) return
      zonesLoading.value = true
      try {
        zones.value = await getZoneAvailability(date, start, serviceId)
        if (form.zoneId && !zones.value.some((z) => z.id === form.zoneId && z.available > 0)) {
          form.zoneId = null
        }
      } finally {
        zonesLoading.value = false
      }
    }
)

// 付款方式選擇視窗：回傳 CREDIT_CARD / CASH，或 null（稍後再付）
const payDialogVisible = ref(false)
let payResolve = null
function askPayMethod() {
  return new Promise((resolve) => {
    payResolve = resolve
    payDialogVisible.value = true
  })
}
function pickPayMethod(method) {
  if (!payResolve) return
  const resolve = payResolve
  payResolve = null
  payDialogVisible.value = false
  resolve(method)
}

async function handleSubmit() {
  submitting.value = true
  try {
    const reservation = await createReservation({
      memberId: authStore.userId,
      vehicleId: form.vehicleId,
      serviceId: form.serviceId,
      employeeId: null, // 不指定員工，後台再指派
      zoneId: form.zoneId,
      reservationDate: form.reservationDate, // 已經是 YYYY-MM-DD 字串，直接送出，不用再轉換
      timeSlotStart: form.timeSlotStart,
      couponId: form.couponId,
      remark: form.remark
    })
    ElMessage.success('預約建立成功')
    // 預約建立後詢問付款方式；不付款也可以之後在「我的預約」補付
    const payMethod = await askPayMethod()
    if (payMethod === 'CASH') {
      try {
        await chooseCashPayment(reservation.id)
        ElMessage.success('已選擇現場付現，請於到場時付款')
      } catch {
        // 建立現金付款單失敗不影響預約，到場時店家仍可收款
      }
    } else if (payMethod) {
      try {
        await payByNewebPay(reservation.id, payMethod)
        return
      } catch {
        // 付款表單建立失敗：預約已建立，回預約列表可再按付款按鈕重試
      }
    }
    router.push('/reservations')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  services.value = await listServices()
  vehicles.value = await listVehicles(authStore.userId)
  myCoupons.value = await listMyCoupons(authStore.userId)
  closedDates.value = await listClosedDates()
  if (form.serviceId) activeStep.value = 1
})
</script>

<style scoped>
.pay-dialog__hint {
  margin: 0 0 16px;
  color: var(--ink-text-secondary);
}
.pay-dialog__actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.pay-dialog__actions .el-button {
  margin: 0;
}
.page-container {
  max-width: 700px;
  margin: 24px auto;
  padding: 0 16px;
}
.step-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 24px;
}
.step-actions .el-button {
  flex: 0 1 140px;
  min-width: 110px;
  min-height: 44px;
  margin: 0;
}
.coupon-picker {
  margin-top: 16px;
}
.coupon-picker label {
  display: block;
  font-size: 13px;
  color: var(--ink-text-secondary);
  margin-bottom: 6px;
}
.coupon-empty {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--ink-text-secondary);
}
.coupon-estimate-hint {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--ink-text-secondary);
}
.coupon-threshold-hint {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--rinse-deep);
}
.coupon-option {
  display: flex;
  flex-direction: column;
  justify-content: center;
  line-height: 1.4;
  padding: 4px 0;
  height: auto;
}
.coupon-option__main {
  display: flex;
  gap: 8px;
  align-items: baseline;
}
.coupon-option__sub {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  font-size: 12px;
  color: var(--ink-text-secondary);
}
.coupon-option__warn {
  color: var(--danger);
}
.date-picker-block label {
  display: block;
  font-size: 13px;
  color: var(--ink-text-secondary);
  margin-bottom: 6px;
}
.day-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 6px;
}
.day-cell {
  padding: 8px 0;
  font: inherit;
  cursor: pointer;
  color: var(--ink);
  background: #fff;
  border: 1px solid var(--paper-dim);
  border-radius: var(--radius-md);
}
.day-cell small {
  margin-left: 2px;
  font-size: 11px;
}
.day-cell:disabled {
  cursor: not-allowed;
  opacity: 0.4;
}
.day-cell--closed {
  background: var(--paper-dim);
  color: var(--danger);
}
.day-cell--active {
  border-color: var(--rinse);
  background: var(--el-color-primary-light-9);
}
.closed-tag {
  margin-right: 12px;
  white-space: nowrap;
}
.zone-picker {
  margin-top: 16px;
}
.zone-picker label {
  display: block;
  font-size: 13px;
  color: var(--ink-text-secondary);
  margin-bottom: 6px;
}
.zone-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  min-height: 24px;
}
.zone-card {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 110px;
  padding: 10px 16px;
  text-align: left;
  cursor: pointer;
  color: var(--ink);
  background: #fff;
  border: 1px solid var(--paper-dim);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
  font: inherit;
}
.zone-card span {
  font-size: 12px;
  color: var(--ink-text-secondary);
}
.zone-card--active {
  border-color: var(--rinse);
  background: var(--el-color-primary-light-9);
}
.zone-card--full {
  cursor: not-allowed;
  opacity: 0.55;
}
.zone-full-hint {
  margin: 8px 0 0;
  font-size: 13px;
  color: var(--danger);
}
.price-original--struck {
  text-decoration: line-through;
  color: var(--ink-text-secondary);
}
</style>
