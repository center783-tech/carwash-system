<template>
  <div>
  <AdminNav />
  <div class="page-container">
    <div class="header-row">
      <h2>服務項目管理</h2>
      <div class="header-actions">
        <el-button @click="openDialog(null, true)">新增組合優惠</el-button>
        <el-button type="primary" @click="openDialog()">新增服務項目</el-button>
      </div>
    </div>

    <div class="filter-row">
      <el-input v-model="keyword" placeholder="搜尋名稱" clearable style="width: 200px" />
      <el-select v-model="categoryFilter" placeholder="全部分類" clearable style="width: 140px">
        <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
      </el-select>
      <el-radio-group v-model="typeFilter">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="single">單項</el-radio-button>
        <el-radio-button value="bundle">組合優惠</el-radio-button>
      </el-radio-group>
      <el-checkbox v-model="showOffline">顯示已下架</el-checkbox>
    </div>

    <el-table :data="filtered" v-loading="loading" row-key="id">
      <el-table-column label="名稱" min-width="200">
        <template #default="{ row }">
          <div class="name-cell">
            <strong>{{ row.name }}</strong>
            <el-tag v-if="row.bundle" size="small" type="danger" effect="dark">組合</el-tag>
          </div>
          <div v-if="row.bundle" class="bundle-contents">{{ bundleNames(row) }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="category" label="分類" width="90" />
      <el-table-column label="價格" width="150">
        <template #default="{ row }">
          <template v-if="row.bundle && row.originalPrice">
            <span class="price-old">NT$ {{ row.originalPrice }}</span>
            <strong class="price-now">NT$ {{ row.price }}</strong>
          </template>
          <template v-else>NT$ {{ row.price }}</template>
        </template>
      </el-table-column>
      <el-table-column prop="durationMinutes" label="時長(分)" width="90" />
      <el-table-column label="上架" width="80">
        <template #default="{ row }">
          <el-switch
            :model-value="row.status === 1"
            :loading="togglingId === row.id"
            @change="(v) => toggleStatus(row, v)"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">編輯</el-button>
          <el-button link type="primary" @click="duplicate(row)">複製</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="類型">
          <el-radio-group v-model="form.bundle" :disabled="!!editingId">
            <el-radio-button :value="false">單項服務</el-radio-button>
            <el-radio-button :value="true">組合優惠</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="名稱"><el-input v-model="form.name" maxlength="50" show-word-limit /></el-form-item>
        <el-form-item label="說明">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="255" show-word-limit />
        </el-form-item>
        <el-form-item label="分類">
          <el-select v-model="form.category" filterable allow-create default-first-option placeholder="選擇或輸入新分類" style="width: 100%">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>

        <el-form-item v-if="form.bundle" label="包含項目">
          <el-select v-model="form.bundleItemIds" multiple filterable placeholder="選 2 項以上單項服務" style="width: 100%">
            <el-option
              v-for="s in singleServices"
              :key="s.id"
              :label="`${s.name}（NT$${s.price}・${s.durationMinutes}分）`"
              :value="s.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item :label="form.bundle ? '組合優惠價' : '價格'">
          <el-input-number v-model="form.price" :min="0" :step="50" />
          <el-button v-if="form.bundle && bundleOriginal > 0" link type="primary" style="margin-left: 8px" @click="applyDiscountRate(0.9)">
            9 折
          </el-button>
          <el-button v-if="form.bundle && bundleOriginal > 0" link type="primary" @click="applyDiscountRate(0.8)">8 折</el-button>
        </el-form-item>

        <el-form-item v-if="form.bundle" label="試算">
          <div class="bundle-calc">
            <span>原價 NT$ {{ bundleOriginal }}</span>
            <span v-if="bundleSaving > 0" class="save">會員省 NT$ {{ bundleSaving }}</span>
            <span v-else-if="bundleOriginal > 0" class="warn">優惠價未低於原價，會員看不到優惠</span>
          </div>
        </el-form-item>

        <el-form-item label="時長(分)">
          <template v-if="form.bundle">
            <strong>{{ bundleDuration }}</strong>
            <span class="hint">（自動加總各項目時長）</span>
          </template>
          <el-input-number v-else v-model="form.durationMinutes" :min="1" :step="10" />
        </el-form-item>
        <el-form-item label="上架">
          <el-switch v-model="form.statusOn" />
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import AdminNav from '@/components/AdminNav.vue'
import { listAllServices, createService, updateService } from '@/api/service'

const services = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref(null)
const togglingId = ref(null)

const keyword = ref('')
const categoryFilter = ref('')
const typeFilter = ref('')
const showOffline = ref(true)

const blankForm = () => ({
  name: '',
  description: '',
  category: '',
  price: 0,
  durationMinutes: 30,
  bundle: false,
  bundleItemIds: [],
  statusOn: true
})
const form = reactive(blankForm())

const categories = computed(() => {
  const set = new Set(['洗車', '美容', '保養'])
  services.value.forEach((s) => s.category && set.add(s.category))
  return [...set]
})

const filtered = computed(() =>
  services.value.filter((s) => {
    if (!showOffline.value && s.status !== 1) return false
    if (categoryFilter.value && s.category !== categoryFilter.value) return false
    if (typeFilter.value === 'bundle' && !s.bundle) return false
    if (typeFilter.value === 'single' && s.bundle) return false
    if (keyword.value && !s.name.includes(keyword.value.trim())) return false
    return true
  })
)

// 組合只能由「上架中的單項」組成（編輯時，已選的項目即使被下架也保留在選項裡）
const singleServices = computed(() =>
  services.value.filter((s) => !s.bundle && (s.status === 1 || form.bundleItemIds.includes(s.id)))
)

const selectedChildren = computed(() => services.value.filter((s) => form.bundleItemIds.includes(s.id)))
const bundleOriginal = computed(() => selectedChildren.value.reduce((sum, s) => sum + Number(s.price), 0))
const bundleDuration = computed(() => selectedChildren.value.reduce((sum, s) => sum + s.durationMinutes, 0))
const bundleSaving = computed(() => bundleOriginal.value - (form.price || 0))

const dialogTitle = computed(() => {
  const kind = form.bundle ? '組合優惠' : '服務項目'
  return `${editingId.value ? '編輯' : '新增'}${kind}`
})

function bundleNames(row) {
  return (row.bundleItemIds || [])
    .map((id) => services.value.find((s) => s.id === id)?.name)
    .filter(Boolean)
    .join(' + ')
}

function applyDiscountRate(rate) {
  form.price = Math.round(bundleOriginal.value * rate)
}

async function fetchServices() {
  loading.value = true
  try {
    services.value = await listAllServices()
  } finally {
    loading.value = false
  }
}

function openDialog(row, asBundle = false) {
  editingId.value = row ? row.id : null
  Object.assign(form, blankForm(), { bundle: asBundle })
  if (row) {
    Object.assign(form, {
      name: row.name,
      description: row.description || '',
      category: row.category || '',
      price: Number(row.price),
      durationMinutes: row.durationMinutes,
      bundle: !!row.bundle,
      bundleItemIds: [...(row.bundleItemIds || [])],
      statusOn: row.status === 1
    })
  }
  dialogVisible.value = true
}

function duplicate(row) {
  openDialog(row)
  editingId.value = null
  form.name = `${row.name}（複製）`.slice(0, 50)
}

function toPayload(f) {
  return {
    name: f.name,
    description: f.description,
    category: f.category,
    price: f.price,
    durationMinutes: f.bundle ? bundleDuration.value : f.durationMinutes,
    bundle: f.bundle,
    bundleItemIds: f.bundle ? f.bundleItemIds : [],
    status: f.statusOn ? 1 : 0
  }
}

async function handleSave() {
  if (!form.name.trim()) return ElMessage.warning('請輸入名稱')
  if (!(form.price > 0)) return ElMessage.warning('價格須大於 0')
  if (form.bundle) {
    if (form.bundleItemIds.length < 2) return ElMessage.warning('組合優惠至少要選 2 個服務項目')
    if (bundleSaving.value <= 0) return ElMessage.warning('組合優惠價須低於原價')
  }
  saving.value = true
  try {
    if (editingId.value) {
      await updateService(editingId.value, toPayload(form))
    } else {
      await createService(toPayload(form))
    }
    ElMessage.success('儲存成功')
    dialogVisible.value = false
    fetchServices()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row, on) {
  togglingId.value = row.id
  try {
    await updateService(row.id, {
      name: row.name,
      description: row.description,
      category: row.category,
      price: row.price,
      durationMinutes: row.durationMinutes,
      imageUrl: row.imageUrl,
      bundle: row.bundle,
      bundleItemIds: row.bundleItemIds || [],
      status: on ? 1 : 0
    })
    ElMessage.success(on ? '已上架' : '已下架')
    await fetchServices()
  } finally {
    togglingId.value = null
  }
}

onMounted(fetchServices)
</script>

<style scoped>
.page-container {
  max-width: 1100px;
  margin: 24px auto;
  padding: 0 16px;
}
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  margin: 16px 0;
}
.name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.bundle-contents {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}
.price-old {
  text-decoration: line-through;
  color: #909399;
  margin-right: 6px;
  font-size: 12px;
}
.price-now {
  color: #e6522c;
}
.bundle-calc {
  display: flex;
  gap: 12px;
  align-items: center;
}
.bundle-calc .save {
  color: #e6522c;
  font-weight: 600;
}
.bundle-calc .warn {
  color: #e6a23c;
}
.hint {
  margin-left: 6px;
  font-size: 12px;
  color: #909399;
}
</style>
