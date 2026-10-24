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
        <el-option v-for="s in services" :key="s.id" :label="`${s.name}（NT$${s.price}）`" :value="s.id" />
      </el-select>
      <el-button type="primary" style="margin-top: 16px" :disabled="!form.serviceId" @click="activeStep = 1">
        下一步
      </el-button>
    </div>

    <!-- Step 2: 選日期時段 -->
    <div v-else-if="activeStep === 1">
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 16px">
        預約需在 12 小時前完成，太早或太晚的時段不會顯示在選項裡。
      </el-alert>

      <el-date-picker
          v-model="form.reservationDate"
          type="date"
          placeholder="選擇日期"
          value-format="YYYY-MM-DD"
          style="margin-right: 12px"
          :disabled-date="isDateDisabled"
          @change="form.timeSlotStart = null"
      />
      <el-time-select
          v-model="form.timeSlotStart"
          start="09:00" step="00:30" end="20:00"
          :min-time="minTimeForSelectedDate"
          placeholder="選擇時段"
      />
      <div style="margin-top: 16px">
        <el-button @click="activeStep = 0">上一步</el-button>
        <el-button type="primary" :disabled="!form.reservationDate || !form.timeSlotStart" @click="activeStep = 2">
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
      <div style="margin-top: 16px">
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
        <el-descriptions-item label="原價">
          <span :class="{ 'price-original--struck': selectedCoupon }">NT$ {{ selectedService?.price }}</span>
        </el-descriptions-item>
        <el-descriptions-item v-if="selectedCoupon" label="折扣後金額">
          <span class="price-tag">NT$ {{ estimatedTotal }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <div class="coupon-picker">
        <label>使用優惠券</label>
        <el-select v-model="form.couponId" placeholder="不使用優惠券" clearable style="width: 100%">
          <el-option
              v-for="c in usableCoupons"
              :key="c.id"
              :value="c.id"
              :label="couponLabel(c)"
              :disabled="!meetsThreshold(c)"
          />
        </el-select>
        <p v-if="usableCoupons.length === 0" class="coupon-empty">目前沒有可使用的優惠券</p>
        <p v-else-if="usableCoupons.some((c) => !meetsThreshold(c))" class="coupon-empty">
          灰色選項代表這次的金額還未達該優惠券的最低消費門檻
        </p>
        <p v-if="selectedCoupon" class="coupon-estimate-hint">
          折扣金額以實際送出結果為準，這裡顯示的是預估金額
        </p>
      </div>

      <el-input v-model="form.remark" placeholder="備註（選填）" style="margin-top: 16px" />

      <div style="margin-top: 16px">
        <el-button @click="activeStep = 2">上一步</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">送出預約</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listServices } from '@/api/service'
import { listVehicles } from '@/api/member'
import { listMyCoupons } from '@/api/coupon'
import { createReservation } from '@/api/reservation'
import { useAuthStore } from '@/store/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const activeStep = ref(0)
const services = ref([])
const vehicles = ref([])
const myCoupons = ref([])
const submitting = ref(false)

const form = reactive({
  serviceId: route.query.serviceId ? Number(route.query.serviceId) : null,
  reservationDate: null,
  timeSlotStart: null,
  vehicleId: null,
  couponId: null,
  remark: ''
})

const selectedService = computed(() => services.value.find((s) => s.id === form.serviceId))
const usableCoupons = computed(() => myCoupons.value.filter((c) => c.isUsed === 0))
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
  const dayEnd = new Date(date)
  dayEnd.setHours(20, 0, 0, 0)
  return dayEnd < earliestAllowed()
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

async function handleSubmit() {
  submitting.value = true
  try {
    await createReservation({
      memberId: authStore.userId,
      vehicleId: form.vehicleId,
      serviceId: form.serviceId,
      employeeId: null, // 不指定員工，後台再指派
      reservationDate: form.reservationDate, // 已經是 YYYY-MM-DD 字串，直接送出，不用再轉換
      timeSlotStart: form.timeSlotStart,
      couponId: form.couponId,
      remark: form.remark
    })
    ElMessage.success('預約建立成功')
    router.push(`/reservations`)
    // 如需接著導去付款頁，可改成：
    // router.push({ path: '/payment', query: { reservationId: reservation.id } })
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  services.value = await listServices()
  vehicles.value = await listVehicles(authStore.userId)
  myCoupons.value = await listMyCoupons(authStore.userId)
  if (form.serviceId) activeStep.value = 1
})
</script>

<style scoped>
.page-container {
  max-width: 700px;
  margin: 24px auto;
  padding: 0 16px;
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
.price-original--struck {
  text-decoration: line-through;
  color: var(--ink-text-secondary);
}
</style>
