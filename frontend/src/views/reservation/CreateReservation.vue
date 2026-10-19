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
      <el-date-picker v-model="form.reservationDate" type="date" placeholder="選擇日期" style="margin-right: 12px" />
      <el-time-select
          v-model="form.timeSlotStart"
          start="09:00" step="00:30" end="20:00"
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
        <el-descriptions-item label="日期">{{ formatDate(form.reservationDate) }}</el-descriptions-item>
        <el-descriptions-item label="時段">{{ form.timeSlotStart }}</el-descriptions-item>
        <el-descriptions-item label="金額">NT$ {{ selectedService?.price }}</el-descriptions-item>
      </el-descriptions>

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
import { createReservation } from '@/api/reservation'
import { useAuthStore } from '@/store/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const activeStep = ref(0)
const services = ref([])
const vehicles = ref([])
const submitting = ref(false)

const form = reactive({
  serviceId: route.query.serviceId ? Number(route.query.serviceId) : null,
  reservationDate: null,
  timeSlotStart: null,
  vehicleId: null,
  remark: ''
})

const selectedService = computed(() => services.value.find((s) => s.id === form.serviceId))

function formatDate(date) {
  if (!date) return ''
  return new Date(date).toISOString().slice(0, 10)
}

function onServiceSelected() {
  // 預留：選完服務後可以依服務時長限制可選時段
}

async function handleSubmit() {
  submitting.value = true
  try {
    const reservation = await createReservation({
      memberId: authStore.userId,
      vehicleId: form.vehicleId,
      serviceId: form.serviceId,
      employeeId: null, // 不指定員工，後台再指派
      reservationDate: formatDate(form.reservationDate),
      timeSlotStart: form.timeSlotStart,
      couponId: null,
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
  if (form.serviceId) activeStep.value = 1
})
</script>

<style scoped>
.page-container {
  max-width: 700px;
  margin: 24px auto;
  padding: 0 16px;
}
</style>
