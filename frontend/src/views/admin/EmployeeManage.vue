<template>
  <div>
  <AdminNav />
  <div class="page-container">
    <h2>員工管理</h2>

    <el-tabs v-model="activeTab" class="main-tabs">
      <el-tab-pane label="員工清單" name="employees">
        <div class="header-row">
          <el-button type="primary" @click="openDialog()">新增員工</el-button>
        </div>

        <p class="hint">停用的員工不會出現在可指派清單，且已排的班會全部刪除；既有預約不受影響，可隨時重新啟用（需重新排班）。</p>

        <el-table :data="employees" v-loading="loading" empty-text="尚未新增員工">
          <el-table-column prop="name" label="姓名" class-name="employee-name" />
          <el-table-column prop="phone" label="電話" />
          <el-table-column label="職稱">
            <template #default="{ row }">
              <el-tag v-if="row.title" effect="plain">{{ row.title }}</el-tag>
              <span v-else class="muted">—</span>
            </template>
          </el-table-column>
          <el-table-column label="狀態">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '在職' : '已停用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="可服務項目">
            <template #default="{ row }">
              <el-tag v-for="n in row.serviceItemNames" :key="n" class="service-tag">{{ n }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="240">
            <template #default="{ row }">
              <div class="actions">
                <el-button link type="primary" @click="openDialog(row)">編輯</el-button>
                <el-button link type="primary" @click="openAssignDialog(row)">指派服務</el-button>
                <el-button link type="primary" @click="openScheduleDialog(row)">排班</el-button>
                <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="toggleStatus(row)">
                  {{ row.status === 1 ? '停用' : '啟用' }}
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="總班表" name="overview" lazy>
        <ScheduleOverview ref="overviewRef" />
      </el-tab-pane>
    </el-tabs>

    <!-- 新增 / 編輯員工 -->
    <el-dialog v-model="dialogVisible" :title="editingId ? '編輯員工' : '新增員工'">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" maxlength="50" placeholder="請輸入員工姓名" />
        </el-form-item>
        <el-form-item label="電話" prop="phone">
          <el-input v-model="form.phone" maxlength="10" inputmode="numeric" placeholder="09 開頭共 10 碼（選填）" />
        </el-form-item>
        <el-form-item label="職稱">
          <el-select
              v-model="form.title"
              filterable
              allow-create
              clearable
              default-first-option
              placeholder="選擇或輸入職稱（選填）"
              style="width: 100%"
          >
            <el-option v-for="t in titleOptions" :key="t" :label="t" :value="t" />
          </el-select>
          <div class="form-hint">有填職稱的員工，總班表會依職稱分組；沒人使用的職稱不會出現在班表上。</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">儲存</el-button>
      </template>
    </el-dialog>

    <!-- 指派服務項目 -->
    <el-dialog v-model="assignDialogVisible" title="指派可服務項目">
      <el-checkbox-group v-model="selectedServiceIds" class="check-list">
        <el-checkbox v-for="s in allServices" :key="s.id" :value="s.id">{{ s.name }}</el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="assigning" @click="handleAssign">儲存</el-button>
      </template>
    </el-dialog>

    <!-- 排班 -->
    <el-dialog v-model="scheduleDialogVisible" :title="`${scheduleEmployee?.name || ''} 的排班`" width="640px">
      <div class="schedule-form">
        <label>1. 選擇上班日（可多選）</label>
        <el-date-picker
            v-model="scheduleDates"
            type="dates"
            placeholder="點選多個上班日"
            value-format="YYYY-MM-DD"
            :editable="false"
            :disabled-date="isPastDate"
            style="width: 100%"
        />
        <label>2. 選擇班別（可複選，兩個都選就是全天）</label>
        <div class="shift-row">
          <button
              v-for="sh in SHIFTS"
              :key="sh.key"
              type="button"
              class="shift-card"
              :class="{ 'shift-card--on': scheduleShifts.includes(sh.key) }"
              @click="toggleShift(sh.key)"
          >
            <strong>{{ sh.label }}</strong>
            <span>{{ sh.start }} – {{ sh.end }}</span>
          </button>
        </div>
        <el-button
            type="primary"
            :disabled="!scheduleDates.length || !scheduleShifts.length"
            :loading="scheduleSaving"
            @click="handleAddSchedules"
        >
          新增排班（{{ scheduleDates.length }} 天）
        </el-button>
      </div>

      <div class="week-bar">
        <el-button size="small" :disabled="isCurrentWeek" @click="shiftWeek(-1)">‹ 上一週</el-button>
        <div class="week-bar__title">
          <strong>{{ weekRangeText }}</strong>
          <el-button v-if="!isCurrentWeek" link type="primary" size="small" @click="goThisWeek">回到本週</el-button>
        </div>
        <el-button size="small" @click="shiftWeek(1)">下一週 ›</el-button>
      </div>

      <div v-loading="scheduleLoading" class="week-grid">
        <div class="week-grid__head">
          <span>日期</span>
          <span v-for="sh in SHIFTS" :key="sh.key">{{ sh.label }}</span>
        </div>
        <div v-for="d in weekDays" :key="d.date" class="week-grid__row" :class="{ 'week-grid__row--past': d.past }">
          <div class="week-grid__date">
            <strong>{{ d.label }}</strong>
            <span>週{{ d.weekday }}{{ d.closed ? '・休息日' : '' }}</span>
          </div>
          <template v-for="sh in SHIFTS" :key="sh.key">
            <el-dropdown v-if="cellOf(d.date, sh.key)" trigger="click" class="cell-wrap">
              <button type="button" class="cell" :class="cellOf(d.date, sh.key).status === 0 ? 'cell--leave' : 'cell--on'">
                {{ cellOf(d.date, sh.key).status === 0 ? '請假' : '上班' }}
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="cellOf(d.date, sh.key).status === 1" @click="handleLeave(cellOf(d.date, sh.key))">請假</el-dropdown-item>
                  <el-dropdown-item v-else @click="handleCancelLeave(cellOf(d.date, sh.key))">取消請假</el-dropdown-item>
                  <el-dropdown-item @click="handleDeleteSchedule(cellOf(d.date, sh.key))">刪除這個班</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <button v-else type="button" class="cell cell--empty" :disabled="d.past || d.closed" @click="quickAdd(d.date, sh)">
              {{ d.closed ? '休' : d.past ? '—' : '＋' }}
            </button>
          </template>
          <div v-for="c in customOf(d.date)" :key="c.id" class="week-grid__custom">
            自訂 {{ c.startTime.slice(0, 5) }}–{{ c.endTime.slice(0, 5) }}{{ c.status === 0 ? '（請假）' : '' }}
            <el-button link type="danger" size="small" @click="handleDeleteSchedule(c)">刪除</el-button>
          </div>
        </div>
      </div>
      <p class="hint week-hint">點「＋」可直接排該班；點「上班」或「請假」可請假或刪除。休息日（休）不能排班。</p>
    </el-dialog>
  </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminNav from '@/components/AdminNav.vue'
import ScheduleOverview from '@/components/ScheduleOverview.vue'
import {
  listEmployees, createEmployee, updateEmployee, updateEmployeeStatus, assignServices,
  listSchedules, createSchedules, markLeave, cancelLeave, deleteSchedule
} from '@/api/employee'
import { listServices } from '@/api/service'
import { listClosedDates } from '@/api/zone'

const activeTab = ref('employees')
const overviewRef = ref(null)
const employees = ref([])
const allServices = ref([])
const loading = ref(false)
const saving = ref(false)
const assigning = ref(false)
const dialogVisible = ref(false)
const assignDialogVisible = ref(false)
const editingId = ref(null)
const currentEmployeeId = ref(null)
const selectedServiceIds = ref([])
const formRef = ref(null)

const form = reactive({ name: '', phone: '', title: '' })

// 職稱選項：常用職稱 + 目前員工已經用過的職稱
const titleOptions = computed(() => {
  const set = new Set(['店長', '資深技師', '技師', '助理', '實習生'])
  employees.value.forEach((e) => e.title && set.add(e.title))
  return [...set]
})
const rules = {
  name: [{ required: true, message: '請輸入員工姓名', trigger: 'blur' }],
  phone: [{ pattern: /^(09\d{8})?$/, message: '電話格式錯誤，請輸入 09 開頭共 10 碼的手機號碼', trigger: 'blur' }]
}

async function fetchData() {
  loading.value = true
  try {
    employees.value = await listEmployees()
    allServices.value = await listServices()
  } finally {
    loading.value = false
  }
}

function openDialog(row) {
  editingId.value = row ? row.id : null
  Object.assign(form, {
    name: row ? row.name : '',
    phone: row ? row.phone || '' : '',
    title: row ? row.title || '' : ''
  })
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

async function handleSave() {
  if (!(await formRef.value.validate().catch(() => false))) return
  saving.value = true
  try {
    const data = { name: form.name.trim(), phone: form.phone.trim(), title: (form.title || '').trim() }
    if (editingId.value) await updateEmployee(editingId.value, data)
    else await createEmployee(data)
    ElMessage.success('儲存成功')
    dialogVisible.value = false
    fetchData()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {
  const enabling = row.status !== 1
  if (!enabling) {
    await ElMessageBox.confirm(`停用後「${row.name}」將不會出現在可指派清單，且他所有已排的班會全部刪除（重新啟用也不會恢復），既有預約不受影響。確定停用嗎？`, '停用員工', {
      type: 'warning'
    })
  }
  await updateEmployeeStatus(row.id, enabling ? 1 : 0)
  ElMessage.success(enabling ? '已啟用' : '已停用，已排的班已全部刪除')
  fetchData()
}

function openAssignDialog(row) {
  currentEmployeeId.value = row.id
  selectedServiceIds.value = [...(row.serviceItemIds || [])]
  assignDialogVisible.value = true
}

async function handleAssign() {
  assigning.value = true
  try {
    await assignServices(currentEmployeeId.value, selectedServiceIds.value)
    ElMessage.success('指派成功')
    assignDialogVisible.value = false
    fetchData()
  } finally {
    assigning.value = false
  }
}

// ---------- 排班 ----------
const scheduleDialogVisible = ref(false)
const scheduleEmployee = ref(null)
const scheduleDates = ref([])
// 班別固定兩種：上午班 08:00–12:00、下午班 13:00–17:00
const SHIFTS = [
  { key: 'AM', label: '上午班', start: '08:00', end: '12:00' },
  { key: 'PM', label: '下午班', start: '13:00', end: '17:00' }
]
const scheduleShifts = ref([])
const scheduleSaving = ref(false)
const scheduleLoading = ref(false)
const schedules = ref([])

const WEEKDAYS = ['日', '一', '二', '三', '四', '五', '六']
// 'YYYY-MM-DD' 轉星期；用本地時間建立日期，避免 UTC 造成差一天
function weekday(dateStr) {
  const [y, m, d] = dateStr.split('-').map(Number)
  return `週${WEEKDAYS[new Date(y, m - 1, d).getDay()]}`
}

function toggleShift(key) {
  const i = scheduleShifts.value.indexOf(key)
  if (i >= 0) scheduleShifts.value.splice(i, 1)
  else scheduleShifts.value.push(key)
}

// ---------- 週曆 ----------
function toDateStr(date) {
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const dd = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${m}-${dd}`
}
function mondayOf(date) {
  const d = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  d.setDate(d.getDate() - ((d.getDay() + 6) % 7)) // 週一為一週開頭
  return d
}
const thisMonday = mondayOf(new Date())
const weekStart = ref(thisMonday)

const isCurrentWeek = computed(() => toDateStr(weekStart.value) === toDateStr(thisMonday))

const weekDays = computed(() => {
  const todayStr = toDateStr(new Date())
  return Array.from({ length: 7 }, (_, i) => {
    const d = new Date(weekStart.value.getFullYear(), weekStart.value.getMonth(), weekStart.value.getDate() + i)
    const date = toDateStr(d)
    return {
      date,
      label: `${d.getMonth() + 1}/${d.getDate()}`,
      weekday: WEEKDAYS[d.getDay()],
      past: date < todayStr,
      closed: closedDates.value.has(date)
    }
  })
})

const weekRangeText = computed(() => {
  const first = weekDays.value[0]
  const last = weekDays.value[6]
  return `${first.label} – ${last.label}`
})

// 班表只載入今天起的資料，所以不能往前翻到本週之前
function shiftWeek(delta) {
  const d = new Date(weekStart.value)
  d.setDate(d.getDate() + delta * 7)
  if (d < thisMonday) return
  weekStart.value = d
}
function goThisWeek() {
  weekStart.value = thisMonday
}

// 依上班時間判斷班別；cellOf 取得某天某班別的排班，customOf 取得不是這兩種班別的舊資料
function shiftOf(s) {
  return SHIFTS.find((sh) => sh.start === s.startTime.slice(0, 5) && sh.end === s.endTime.slice(0, 5))
}
function cellOf(date, key) {
  return schedules.value.find((s) => s.workDate === date && shiftOf(s)?.key === key)
}
function customOf(date) {
  return schedules.value.filter((s) => s.workDate === date && !shiftOf(s))
}

// 在週曆上點「＋」直接排班
async function quickAdd(date, sh) {
  await createSchedules(scheduleEmployee.value.id, [date], sh.start, sh.end)
  ElMessage.success(`已排 ${date} ${sh.label}`)
  fetchSchedules()
}

// 休息日（後台「休息日」設定）不能排班
const closedDates = ref(new Set())
async function loadClosedDates() {
  try {
    closedDates.value = new Set((await listClosedDates()).map((c) => c.closedDate))
  } catch {
    closedDates.value = new Set()
  }
}

function isPastDate(date) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date < today || closedDates.value.has(toDateStr(date))
}

async function fetchSchedules() {
  scheduleLoading.value = true
  try {
    schedules.value = await listSchedules(scheduleEmployee.value.id)
    overviewRef.value?.refresh()
  } finally {
    scheduleLoading.value = false
  }
}

function openScheduleDialog(row) {
  scheduleEmployee.value = row
  scheduleDates.value = []
  scheduleShifts.value = []
  schedules.value = []
  weekStart.value = thisMonday
  scheduleDialogVisible.value = true
  loadClosedDates()
  fetchSchedules()
}

async function handleAddSchedules() {
  scheduleSaving.value = true
  try {
    // 每個班別各送一次（上下班時間不同）
    let created = 0
    let skipped = 0
    for (const sh of SHIFTS.filter((x) => scheduleShifts.value.includes(x.key))) {
      const result = await createSchedules(scheduleEmployee.value.id, scheduleDates.value, sh.start, sh.end)
      created += result.length
      skipped += scheduleDates.value.length - result.length
    }
    ElMessage.success(skipped > 0 ? `已新增 ${created} 筆，${skipped} 筆已有相同班別而略過` : `已新增 ${created} 筆排班`)
    scheduleDates.value = []
    scheduleShifts.value = []
    fetchSchedules()
  } finally {
    scheduleSaving.value = false
  }
}

async function handleLeave(s) {
  await markLeave(scheduleEmployee.value.id, s.id)
  ElMessage.success('已標記請假')
  fetchSchedules()
}

async function handleCancelLeave(s) {
  await cancelLeave(scheduleEmployee.value.id, s.id)
  ElMessage.success('已取消請假')
  fetchSchedules()
}

async function handleDeleteSchedule(s) {
  await ElMessageBox.confirm(`確定刪除 ${s.workDate} 的排班嗎？`, '刪除排班', { type: 'warning' })
  await deleteSchedule(scheduleEmployee.value.id, s.id)
  ElMessage.success('排班已刪除')
  fetchSchedules()
}

onMounted(fetchData)
</script>

<style scoped>
.page-container {
  max-width: 1000px;
  margin: 24px auto;
  padding: 0 16px;
}
.header-row {
  display: flex;
  justify-content: flex-end;
  align-items: center;
}
.muted {
  color: var(--ink-text-secondary);
}
.form-hint {
  width: 100%;
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.4;
  color: var(--ink-text-secondary);
}
.hint {
  margin: 4px 0 16px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.service-tag {
  margin: 0 4px 4px 0;
}
.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 8px;
}
.actions .el-button {
  margin: 0;
}
.check-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.schedule-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.schedule-form label {
  font-size: 13px;
  color: var(--ink-text-secondary);
}
.schedule-form .el-button {
  align-self: flex-start;
  margin-top: 4px;
}
/* 左邊上午班、右邊下午班 */
.shift-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.shift-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 6px 8px;
  font: inherit;
  color: var(--ink);
  cursor: pointer;
  background: #fff;
  border: 1.5px solid var(--paper-dim);
  border-radius: 8px;
}
.shift-card strong {
  font-size: 14px;
}
.shift-card span {
  font-size: 12px;
  color: var(--ink-text-secondary);
}
.shift-card--on {
  border-color: var(--rinse);
  background: var(--el-color-primary-light-9);
}
.schedule-title {
  margin: 20px 0 8px;
}
.schedule-list {
  min-height: 60px;
  max-height: 40vh;
  overflow-y: auto;
}
.week-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin: 16px 0 8px;
}
.week-bar__title {
  display: flex;
  flex-direction: column;
  align-items: center;
  font-size: 15px;
}
.week-grid {
  border: 1px solid var(--paper-dim);
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}
.week-grid__head,
.week-grid__row {
  display: grid;
  grid-template-columns: 64px 1fr 1fr;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
}
.week-grid__head {
  padding-top: 6px;
  padding-bottom: 6px;
  font-size: 13px;
  text-align: center;
  color: var(--el-text-color-secondary);
  background: #f3f4f5;
}
.week-grid__row {
  border-top: 1px solid var(--paper-dim);
}
.week-grid__row--past {
  opacity: 0.5;
}
.week-grid__date {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.week-grid__date strong {
  font-size: 14px;
  color: var(--ink);
}
.cell-wrap {
  width: 100%;
}
.cell {
  width: 100%;
  height: 30px;
  font: inherit;
  font-size: 13px;
  cursor: pointer;
  border: none;
  border-radius: 6px;
}
.cell--on {
  color: #1d7a3c;
  background: #e3f4e8;
}
.cell--leave {
  color: #a8650a;
  background: #fff1d6;
}
.cell--empty {
  color: var(--el-text-color-secondary);
  background: #f3f4f5;
}
.cell--empty:disabled {
  cursor: default;
}
.week-grid__custom {
  grid-column: 1 / -1;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.week-hint {
  margin: 8px 0 0;
}
</style>
