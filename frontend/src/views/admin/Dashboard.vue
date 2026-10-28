<template>
  <div>
    <AdminNav />

    <div class="page-container">
      <div class="head">
        <div>
          <h2>管理總覽</h2>
          <p class="head__sub">{{ todayLabel }}<span v-if="updatedAt">・更新於 {{ updatedAt }}</span></p>
        </div>
        <el-button :loading="loading" @click="fetchAll">重新整理</el-button>
      </div>

      <!-- 需要處理的事放最前面 -->
      <router-link v-if="pendingCount > 0" to="/admin/reservations" class="alert">
        <strong>{{ pendingCount }} 筆預約待確認</strong>
        <span>未來 7 天內還沒確認的預約，點此前往處理 →</span>
      </router-link>

      <div class="kpis">
        <div class="kpi">
          <span class="kpi__label">今日預約</span>
          <span class="kpi__value">{{ val(todayActive.length) }}</span>
          <span class="kpi__hint">
            完成 {{ val(todayCount('COMPLETED')) }}・服務中 {{ val(todayCount('IN_PROGRESS')) }}
          </span>
        </div>

        <div v-if="isAdminRole" class="kpi kpi--money">
          <span class="kpi__label">今日營收</span>
          <span class="kpi__value">{{ revenueToday === null ? '--' : money(revenueToday) }}</span>
          <span class="kpi__hint" :class="growthClass">{{ growthText }}</span>
        </div>

        <div class="kpi">
          <span class="kpi__label">上架服務</span>
          <span class="kpi__value">{{ val(services.length) }}</span>
          <span class="kpi__hint">單項 {{ val(services.length - bundleCount) }}・組合優惠 {{ val(bundleCount) }}</span>
        </div>

        <div class="kpi">
          <span class="kpi__label">在職員工</span>
          <span class="kpi__value">{{ val(activeEmployeeCount) }}</span>
          <span class="kpi__hint">今日有預約的員工 {{ val(busyEmployeeCount) }} 位</span>
        </div>
      </div>

      <div class="panels">
        <section class="panel panel--wide">
          <div class="panel__title">
            <h3>今日行程</h3>
            <router-link to="/admin/reservations">全部預約 →</router-link>
          </div>
          <el-table :data="todayActive" v-loading="loading" empty-text="今天還沒有預約" size="small">
            <el-table-column label="時間" width="110">
              <template #default="{ row }">{{ hm(row.timeSlotStart) }}–{{ hm(row.timeSlotEnd) }}</template>
            </el-table-column>
            <el-table-column prop="plateNumber" label="車牌" width="100" />
            <el-table-column prop="serviceName" label="服務" min-width="120" />
            <el-table-column label="員工" width="90">
              <template #default="{ row }">{{ row.employeeName || '未指派' }}</template>
            </el-table-column>
            <el-table-column label="狀態" width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="statusMap[row.status]?.type">{{ statusMap[row.status]?.text || row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="付款" width="80">
              <template #default="{ row }">
                <el-tag size="small" :type="row.paymentStatus === 'PAID' ? 'success' : 'info'" effect="plain">
                  {{ row.paymentStatus === 'PAID' ? '已付' : '未付' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section class="panel">
          <div class="panel__title"><h3>未來 7 天預約量</h3></div>
          <div class="bars">
            <div v-for="d in week" :key="d.date" class="bar" :class="{ 'bar--today': d.isToday }">
              <span class="bar__num">{{ d.count }}</span>
              <div class="bar__track"><div class="bar__fill" :style="{ height: d.pct + '%' }" /></div>
              <span class="bar__day">{{ d.label }}</span>
            </div>
          </div>
        </section>
      </div>

      <div class="shortcuts">
        <router-link to="/admin/reservations">預約管理</router-link>
        <router-link to="/admin/services">服務項目</router-link>
        <router-link to="/admin/coupons">優惠券</router-link>
        <router-link to="/admin/employees">員工管理</router-link>
        <router-link v-if="isAdminRole" to="/admin/reports">營收報表</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import AdminNav from '@/components/AdminNav.vue'
import { useAuthStore } from '@/store/auth'
import { listReservationsByDateRange } from '@/api/reservation'
import { listServices } from '@/api/service'
import { listEmployees } from '@/api/employee'
import { getRevenueReport } from '@/api/report'

const authStore = useAuthStore()
// 營收報表後端只開放 ADMIN，STAFF 看不到營收卡片
const isAdminRole = computed(() => authStore.role === 'ADMIN')

const statusMap = {
  PENDING: { text: '待確認', type: 'info' },
  CONFIRMED: { text: '已確認', type: 'warning' },
  IN_PROGRESS: { text: '服務中', type: 'warning' },
  COMPLETED: { text: '已完成', type: 'success' },
  CANCELLED: { text: '已取消', type: 'danger' }
}

const loading = ref(false)
const updatedAt = ref('')
const weekReservations = ref(null)
const services = ref([])
const employees = ref(null)
const revenue = ref(null)

// 本地時區的 YYYY-MM-DD；不能用 toISOString()（UTC），台灣凌晨 0~8 點會變成前一天
function dateString(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

const WEEKDAYS = ['日', '一', '二', '三', '四', '五', '六']
const todayStr = computed(() => dateString(new Date()))
const todayLabel = computed(() => {
  const n = new Date()
  return `${n.getFullYear()}/${n.getMonth() + 1}/${n.getDate()}（週${WEEKDAYS[n.getDay()]}）`
})

const val = (n) => (weekReservations.value === null && n === 0 ? '--' : n)
const money = (n) => `NT$ ${Number(n).toLocaleString()}`
const hm = (t) => (t ? String(t).slice(0, 5) : '')

// 預約：取消的不算進任何數字
const activeWeek = computed(() => (weekReservations.value || []).filter((r) => r.status !== 'CANCELLED'))
const todayActive = computed(() =>
  activeWeek.value
    .filter((r) => r.reservationDate === todayStr.value)
    .sort((a, b) => String(a.timeSlotStart).localeCompare(String(b.timeSlotStart)))
)
const todayCount = (status) => todayActive.value.filter((r) => r.status === status).length
const pendingCount = computed(() => activeWeek.value.filter((r) => r.status === 'PENDING').length)
const busyEmployeeCount = computed(
  () => new Set(todayActive.value.map((r) => r.employeeId).filter(Boolean)).size
)

const bundleCount = computed(() => services.value.filter((s) => s.bundle).length)
const activeEmployeeCount = computed(() =>
  employees.value === null ? 0 : employees.value.filter((e) => e.status === 1).length
)

const week = computed(() => {
  const days = []
  for (let i = 0; i < 7; i++) {
    const d = new Date()
    d.setDate(d.getDate() + i)
    const key = dateString(d)
    days.push({
      date: key,
      isToday: i === 0,
      label: i === 0 ? '今天' : `${d.getMonth() + 1}/${d.getDate()}`,
      count: activeWeek.value.filter((r) => r.reservationDate === key).length
    })
  }
  const max = Math.max(1, ...days.map((d) => d.count))
  return days.map((d) => ({ ...d, pct: d.count ? Math.max(8, (d.count / max) * 100) : 0 }))
})

// 營收：與上一個同長度區間（昨天）比較
const revenueToday = computed(() => (revenue.value ? Number(revenue.value.summary.revenue) : null))
const growthText = computed(() => {
  const s = revenue.value?.summary
  if (!s) return ''
  if (s.growthRate === null) return Number(s.previousRevenue) > 0 ? '' : '昨日無營收可比較'
  return `${s.growthRate >= 0 ? '▲' : '▼'} 較昨日 ${Math.abs(s.growthRate)}%`
})
const growthClass = computed(() => {
  const g = revenue.value?.summary?.growthRate
  if (g === null || g === undefined) return ''
  return g >= 0 ? 'up' : 'down'
})

async function fetchAll() {
  loading.value = true
  const end = new Date()
  end.setDate(end.getDate() + 6)

  // 各項資料各自失敗不互相影響，失敗的卡片維持顯示 '--'
  const tasks = [
    listReservationsByDateRange(todayStr.value, dateString(end)).then((l) => { weekReservations.value = l }),
    listServices().then((l) => { services.value = l }),
    listEmployees().then((l) => { employees.value = l })
  ]
  if (isAdminRole.value) {
    tasks.push(getRevenueReport('DAY', todayStr.value).then((r) => { revenue.value = r }))
  }
  await Promise.allSettled(tasks)

  const n = new Date()
  updatedAt.value = `${String(n.getHours()).padStart(2, '0')}:${String(n.getMinutes()).padStart(2, '0')}`
  loading.value = false
}

// 櫃檯會一直開著這頁，每分鐘自動更新一次
let timer = null
onMounted(() => {
  fetchAll()
  timer = setInterval(fetchAll, 60000)
})
onBeforeUnmount(() => clearInterval(timer))
</script>

<style scoped>
.page-container {
  max-width: 1100px;
  margin: 24px auto;
  padding: 0 16px 40px;
}
.head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 16px;
}
.head h2 {
  margin: 0;
}
.head__sub {
  margin: 4px 0 0;
  color: var(--ink-text-secondary);
  font-size: 14px;
}

.alert {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px 12px;
  margin-bottom: 16px;
  padding: 12px 16px;
  border-radius: var(--radius-md);
  background: #fff4e0;
  border: 1px solid var(--wax);
  color: #8a5a00;
  text-decoration: none;
}
.alert strong {
  font-size: 16px;
}

.kpis {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}
.kpi {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 16px 18px;
  background: #fff;
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
}
.kpi--money {
  background: var(--ink);
  color: #fff;
}
.kpi__label {
  font-size: 13px;
  color: var(--ink-text-secondary);
}
.kpi--money .kpi__label,
.kpi--money .kpi__hint {
  color: #b7c4c9;
}
.kpi__value {
  font-family: var(--font-display);
  font-size: 30px;
  font-weight: 700;
  color: var(--rinse-deep);
  line-height: 1.2;
}
.kpi--money .kpi__value {
  color: var(--wax);
}
.kpi__hint {
  font-size: 12px;
  color: var(--ink-text-secondary);
  min-height: 16px;
}
.kpi__hint.up {
  color: #7fd89a;
}
.kpi__hint.down {
  color: #ff9a8f;
}

.panels {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(0, 1fr);
  gap: 16px;
}
.panel {
  padding: 16px 18px;
  background: #fff;
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
}
.panel__title {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 10px;
}
.panel__title h3 {
  margin: 0;
  font-size: 16px;
}
.panel__title a {
  font-size: 13px;
  color: var(--rinse-deep);
  text-decoration: none;
}

.bars {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  height: 180px;
}
.bar {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  height: 100%;
}
.bar__num {
  font-size: 13px;
  font-weight: 600;
}
.bar__track {
  flex: 1;
  width: 100%;
  display: flex;
  align-items: flex-end;
  background: var(--paper);
  border-radius: 6px;
  overflow: hidden;
}
.bar__fill {
  width: 100%;
  background: var(--rinse);
  border-radius: 6px 6px 0 0;
  transition: height 0.4s ease;
}
.bar--today .bar__fill {
  background: var(--wax);
}
.bar__day {
  font-size: 12px;
  color: var(--ink-text-secondary);
}
.bar--today .bar__day {
  color: var(--ink);
  font-weight: 600;
}

.shortcuts {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}
.shortcuts a {
  padding: 8px 16px;
  border-radius: 999px;
  background: #fff;
  box-shadow: var(--shadow-card);
  color: var(--ink);
  text-decoration: none;
  font-size: 14px;
}
.shortcuts a:hover {
  background: var(--rinse);
  color: #fff;
}

@media (max-width: 800px) {
  .panels {
    grid-template-columns: 1fr;
  }
}
@media (prefers-reduced-motion: reduce) {
  .bar__fill {
    transition: none;
  }
}
</style>
