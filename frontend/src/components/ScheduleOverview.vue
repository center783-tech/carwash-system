<template>
  <div>
    <div class="toolbar">
      <el-radio-group v-model="viewMode" size="small">
        <el-radio-button value="week">週</el-radio-button>
        <el-radio-button value="month">月</el-radio-button>
      </el-radio-group>
    </div>

    <div class="nav-bar">
      <el-button size="small" @click="shift(-1)">‹ {{ viewMode === 'week' ? '上一週' : '上個月' }}</el-button>
      <div class="nav-bar__title">
        <strong>{{ titleText }}</strong>
        <el-button v-if="!isCurrent" link type="primary" size="small" @click="goCurrent">
          回到{{ viewMode === 'week' ? '本週' : '本月' }}
        </el-button>
      </div>
      <el-button size="small" @click="shift(1)">{{ viewMode === 'week' ? '下一週' : '下個月' }} ›</el-button>
    </div>

    <!-- ========== 週檢視 ========== -->
    <div v-if="viewMode === 'week'" v-loading="loading" class="grid">
      <div class="grid__head">
        <span>日期</span>
        <span class="head--AM">上午班<small>08:00–12:00</small></span>
        <span class="head--PM">下午班<small>13:00–17:00</small></span>
      </div>
      <div v-for="d in weekDays" :key="d.date" class="grid__row" :class="{ 'grid__row--today': d.today }">
        <div class="grid__date">
          <strong>{{ d.label }}</strong>
          <span>週{{ d.weekday }}</span>
          <el-tag v-if="d.closed" type="info" size="small">休息日</el-tag>
        </div>
        <div v-for="sh in SHIFT_KEYS" :key="sh" class="slot" :class="[slotClass(d, sh), `shift--${sh}`]">
          <span v-for="n in working(d.date, sh)" :key="n.id" class="chip" :style="chipStyle(n)">
            <i class="chip__avatar">{{ initial(n) }}</i>{{ n.employeeName }}
          </span>
          <span v-for="n in onLeave(d.date, sh)" :key="n.id" class="chip chip--leave">
            <i class="chip__avatar">{{ initial(n) }}</i>{{ n.employeeName }}<em>請假</em>
          </span>
          <span v-if="!working(d.date, sh).length" class="slot__empty">{{ emptyText(d) }}</span>
        </div>
      </div>
    </div>

    <!-- ========== 月檢視 ========== -->
    <template v-else>
      <div v-loading="loading" class="month">
        <div class="month__head">
          <span v-for="w in ['一', '二', '三', '四', '五', '六', '日']" :key="w">{{ w }}</span>
        </div>
        <div class="month__body">
          <button
              v-for="d in monthDays"
              :key="d.date"
              type="button"
              class="day"
              :class="{
                'day--out': !d.inMonth,
                'day--today': d.today,
                'day--selected': selectedDate === d.date,
                'day--closed': d.closed
              }"
              @click="selectedDate = d.date"
          >
            <span class="day__num">{{ d.day }}</span>
            <template v-if="d.inMonth">
              <span v-if="d.closed" class="day__closed">休</span>
              <template v-else>
                <span v-for="sh in SHIFT_KEYS" :key="sh" class="day__badge" :class="[`day__badge--${slotClass(d, sh).replace('slot--', '')}`, `shift--${sh}`]">
                  {{ SHIFT_SHORT[sh] }}{{ working(d.date, sh).length }}
                </span>
              </template>
            </template>
          </button>
        </div>
      </div>

      <!-- 點選某一天看詳細名單 -->
      <div v-if="selectedDay" class="detail">
        <div class="detail__title">
          {{ selectedDay.date }}（週{{ selectedDay.weekday }}）
          <el-tag v-if="selectedDay.closed" type="info" size="small">休息日</el-tag>
        </div>
        <div class="detail__slots">
          <div v-for="sh in SHIFT_KEYS" :key="sh" class="slot" :class="[slotClass(selectedDay, sh), `shift--${sh}`]">
            <div class="slot__name">{{ SHIFT_LABEL[sh] }}<small>{{ SHIFT_TIME[sh] }}</small></div>
            <span v-for="n in working(selectedDay.date, sh)" :key="n.id" class="chip" :style="chipStyle(n)">
              <i class="chip__avatar">{{ initial(n) }}</i>{{ n.employeeName }}
            </span>
            <span v-for="n in onLeave(selectedDay.date, sh)" :key="n.id" class="chip chip--leave">
              <i class="chip__avatar">{{ initial(n) }}</i>{{ n.employeeName }}<em>請假</em>
            </span>
            <span v-if="!working(selectedDay.date, sh).length" class="slot__empty">{{ emptyText(selectedDay) }}</span>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { listScheduleOverview } from '@/api/employee'
import { listClosedDates } from '@/api/zone'

const WEEKDAYS = ['日', '一', '二', '三', '四', '五', '六']
const SHIFT_KEYS = ['AM', 'PM']
const SHIFT_LABEL = { AM: '上午班', PM: '下午班' }
const SHIFT_SHORT = { AM: '上', PM: '下' }
const SHIFT_TIME = { AM: '08:00–12:00', PM: '13:00–17:00' }

// 每位員工固定一個顏色（依 id 取色），一眼就能認出是誰；色相拉開避免相鄰員工太像
const HUES = [200, 150, 25, 280, 340, 50, 175, 235]
function chipStyle(n) {
  const h = HUES[(n.employeeId ?? 0) % HUES.length]
  return {
    '--c-bg': `hsl(${h} 70% 93%)`,
    '--c-fg': `hsl(${h} 55% 24%)`,
    '--c-dot': `hsl(${h} 55% 42%)`
  }
}
// 頭像字取名字最後一個字：王小明／王大明都姓王，取姓氏分不出來
const initial = (n) => Array.from(n.employeeName || '?').slice(-1)[0]

const viewMode = ref('week')
const loading = ref(false)
const schedules = ref([])
const closedSet = ref(new Set())
const selectedDate = ref(null)

function toDateStr(date) {
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${m}-${d}`
}
function mondayOf(date) {
  const d = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  d.setDate(d.getDate() - ((d.getDay() + 6) % 7)) // 週一為一週開頭
  return d
}
function addDays(date, n) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate() + n)
}

const today = new Date()
const thisMonday = mondayOf(today)
const thisMonth = new Date(today.getFullYear(), today.getMonth(), 1)
const weekStart = ref(thisMonday)
const monthStart = ref(thisMonth)

function makeDay(d, extra = {}) {
  const todayStr = toDateStr(new Date())
  const date = toDateStr(d)
  return {
    date,
    day: d.getDate(),
    label: `${d.getMonth() + 1}/${d.getDate()}`,
    weekday: WEEKDAYS[d.getDay()],
    today: date === todayStr,
    past: date < todayStr,
    closed: closedSet.value.has(date),
    ...extra
  }
}

const weekDays = computed(() => Array.from({ length: 7 }, (_, i) => makeDay(addDays(weekStart.value, i))))

// 月曆從「當月 1 號所在週的週一」排到「當月最後一天所在週的週日」
const monthDays = computed(() => {
  const first = monthStart.value
  const last = new Date(first.getFullYear(), first.getMonth() + 1, 0)
  const gridStart = mondayOf(first)
  const total = Math.round((mondayOf(last).getTime() - gridStart.getTime()) / 86400000) + 7
  return Array.from({ length: total }, (_, i) => {
    const d = addDays(gridStart, i)
    return makeDay(d, { inMonth: d.getMonth() === first.getMonth() })
  })
})

const selectedDay = computed(() => monthDays.value.find((d) => d.date === selectedDate.value && d.inMonth))

const titleText = computed(() => {
  if (viewMode.value === 'week') return `${weekDays.value[0].label} – ${weekDays.value[6].label}`
  return `${monthStart.value.getFullYear()} 年 ${monthStart.value.getMonth() + 1} 月`
})
const isCurrent = computed(() =>
    viewMode.value === 'week'
        ? toDateStr(weekStart.value) === toDateStr(thisMonday)
        : toDateStr(monthStart.value) === toDateStr(thisMonth)
)

// 12:00 以前開始算上午班，之後算下午班（涵蓋舊資料裡不是固定時段的班）
function shiftKeyOf(s) {
  return s.startTime.slice(0, 5) < '12:00' ? 'AM' : 'PM'
}
function inSlot(date, shift) {
  return schedules.value.filter((s) => s.workDate === date && shiftKeyOf(s) === shift)
}
const working = (date, shift) => inSlot(date, shift).filter((s) => s.status === 1)
const onLeave = (date, shift) => inSlot(date, shift).filter((s) => s.status === 0)

// 有人上班 ok；休息日或已過去的日子 none；其餘沒人上班 gap（可能漏排）
function slotClass(d, shift) {
  if (working(d.date, shift).length) return 'slot--ok'
  if (d.closed || d.past) return 'slot--none'
  return 'slot--gap'
}
function emptyText(d) {
  return d.closed ? '休息' : d.past ? '無' : '⚠ 無人上班'
}

async function fetchData() {
  loading.value = true
  try {
    const days = viewMode.value === 'week' ? weekDays.value : monthDays.value
    schedules.value = await listScheduleOverview(days[0].date, days[days.length - 1].date)
  } finally {
    loading.value = false
  }
}

function shift(delta) {
  if (viewMode.value === 'week') weekStart.value = addDays(weekStart.value, delta * 7)
  else monthStart.value = new Date(monthStart.value.getFullYear(), monthStart.value.getMonth() + delta, 1)
}
function goCurrent() {
  if (viewMode.value === 'week') weekStart.value = thisMonday
  else monthStart.value = thisMonth
}

// 切換檢視或翻頁時重新載入；月檢視預設選中今天（不在該月就不選）
watch([viewMode, weekStart, monthStart], () => {
  if (viewMode.value === 'month') {
    const todayStr = toDateStr(new Date())
    selectedDate.value = monthDays.value.some((d) => d.date === todayStr && d.inMonth) ? todayStr : null
  }
  fetchData()
})

onMounted(async () => {
  // 休息日不算漏排；休息日清單是「今天起」的，過去的日子不需要
  try {
    closedSet.value = new Set((await listClosedDates()).map((c) => c.closedDate))
  } catch {
    closedSet.value = new Set()
  }
  fetchData()
})

// 外部（例如剛修改排班）可以呼叫重新整理
defineExpose({ refresh: fetchData })
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: flex-end;
  margin: 8px 0;
}
.nav-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}
.nav-bar__title {
  display: flex;
  flex-direction: column;
  align-items: center;
  font-size: 15px;
}

/* ---------- 週檢視 ---------- */
.grid {
  border: 1px solid var(--paper-dim);
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}
.grid__head,
.grid__row {
  display: grid;
  grid-template-columns: 64px 1fr 1fr;
  gap: 6px;
  padding: 6px 8px;
}
.grid__head {
  text-align: center;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  background: #f3f4f5;
}
.grid__head small {
  display: block;
  font-size: 11px;
}
.grid__row {
  align-items: stretch;
  border-top: 1px solid var(--paper-dim);
}
.grid__row--today {
  background: #f1f8fb;
}
.grid__date {
  display: flex;
  flex-direction: column;
  gap: 2px;
  line-height: 1.2;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.grid__date strong {
  font-size: 14px;
  color: var(--ink);
}

/* ---------- 班別格（週檢視與月檢視詳細共用） ---------- */
.slot {
  display: flex;
  flex-wrap: wrap;
  align-content: center;
  gap: 4px;
  min-height: 40px;
  padding: 4px 6px;
  border-radius: 6px;
}
.slot__name {
  width: 100%;
  font-size: 13px;
  font-weight: 600;
  color: var(--ink);
}
/* 有人上班：上午班青藍、下午班靛紫，兩個班一眼分得開；紅色只留給「無人上班」警示 */
.slot--ok.shift--AM {
  background: #e8f5f7;
  box-shadow: inset 3px 0 0 #1c7c8c;
}
.slot--ok.shift--PM {
  background: #eeeffa;
  box-shadow: inset 3px 0 0 #5b5fc7;
}
.slot--gap {
  background: #fde8e8;
  box-shadow: inset 3px 0 0 #d9534f;
}
.slot--none {
  background: #f3f4f5;
}
.slot__name small {
  margin-left: 6px;
  font-weight: 400;
  font-size: 12px;
  color: var(--ink-text-secondary);
}
.head--AM {
  color: #1c7c8c;
  font-weight: 600;
}
.head--PM {
  color: #5b5fc7;
  font-weight: 600;
}

/* 員工名字：圓形頭像字 + 全名，每人固定一種顏色（--c-* 由 chipStyle 帶入） */
.chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 2px 10px 2px 3px;
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  color: var(--c-fg, #333);
  background: var(--c-bg, #eee);
  border-radius: 999px;
}
.chip__avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  font-style: normal;
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  background: var(--c-dot, #999);
  border-radius: 50%;
}
.chip--leave {
  color: #8a949b;
  background: #f0f1f2;
  border: 1px dashed #c4cacf;
  padding: 1px 9px 1px 2px;
}
.chip--leave .chip__avatar {
  background: #b5bcc2;
}
.chip--leave em {
  font-style: normal;
  font-size: 11px;
  color: #a8650a;
  background: #fff1d6;
  padding: 0 6px;
  border-radius: 999px;
}
.slot__empty {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.slot--gap .slot__empty {
  font-weight: 700;
  color: var(--danger, #d03050);
}

/* ---------- 月檢視 ---------- */
.month {
  border: 1px solid var(--paper-dim);
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}
.month__head,
.month__body {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
}
.month__head {
  text-align: center;
  padding: 6px 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  background: #f3f4f5;
}
.day {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  min-height: 74px;
  padding: 4px 2px;
  font: inherit;
  cursor: pointer;
  background: #fff;
  border: none;
  border-top: 1px solid var(--paper-dim);
  border-right: 1px solid var(--paper-dim);
}
.day:nth-child(7n) {
  border-right: none;
}
.day--out {
  opacity: 0.35;
  pointer-events: none;
}
.day--today .day__num {
  color: #fff;
  background: var(--rinse-deep, #1c7c8c);
  border-radius: 999px;
}
.day--selected {
  background: var(--el-color-primary-light-9);
  box-shadow: inset 0 0 0 2px var(--rinse, #3fb3c2);
}
.day--closed {
  background: #f3f4f5;
}
.day__num {
  min-width: 22px;
  padding: 0 4px;
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
}
.day__closed {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.day__badge {
  width: calc(100% - 6px);
  padding: 1px 0;
  font-size: 12px;
  line-height: 1.4;
  text-align: center;
  border-radius: 4px;
}
.day__badge--ok.shift--AM {
  color: #14606c;
  background: #d5eef2;
}
.day__badge--ok.shift--PM {
  color: #4448a8;
  background: #e0e2f7;
}
.day__badge--gap {
  color: #fff;
  font-weight: 700;
  background: #d9534f;
}
.day__badge--none {
  color: var(--el-text-color-secondary);
  background: #f3f4f5;
}
.detail {
  margin-top: 8px;
  padding: 12px;
  background: #fff;
  border: 1px solid var(--paper-dim);
  border-radius: 8px;
}
.detail__title {
  margin-bottom: 8px;
  font-size: 16px;
  font-weight: 700;
}
.detail__slots {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}
</style>
