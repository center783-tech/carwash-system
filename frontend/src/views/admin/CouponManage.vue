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
        <el-form-item label="生效日期"><el-date-picker v-model="form.startDate" type="date" :editable="false" /></el-form-item>
        <el-form-item label="到期日期"><el-date-picker v-model="form.endDate" type="date" :editable="false" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleCreate">儲存</el-button>
      </template>
    </el-dialog>

    <!-- 發放給會員：先列出 10 筆，勾選後一次發放 -->
    <el-dialog v-model="issueDialogVisible" title="發放給會員" width="640px">
      <el-input
          v-model="keyword"
          placeholder="搜尋姓名、電話、Email"
          clearable
          @keyup.enter="searchMembers"
          @clear="searchMembers"
      >
        <template #append><el-button @click="searchMembers">搜尋</el-button></template>
      </el-input>

      <div v-loading="membersLoading" class="member-list">
        <label v-for="m in members" :key="m.id" class="member-row" :class="{ 'member-row--on': selected.has(m.id) }">
          <el-checkbox :model-value="selected.has(m.id)" @change="toggleMember(m)" />
          <div class="member-row__info">
            <strong>{{ m.realName || m.username }}</strong>
            <span>{{ m.phone || '—' }}</span>
            <span>{{ m.email || '—' }}</span>
          </div>
        </label>
        <p v-if="!membersLoading && members.length === 0" class="hint">找不到符合的會員</p>
      </div>

      <div class="member-more">
        <span class="hint">已載入 {{ members.length }} / {{ memberTotal }} 位</span>
        <el-button v-if="members.length < memberTotal" :loading="membersLoading" @click="loadMore">載入更多 10 筆</el-button>
      </div>

      <template #footer>
        <span class="footer-hint">已勾選 {{ selected.size }} 位，發放後會寄通知信</span>
        <el-button @click="issueDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="issuing" :disabled="selected.size === 0" @click="handleIssue">
          發放（{{ selected.size }}）
        </el-button>
      </template>
    </el-dialog>
  </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import AdminNav from '@/components/AdminNav.vue'
import { listActiveCoupons, createCoupon, listMembersForAdmin, issueCouponToMembers } from '@/api/coupon'

const coupons = ref([])
const loading = ref(false)
const saving = ref(false)
const issuing = ref(false)
const dialogVisible = ref(false)
const issueDialogVisible = ref(false)
const currentCouponId = ref(null)
const keyword = ref('')
const members = ref([])
const memberTotal = ref(0)
const membersLoading = ref(false)
const selected = reactive(new Set())
let memberPage = 0
let memberSeq = 0

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
  keyword.value = ''
  selected.clear()
  issueDialogVisible.value = true
  searchMembers()
}

// 先撈 10 筆，要更多再按「載入更多」；勾選的會員跨頁保留
const PAGE_SIZE = 10
async function fetchMembers(page) {
  membersLoading.value = true
  const seq = ++memberSeq
  try {
    const result = await listMembersForAdmin({ keyword: keyword.value, page, size: PAGE_SIZE })
    if (seq !== memberSeq) return
    members.value = page === 0 ? result.content : [...members.value, ...result.content]
    memberPage = page
    memberTotal.value = result.totalElements
  } finally {
    if (seq === memberSeq) membersLoading.value = false
  }
}
function searchMembers() {
  members.value = []
  memberTotal.value = 0
  return fetchMembers(0)
}
function loadMore() {
  return fetchMembers(memberPage + 1)
}

function toggleMember(m) {
  if (selected.has(m.id)) selected.delete(m.id)
  else selected.add(m.id)
}

async function handleIssue() {
  issuing.value = true
  try {
    const result = await issueCouponToMembers(currentCouponId.value, [...selected])
    if (result.emailFailed === 0) {
      ElMessage.success(`已發放給 ${result.issued} 位會員，並寄送通知信`)
    } else {
      ElMessage.warning(`已發放給 ${result.issued} 位會員，其中 ${result.emailFailed} 位通知信寄送失敗，請另行通知`)
    }
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
.member-list {
  margin-top: 12px;
  min-height: 80px;
  max-height: 50vh;
  overflow-y: auto;
}
.member-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  margin-bottom: 8px;
  cursor: pointer;
  border: 1px solid var(--paper-dim);
  border-radius: var(--radius-md);
  background: #fff;
}
.member-row--on {
  border-color: var(--rinse);
  background: var(--el-color-primary-light-9);
}
.member-row__info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  font-size: 13px;
  color: var(--ink-text-secondary);
  word-break: break-all;
}
.member-row__info strong {
  font-size: 15px;
  color: var(--ink);
}
.member-more {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}
.footer-hint {
  margin-right: 12px;
  font-size: 13px;
  color: var(--ink-text-secondary);
}
.hint {
  display: block;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
