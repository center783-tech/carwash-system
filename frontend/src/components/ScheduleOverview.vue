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

    <!-- ========== 週檢視：人員 × 日期 ========== -->
    <div v-if="viewMode === 'week'" v-loading="loading" class="roster-wrap">
      <div class="roster">
        <!-- 日期表頭 -->
        <div class="r-row r-row--head">
          <div class="r-name r-corner">人員</div>
          <div v-for="d in weekDays" :key="d.date" class="r-cell r-day" :class="{ 'r-day--today': d.today, 'r-day--closed': d.closed }">
            <strong>{{ d.label }}</strong> 週{{ d.weekday }}
            <el-tag v-if="d.closed" type="info" size="small">休息日</el-tag>
          </div>
        </div>
        <!-- 每日統計：上班人數、總工時；有班別沒人時紅字警示 -->
        <div class="r-row r-row--stat">
          <div class="r-name r-corner">統計</div>
          <div v-for="d in weekDays" :key="d.date" class="r-cell r-stat">
            <template v-if="d.closed">—</template>
            <template v-else>
              <span><b>{{ dayStat(d.date).people }}</b> 人</span>
              <span class="r-stat__hours">{{ dayStat(d.date).hours }} 小時</span>
              <span v-for="sh in gapShifts(d)" :key="sh" class="r-stat__gap">⚠ {{ SHIFT_LABEL[sh] }}無人</span>
            </template>
          </div>
        </div>

        <template v-for="g in rosterGroups" :key="g.key">
          <!-- 職稱分組標題（只有真的有人設定職稱時才會出現） -->
          <div v-if="g.title" class="r-row r-row--group" :style="g.style">
            <div class="r-name r-group__name">{{ g.title }}<small>{{ g.rows.length }} 人</small></div>
            <div v-for="d in weekDays" :key="d.date" class="r-cell r-group__count">
              {{ d.closed ? '' : groupCount(g, d.date) }}
            </div>
          </div>

          <div v-for="emp in g.rows" :key="emp.id" class="r-row r-row--emp">
            <div class="r-name">
              <span class="r-name__text">{{ emp.name }}</span>
              <span v-if="emp.title" class="r-badge" :style="g.style">{{ emp.title }}</span>
            </div>
            <div
                v-for="d in weekDays"
                :key="d.date"
                class="r-cell r-slot"
                :class="{ 'r-slot--today': d.today, 'r-slot--closed': d.closed }"
            >
              <template v-if="shiftsOf(emp.id, d.date).length">
                <div
                    v-for="s in shiftsOf(emp.id, d.date)"
                    :key="s.id"
                    class="r-shift"
                    :class="[`shift--${shiftKeyOf(s)}`, { 'r-shift--leave': s.status === 0 }]"
                >
                  <strong>{{ s.status === 0 ? '請假' : SHIFT_LABEL[shiftKeyOf(s)] }}</strong>
                  <span>{{ s.startTime.slice(0, 5) }}–{{ s.endTime.slice(0, 5) }}</span>
                </div>
              </template>
              <span v-else-if="d.closed" class="r-off">休息日</span>
            </div>
          </div>
        </template>

        <div v-if="!loading && !rosterGroups.length" class="r-empty">這段期間沒有排班資料</div>
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
import { listScheduleOverview, listEmployees } from '@/api/employee'
import { listClosedDates } from '@/api/zone'
import { useAuthStore } from '@/store/auth'

const authStore = useAuthStore()

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

/* ---------- 週檢視：人員 × 日期 ---------- */
const employees = ref([]) // 在職員工（EMPLOYEE 角色打不到員工 API 時為空，只顯示有排班的人）

// 職稱分組配色：依分組順序輪流取色，只用在有人設定的職稱上
const GROUP_PALETTE = [
  { bg: '#fde2e4', fg: '#9b2c3a', line: '#f5b5bd' },
  { bg: '#fff3d6', fg: '#8a5a00', line: '#f0d08a' },
  { bg: '#dce8fb', fg: '#2d4f8e', line: '#aac3ee' },
  { bg: '#dff1e8', fg: '#2a6a4a', line: '#a9d8c0' },
  { bg: '#f3e1f7', fg: '#7a3a8c', line: '#d9b0e3' },
  { bg: '#e0f0f2', fg: '#1c6470', line: '#a8d3d9' }
]
const UNTITLED_STYLE = { '--g-bg': '#eceff1', '--g-fg': '#5a666e', '--g-line': '#cfd6da' }
const groupStyle = (i) => ({
  '--g-bg': GROUP_PALETTE[i % GROUP_PALETTE.length].bg,
  '--g-fg': GROUP_PALETTE[i % GROUP_PALETTE.length].fg,
  '--g-line': GROUP_PALETTE[i % GROUP_PALETTE.length].line
})

// 這一週要列出的人：在職員工 + 這週有排班的人（姓名、職稱以員工資料為準）
const rosterPeople = computed(() => {
  const map = new Map()
  employees.value.forEach((e) => map.set(e.id, { id: e.id, name: e.name, title: e.title || '' }))
  schedules.value.forEach((s) => {
    if (!map.has(s.employeeId)) {
      map.set(s.employeeId, { id: s.employeeId, name: s.employeeName, title: s.employeeTitle || '' })
    }
  })
  return [...map.values()].sort((a, b) => a.id - b.id)
})

// 有人設定職稱才分組；沒人設定職稱時整張表不出現任何職稱標題或標籤
const rosterGroups = computed(() => {
  const people = rosterPeople.value
  if (!people.length) return []
  const titles = [...new Set(people.map((p) => p.title).filter(Boolean))]
  if (!titles.length) return [{ key: '_all', title: '', style: UNTITLED_STYLE, rows: people }]
  const groups = titles.map((t, i) => ({
    key: t,
    title: t,
    style: groupStyle(i),
    rows: people.filter((p) => p.title === t)
  }))
  const rest = people.filter((p) => !p.title)
  if (rest.length) groups.push({ key: '_none', title: '未設定職稱', style: UNTITLED_STYLE, rows: rest })
  return groups
})

const shiftsOf = (empId, date) =>
  schedules.value
    .filter((s) => s.employeeId === empId && s.workDate === date)
    .sort((a, b) => a.startTime.localeCompare(b.startTime))

const toMinutes = (t) => Number(t.slice(0, 2)) * 60 + Number(t.slice(3, 5))
function dayStat(date) {
  const list = schedules.value.filter((s) => s.workDate === date && s.status === 1)
  const minutes = list.reduce((sum, s) => sum + (toMinutes(s.endTime) - toMinutes(s.startTime)), 0)
  return { people: new Set(list.map((s) => s.employeeId)).size, hours: Math.round((minutes / 60) * 10) / 10 }
}
const groupCount = (group, date) =>
  group.rows.filter((e) => shiftsOf(e.id, date).some((s) => s.status === 1)).length
// 還沒休息、還沒過去、卻沒人上的班別（可能漏排）
const gapShifts = (d) => (d.closed || d.past ? [] : SHIFT_KEYS.filter((sh) => !working(d.date, sh).length))

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
    // 員工清單只有 ADMIN/STAFF 能取；一般員工帳號就只顯示有排班的人
    const [list, emps] = await Promise.all([
      listScheduleOverview(days[0].date, days[days.length - 1].date),
      authStore.isAdmin ? listEmployees().catch(() => []) : Promise.resolve([])
    ])
    schedules.value = list
    employees.value = emps.filter((e) => e.status === 1)
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

/* ---------- 週檢視：人員 × 日期 ---------- */
.roster-wrap {
  overflow-x: auto;
  border: 1px solid var(--paper-dim);
  border-radius: 8px;
  background: #fff;
}
.roster {
  min-width: 900px;
  --cols: 150px repeat(7, minmax(104px, 1fr));
}
.r-row {
  display: grid;
  grid-template-columns: var(--cols);
  border-top: 1px solid var(--paper-dim);
}
.r-row:first-child {
  border-top: none;
}
.r-name {
  position: sticky;
  left: 0;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  background: #fff;
  border-right: 1px solid var(--paper-dim);
}
.r-cell {
  border-right: 1px solid var(--paper-dim);
}
.r-cell:last-child {
  border-right: none;
}
.r-row--head,
.r-row--stat {
  background: #f3f4f5;
}
.r-row--head .r-name,
.r-row--stat .r-name {
  background: #f3f4f5;
  font-size: 13px;
  color: var(--ink-text-secondary);
}
.r-day {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 8px 4px;
  font-size: 13px;
  text-align: center;
  color: var(--ink-text-secondary);
}
.r-day strong {
  color: var(--ink);
  font-size: 14px;
}
.r-day--today {
  background: var(--rinse-deep, #155f6c);
  color: #dff1f4;
}
.r-day--today strong {
  color: #fff;
}
.r-stat {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  align-items: baseline;
  gap: 2px 8px;
  padding: 6px 4px;
  font-size: 13px;
  color: var(--ink);
}
.r-stat__hours {
  color: var(--ink-text-secondary);
  font-size: 12px;
}
.r-stat__gap {
  width: 100%;
  text-align: center;
  font-size: 12px;
  font-weight: 700;
  color: #d9534f;
}

/* 職稱分組列 */
.r-row--group {
  background: var(--g-bg);
}
.r-row--group .r-name {
  background: var(--g-bg);
  font-weight: 700;
  color: var(--g-fg);
}
.r-group__name small {
  font-weight: 400;
  font-size: 12px;
  opacity: 0.75;
}
.r-group__count {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  color: var(--g-fg);
  border-right-color: rgba(0, 0, 0, 0.06);
}

/* 人員列 */
.r-name__text {
  font-weight: 600;
  color: var(--ink);
}
.r-badge {
  padding: 0 8px;
  font-size: 12px;
  line-height: 20px;
  white-space: nowrap;
  color: var(--g-fg);
  background: var(--g-bg);
  border: 1px solid var(--g-line);
  border-radius: 4px;
}
.r-slot {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-height: 48px;
  padding: 3px;
  justify-content: center;
}
.r-slot--today {
  background: #f1f8fb;
}
.r-slot--closed {
  background: #e4e6e8;
  align-items: center;
}
.r-off {
  font-size: 12px;
  color: #8a949b;
}
/* 班別方塊：左側色條表示上午（青藍）／下午（靛紫） */
.r-shift {
  display: flex;
  flex-direction: column;
  gap: 1px;
  padding: 3px 8px;
  line-height: 1.3;
  font-size: 12px;
  border-radius: 3px;
  border-left: 4px solid transparent;
  background: #fff;
  box-shadow: 0 0 0 1px var(--paper-dim);
}
.r-shift strong {
  font-size: 13px;
}
.r-shift span {
  color: var(--ink-text-secondary);
}
.r-shift.shift--AM {
  border-left-color: #1c7c8c;
  background: #eef8fa;
}
.r-shift.shift--PM {
  border-left-color: #5b5fc7;
  background: #f1f2fb;
}
.r-shift--leave {
  border-left-color: #c4cacf !important;
  background: #f0f1f2 !important;
  color: #8a949b;
}
.r-shift--leave strong {
  text-decoration: line-through;
}
.r-empty {
  padding: 24px;
  text-align: center;
  color: var(--ink-text-secondary);
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
