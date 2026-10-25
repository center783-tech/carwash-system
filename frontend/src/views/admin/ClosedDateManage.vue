<template>
  <div>
  <AdminNav />
  <div class="page-container">
    <h2>休息日管理</h2>
    <p class="hint">
      在日曆上一次點選多天，按「標註為休息日」就會生效，會員預約時這些日子不能選。備註可以之後在下方清單再補。
    </p>

    <div class="add-row">
      <el-date-picker
          v-model="selectedDates"
          type="dates"
          placeholder="點選多個休息日"
          value-format="YYYY-MM-DD"
          :editable="false"
          :disabled-date="isUnavailable"
          style="width: 360px"
      />
      <el-button type="primary" :disabled="!selectedDates.length" :loading="saving" @click="handleAdd">
        標註為休息日（{{ selectedDates.length }} 天）
      </el-button>
    </div>

    <el-table :data="dates" v-loading="loading" empty-text="目前沒有設定休息日">
      <el-table-column prop="closedDate" label="日期" width="140" />
      <el-table-column label="備註">
        <template #default="{ row }">
          <el-input
              v-model="row.reason"
              placeholder="需要備註再輸入（例如：國定假日）"
              maxlength="100"
              @blur="saveReason(row)"
              @keyup.enter="$event.target.blur()"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button link type="danger" @click="handleDelete(row)">取消休息</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminNav from '@/components/AdminNav.vue'
import { listClosedDates, createClosedDates, updateClosedDateReason, deleteClosedDate } from '@/api/zone'

const dates = ref([])
const loading = ref(false)
const saving = ref(false)
const selectedDates = ref([])
// 記住每筆已儲存的備註，失焦時內容沒變就不用重打 API
const savedReasons = new Map()

function toDateString(date) {
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${m}-${d}`
}

// 過去的日期、已經是休息日的日期不用再選
function isUnavailable(date) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date < today || dates.value.some((c) => c.closedDate === toDateString(date))
}

async function fetchDates() {
  loading.value = true
  try {
    dates.value = await listClosedDates()
    savedReasons.clear()
    dates.value.forEach((c) => savedReasons.set(c.id, c.reason || ''))
  } finally {
    loading.value = false
  }
}

async function handleAdd() {
  saving.value = true
  try {
    await createClosedDates(selectedDates.value)
    ElMessage.success('已標註為休息日')
    selectedDates.value = []
    await fetchDates()
  } finally {
    saving.value = false
  }
}

async function saveReason(row) {
  const reason = (row.reason || '').trim()
  if (reason === savedReasons.get(row.id)) return
  await updateClosedDateReason(row.id, reason)
  savedReasons.set(row.id, reason)
  ElMessage.success('備註已更新')
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`確定取消 ${row.closedDate} 的休息日嗎？`, '取消休息日', { type: 'warning' })
  await deleteClosedDate(row.id)
  ElMessage.success('已取消休息日')
  fetchDates()
}

onMounted(fetchDates)
</script>

<style scoped>
.page-container {
  max-width: 800px;
  margin: 24px auto;
  padding: 0 16px;
}
.hint {
  margin: 4px 0 16px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.add-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}
</style>
