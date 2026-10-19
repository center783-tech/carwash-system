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
      <el-table-column prop="carType" label="車種" />
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">編輯</el-button>
          <el-button link type="danger" @click="handleDelete(row.id)">刪除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '編輯車輛' : '新增車輛'">
      <el-form :model="form" label-width="80px">
        <el-form-item label="車牌"><el-input v-model="form.plateNumber" /></el-form-item>
        <el-form-item label="廠牌"><el-input v-model="form.brand" /></el-form-item>
        <el-form-item label="車型"><el-input v-model="form.model" /></el-form-item>
        <el-form-item label="顏色"><el-input v-model="form.color" /></el-form-item>
        <el-form-item label="車種"><el-input v-model="form.carType" placeholder="小型車/休旅車/貨車" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">儲存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listVehicles, addVehicle, updateVehicle, deleteVehicle } from '@/api/member'
import { useAuthStore } from '@/store/auth'

const authStore = useAuthStore()
const vehicles = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref(null)

const form = reactive({ plateNumber: '', brand: '', model: '', color: '', carType: '' })

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
    Object.assign(form, row)
  } else {
    editingId.value = null
    Object.assign(form, { plateNumber: '', brand: '', model: '', color: '', carType: '' })
  }
  dialogVisible.value = true
}

async function handleSave() {
  saving.value = true
  try {
    if (editingId.value) {
      await updateVehicle(authStore.userId, editingId.value, form)
    } else {
      await addVehicle(authStore.userId, form)
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
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
