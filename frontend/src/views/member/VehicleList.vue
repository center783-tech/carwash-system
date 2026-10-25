<template>
  <div class="page-container">
    <div class="header-row">
      <h2>我的車輛</h2>
      <el-button type="primary" @click="openDialog()">新增車輛</el-button>
    </div>

    <el-table :data="vehicles" v-loading="loading">
      <el-table-column prop="plateNumber" label="車牌" />
      <el-table-column prop="brand" label="廠牌" />
      <el-table-column prop="model" label="車型" />
      <el-table-column prop="color" label="顏色" />
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">編輯</el-button>
          <el-button link type="danger" @click="handleDelete(row.id)">刪除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '編輯車輛' : '新增車輛'">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="車牌" prop="plateNumber">
          <el-radio-group v-model="plateType" @change="onPlateTypeChange">
            <el-radio-button value="NEW">新式車牌</el-radio-button>
            <el-radio-button value="OLD">舊式車牌</el-radio-button>
          </el-radio-group>
          <el-input
              v-model="form.plateNumber"
              :placeholder="plateType === 'NEW' ? 'ABC-1234' : 'AB-1234 / 1234-AB'"
              maxlength="8"
              class="manual-input"
              @input="onPlateInput"
          />
          <span class="hint">
            {{ plateType === 'NEW' ? '3 碼英文 + 4 碼數字，例如 ABC-1234' : '例如 AB-1234、1234-AB、ABC-123、123-ABC' }}，連字號會自動補上
          </span>
        </el-form-item>
        <el-form-item label="廠牌" prop="brand">
          <el-select v-model="brandChoice" placeholder="請選擇廠牌" :filterable="!isTouchDevice"style="width: 100%" @change="onBrandChange">
            <el-option v-for="b in brandOptions" :key="b" :label="b" :value="b" />
            <el-option label="其他（手動輸入）" :value="OTHER" />
          </el-select>
          <el-input v-if="brandChoice === OTHER" v-model="customBrand" placeholder="請輸入廠牌" class="manual-input" />
        </el-form-item>
        <el-form-item label="車型" prop="model">
          <el-select
              v-model="modelChoice"
              :placeholder="brandChoice ? '請選擇車型' : '請先選擇廠牌'"
              :disabled="!brandChoice"
              :filterable="!isTouchDevice"
              style="width: 100%"
          >
            <el-option v-for="m in modelOptions" :key="m" :label="m" :value="m" />
            <el-option label="其他（手動輸入）" :value="OTHER" />
          </el-select>
          <el-input v-if="modelChoice === OTHER" v-model="customModel" placeholder="請輸入車型" class="manual-input" />
        </el-form-item>
        <el-form-item label="顏色"><el-input v-model="form.color" placeholder="例如：白色、珍珠白、消光黑" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">儲存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

// 觸控裝置關閉下拉搜尋，避免點選單時彈出鍵盤（想自己輸入可選「其他（手動輸入）」）
const isTouchDevice = window.matchMedia('(pointer: coarse)').matches
import { listVehicles, addVehicle, updateVehicle, deleteVehicle } from '@/api/member'
import { useAuthStore } from '@/store/auth'
import { vehicleCatalog, brandOptions, OTHER } from '@/data/vehicleCatalog'

const authStore = useAuthStore()
const vehicles = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref(null)

const formRef = ref(null)
const form = reactive({ plateNumber: '', color: '' })

// 廠牌／車型：先從清單選，沒有的才選「其他」手動輸入
const brandChoice = ref('')
const customBrand = ref('')
const modelChoice = ref('')
const customModel = ref('')

const modelOptions = computed(() => vehicleCatalog[brandChoice.value] || [])
const finalBrand = computed(() => (brandChoice.value === OTHER ? customBrand.value.trim() : brandChoice.value))
const finalModel = computed(() => (modelChoice.value === OTHER ? customModel.value.trim() : modelChoice.value))

// 車牌類型：新式 ABC-1234（3 碼英文 + 4 碼數字）；舊式如 AB-1234、1234-AB、ABC-123、123-ABC
const plateType = ref('NEW')
const NEW_PLATE = /^[A-Z]{3}-\d{4}$/
const OLD_PLATE = /^([A-Z]{2}-\d{3,4}|[A-Z]{3}-\d{3}|\d{3,4}-[A-Z]{2,3})$/

const plateValidator = (_r, value, cb) => {
  if (!value) return cb(new Error('請輸入車牌'))
  if (plateType.value === 'NEW') {
    return NEW_PLATE.test(value) ? cb() : cb(new Error('新式車牌須為 3 碼英文 + 4 碼數字，例如 ABC-1234'))
  }
  return OLD_PLATE.test(value) ? cb() : cb(new Error('舊式車牌格式不正確，例如 AB-1234、1234-AB、ABC-123'))
}

const rules = {
  plateNumber: [{ validator: plateValidator, trigger: 'blur' }],
  brand: [{ validator: (_r, _v, cb) => (finalBrand.value ? cb() : cb(new Error('請選擇或輸入廠牌'))), trigger: 'change' }],
  model: [{ validator: (_r, _v, cb) => (finalModel.value ? cb() : cb(new Error('請選擇或輸入車型'))), trigger: 'change' }]
}

// 輸入時自動轉大寫、去掉非英數字，並自動補上連字號，不用手動打 -
// 新式：英文滿 3 碼就補上 -（ABC- 後面接 4 碼數字）
// 舊式：英文與數字交界處補上 -（英文在前或數字在前皆可）
function onPlateInput(value) {
  const raw = value.toUpperCase().replace(/[^A-Z0-9]/g, '')
  if (plateType.value === 'NEW') {
    const letters = raw.match(/^[A-Z]{0,3}/)[0]
    const digits = letters.length === 3 ? raw.slice(3).replace(/\D/g, '').slice(0, 4) : ''
    form.plateNumber = letters.length === 3 ? `${letters}-${digits}` : letters
    return
  }
  const m = raw.match(/^([A-Z]{1,3})(\d{0,4})/) || raw.match(/^(\d{1,4})([A-Z]{0,3})/)
  if (!m) {
    form.plateNumber = ''
    return
  }
  form.plateNumber = m[2] ? `${m[1]}-${m[2]}` : m[1]
}

// 切換新式／舊式時清空已輸入的車牌，避免格式混在一起
function onPlateTypeChange() {
  form.plateNumber = ''
  formRef.value?.clearValidate('plateNumber')
}

function onBrandChange() {
  modelChoice.value = ''
  customModel.value = ''
}

// 編輯時把已存的廠牌／車型還原成下拉選項；不在清單內的視為手動輸入
function restoreChoices(brand, model) {
  if (brand && vehicleCatalog[brand]) {
    brandChoice.value = brand
    customBrand.value = ''
  } else if (brand) {
    brandChoice.value = OTHER
    customBrand.value = brand
  } else {
    brandChoice.value = ''
    customBrand.value = ''
  }
  if (model && vehicleCatalog[brand]?.includes(model)) {
    modelChoice.value = model
    customModel.value = ''
  } else if (model) {
    modelChoice.value = OTHER
    customModel.value = model
  } else {
    modelChoice.value = ''
    customModel.value = ''
  }
}

async function fetchVehicles() {
  loading.value = true
  try {
    vehicles.value = await listVehicles(authStore.userId)
  } finally {
    loading.value = false
  }
}

function openDialog(row) {
  if (row) {
    editingId.value = row.id
    Object.assign(form, { plateNumber: row.plateNumber, color: row.color || '' })
    plateType.value = NEW_PLATE.test(row.plateNumber) ? 'NEW' : 'OLD'
    restoreChoices(row.brand, row.model)
  } else {
    editingId.value = null
    Object.assign(form, { plateNumber: '', color: '' })
    plateType.value = 'NEW'
    restoreChoices('', '')
  }
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

async function handleSave() {
  if (!(await formRef.value.validate().catch(() => false))) return
  const payload = { plateNumber: form.plateNumber, brand: finalBrand.value, model: finalModel.value, color: form.color.trim() }
  saving.value = true
  try {
    if (editingId.value) {
      await updateVehicle(authStore.userId, editingId.value, payload)
    } else {
      await addVehicle(authStore.userId, payload)
    }
    ElMessage.success('儲存成功')
    dialogVisible.value = false
    fetchVehicles()
  } finally {
    saving.value = false
  }
}

async function handleDelete(vehicleId) {
  await ElMessageBox.confirm('確定要刪除這輛車嗎？', '刪除車輛', { type: 'warning' })
  await deleteVehicle(authStore.userId, vehicleId)
  ElMessage.success('已刪除')
  fetchVehicles()
}

onMounted(fetchVehicles)
</script>

<style scoped>
.page-container {
  max-width: 900px;
  margin: 24px auto;
  padding: 0 16px;
}
.hint {
  display: block;
  width: 100%;
  font-size: 12px;
  line-height: 1.5;
  color: var(--el-text-color-secondary);
}
.manual-input {
  margin-top: 8px;
}
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
