<template>
  <div>
  <AdminNav />
  <div class="page-container">
    <div class="header-row">
      <h2>優惠券管理</h2>
      <el-button type="primary" @click="dialogVisible = true">新增優惠券</el-button>
    </div>

    <el-table :data="coupons" v-loading="loading">
      <el-table-column prop="code" label="代碼" />
      <el-table-column label="折扣">
        <template #default="{ row }">
          {{ row.discountType === 'FIXED' ? `折抵 NT$${row.discountValue}` : `${row.discountValue}% 折扣` }}
        </template>
      </el-table-column>
      <el-table-column label="最低消費">
        <template #default="{ row }">
          {{ row.minAmount > 0 ? `NT$${row.minAmount}` : '無門檻' }}
        </template>
      </el-table-column>
      <el-table-column prop="startDate" label="生效日" />
      <el-table-column prop="endDate" label="到期日" />
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button link type="primary" @click="openIssueDialog(row)">發放給會員</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增優惠券 -->
    <el-dialog v-model="dialogVisible" title="新增優惠券">
      <el-form :model="form" label-width="100px">
        <el-form-item label="代碼"><el-input v-model="form.code" /></el-form-item>
        <el-form-item label="折扣類型">
          <el-select v-model="form.discountType">
            <el-option label="定額折抵" value="FIXED" />
            <el-option label="百分比折扣" value="PERCENTAGE" />
          </el-select>
        </el-form-item>
        <el-form-item label="折扣數值">
          <el-input-number v-model="form.discountValue" :min="0" />
          <span class="hint">定額請輸入金額；百分比請輸入 1~100（例如 90 代表 9 折）</span>
        </el-form-item>
        <el-form-item label="最低消費"><el-input-number v-model="form.minAmount" :min="0" /></el-form-item>
        <el-form-item label="生效日期"><el-date-picker v-model="form.startDate" type="date" /></el-form-item>
        <el-form-item label="到期日期"><el-date-picker v-model="form.endDate" type="date" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleCreate">儲存</el-button>
      </template>
    </el-dialog>

    <!-- 發放給會員 -->
    <el-dialog v-model="issueDialogVisible" title="發放給會員">
      <el-form label-width="80px">
        <el-form-item label="會員 ID">
          <el-input-number v-model="issueMemberId" :min="1" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="issueDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="issuing" @click="handleIssue">發放</el-button>
      </template>
    </el-dialog>
  </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import AdminNav from '@/components/AdminNav.vue'
import { listActiveCoupons, createCoupon, issueCouponToMember } from '@/api/coupon'

const coupons = ref([])
const loading = ref(false)
const saving = ref(false)
const issuing = ref(false)
const dialogVisible = ref(false)
const issueDialogVisible = ref(false)
const currentCouponId = ref(null)
const issueMemberId = ref(null)

const form = reactive({
  code: '', discountType: 'FIXED', discountValue: 0, minAmount: 0, startDate: null, endDate: null
})

function formatDate(date) {
  return date ? new Date(date).toISOString().slice(0, 10) : null
}

async function fetchCoupons() {
  loading.value = true
  try {
    coupons.value = await listActiveCoupons()
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  saving.value = true
  try {
    await createCoupon({ ...form, startDate: formatDate(form.startDate), endDate: formatDate(form.endDate) })
    ElMessage.success('優惠券建立成功')
    dialogVisible.value = false
    fetchCoupons()
  } finally {
    saving.value = false
  }
}

function openIssueDialog(row) {
  currentCouponId.value = row.id
  issueMemberId.value = null
  issueDialogVisible.value = true
}

async function handleIssue() {
  issuing.value = true
  try {
    await issueCouponToMember(currentCouponId.value, issueMemberId.value)
    ElMessage.success('發放成功')
    issueDialogVisible.value = false
  } finally {
    issuing.value = false
  }
}

onMounted(fetchCoupons)
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
.hint {
  display: block;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
