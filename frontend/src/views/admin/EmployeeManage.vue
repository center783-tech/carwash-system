<template>
  <div class="page-container">
    <div class="header-row">
      <h2>員工管理</h2>
      <el-button type="primary" @click="openDialog()">新增員工</el-button>
    </div>

    <el-table :data="employees" v-loading="loading">
      <el-table-column prop="name" label="姓名" />
      <el-table-column prop="phone" label="電話" />
      <el-table-column label="可服務項目">
        <template #default="{ row }">
          <el-tag v-for="n in row.serviceItemNames" :key="n" style="margin-right: 4px">{{ n }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button link type="primary" @click="openAssignDialog(row)">指派服務項目</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增員工 -->
    <el-dialog v-model="dialogVisible" title="新增員工">
      <el-form :model="form" label-width="80px">
        <el-form-item label="姓名"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="電話"><el-input v-model="form.phone" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleCreate">儲存</el-button>
      </template>
    </el-dialog>

    <!-- 指派服務項目 -->
    <el-dialog v-model="assignDialogVisible" title="指派可服務項目">
      <el-checkbox-group v-model="selectedServiceIds">
        <el-checkbox v-for="s in allServices" :key="s.id" :value="s.id">{{ s.name }}</el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="assigning" @click="handleAssign">儲存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listEmployees, createEmployee, assignServices } from '@/api/employee'
import { listServices } from '@/api/service'

const employees = ref([])
const allServices = ref([])
const loading = ref(false)
const saving = ref(false)
const assigning = ref(false)
const dialogVisible = ref(false)
const assignDialogVisible = ref(false)
const currentEmployeeId = ref(null)
const selectedServiceIds = ref([])

const form = reactive({ name: '', phone: '' })

async function fetchData() {
  loading.value = true
  try {
    employees.value = await listEmployees()
    allServices.value = await listServices()
  } finally {
    loading.value = false
  }
}

function openDialog() {
  Object.assign(form, { name: '', phone: '' })
  dialogVisible.value = true
}

async function handleCreate() {
  saving.value = true
  try {
    await createEmployee(form)
    ElMessage.success('新增成功')
    dialogVisible.value = false
    fetchData()
  } finally {
    saving.value = false
  }
}

function openAssignDialog(row) {
  currentEmployeeId.value = row.id
  // 後端目前回傳的是服務項目名稱而非 id，實際串接時建議讓 EmployeeResponse 多回傳 serviceItemIds 方便預選
  selectedServiceIds.value = []
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
  justify-content: space-between;
  align-items: center;
}
</style>
