<template>
  <div>
  <AdminNav />
  <div class="page-container">
    <div class="header-row">
      <h2>服務項目管理</h2>
      <el-button type="primary" @click="openDialog()">新增服務項目</el-button>
    </div>

    <el-table :data="services" v-loading="loading">
      <el-table-column prop="name" label="名稱" />
      <el-table-column prop="category" label="分類" />
      <el-table-column prop="price" label="價格" />
      <el-table-column prop="durationMinutes" label="時長(分)" />
      <el-table-column label="狀態">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '上架中' : '已下架' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">編輯</el-button>
          <el-button link type="danger" @click="handleDelete(row.id)">下架</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '編輯服務項目' : '新增服務項目'">
      <el-form :model="form" label-width="90px">
        <el-form-item label="名稱"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="說明"><el-input v-model="form.description" type="textarea" /></el-form-item>
        <el-form-item label="分類"><el-input v-model="form.category" placeholder="洗車/美容/保養" /></el-form-item>
        <el-form-item label="價格"><el-input-number v-model="form.price" :min="0" /></el-form-item>
        <el-form-item label="時長(分)"><el-input-number v-model="form.durationMinutes" :min="1" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">儲存</el-button>
      </template>
    </el-dialog>
  </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminNav from '@/components/AdminNav.vue'
import { listServices, createService, updateService, deleteService } from '@/api/service'

const services = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref(null)

const form = reactive({ name: '', description: '', category: '', price: 0, durationMinutes: 30 })

async function fetchServices() {
  loading.value = true
  try {
    services.value = await listServices()
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
    Object.assign(form, { name: '', description: '', category: '', price: 0, durationMinutes: 30 })
  }
  dialogVisible.value = true
}

async function handleSave() {
  saving.value = true
  try {
    if (editingId.value) {
      await updateService(editingId.value, form)
    } else {
      await createService(form)
    }
    ElMessage.success('儲存成功')
    dialogVisible.value = false
    fetchServices()
  } finally {
    saving.value = false
  }
}

async function handleDelete(id) {
  await ElMessageBox.confirm('確定要下架此服務項目嗎？', '下架服務項目', { type: 'warning' })
  await deleteService(id)
  ElMessage.success('已下架')
  fetchServices()
}

onMounted(fetchServices)
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
