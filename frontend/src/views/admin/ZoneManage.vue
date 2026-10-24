<template>
  <div>
  <AdminNav />
  <div class="page-container">
    <div class="header-row">
      <h2>車位管理</h2>
      <el-button type="primary" @click="openDialog()">新增車位區</el-button>
    </div>

    <p class="hint">每個車位區的「車位數」就是同一時段最多能接受的預約數；額滿後會員就無法再預約該區。</p>

    <el-table :data="zones" v-loading="loading">
      <el-table-column prop="name" label="區域" />
      <el-table-column prop="capacity" label="車位數" />
      <el-table-column label="狀態">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '開放預約' : '已停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">編輯</el-button>
          <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="toggleStatus(row)">
            {{ row.status === 1 ? '停用' : '啟用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '編輯車位區' : '新增車位區'">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="區域名稱" prop="name">
          <el-input v-model="form.name" placeholder="例如：A 區" maxlength="50" />
        </el-form-item>
        <el-form-item label="車位數" prop="capacity">
          <el-input-number v-model="form.capacity" :min="1" :max="200" />
        </el-form-item>
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
import { listZones, createZone, updateZone } from '@/api/zone'

const zones = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref(null)
const formRef = ref(null)

const form = reactive({ name: '', capacity: 1 })
const rules = {
  name: [{ required: true, message: '請輸入區域名稱', trigger: 'blur' }],
  capacity: [{ required: true, message: '請輸入車位數', trigger: 'change' }]
}

async function fetchZones() {
  loading.value = true
  try {
    zones.value = await listZones()
  } finally {
    loading.value = false
  }
}

function openDialog(row) {
  editingId.value = row ? row.id : null
  form.name = row ? row.name : ''
  form.capacity = row ? row.capacity : 1
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

async function handleSave() {
  if (!(await formRef.value.validate().catch(() => false))) return
  saving.value = true
  try {
    if (editingId.value) {
      const current = zones.value.find((z) => z.id === editingId.value)
      await updateZone(editingId.value, { name: form.name, capacity: form.capacity, status: current?.status })
    } else {
      await createZone({ name: form.name, capacity: form.capacity, status: 1 })
    }
    ElMessage.success('儲存成功')
    dialogVisible.value = false
    fetchZones()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {
  const enabling = row.status !== 1
  if (!enabling) {
    await ElMessageBox.confirm(`停用後會員將無法再預約「${row.name}」，既有預約不受影響。確定停用嗎？`, '停用車位區', {
      type: 'warning'
    })
  }
  await updateZone(row.id, { name: row.name, capacity: row.capacity, status: enabling ? 1 : 0 })
  ElMessage.success(enabling ? '已啟用' : '已停用')
  fetchZones()
}

onMounted(fetchZones)
</script>

<style scoped>
.page-container {
  max-width: 800px;
  margin: 24px auto;
  padding: 0 16px;
}
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.hint {
  margin: 4px 0 16px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
</style>
