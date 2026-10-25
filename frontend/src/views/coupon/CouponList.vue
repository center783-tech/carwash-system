<template>
  <div class="page-container">
    <div class="header-row">
      <h2>我的優惠券</h2>
      <router-link to="/reservations/create"><el-button type="primary">現在預約</el-button></router-link>
    </div>

    <el-row :gutter="16" v-loading="loading">
      <el-col :span="8" v-for="c in coupons" :key="c.id" style="margin-bottom: 16px">
        <div class="coupon" :class="{ 'coupon--used': c.isUsed === 1 }">
          <div class="coupon__value">
            {{ c.discountType === 'FIXED' ? `NT$${c.discountValue}` : `${c.discountValue}%` }}
          </div>
          <div class="coupon__body">
            <h3>{{ c.couponCode }}</h3>
            <p>{{ c.discountType === 'FIXED' ? '消費折抵' : '折扣優惠' }} · 有效至 {{ c.endDate }}</p>
            <p class="coupon__threshold">{{ c.minAmount > 0 ? `最低消費 NT$ ${c.minAmount}` : '無最低消費' }}</p>
            <div class="coupon__footer">
              <el-tag size="small" :type="tagType(c)">{{ tagText(c) }}</el-tag>
              <el-button
                  v-if="c.isUsed === 0"
                  size="small"
                  :type="isRedeeming(c) ? 'warning' : 'primary'"
                  @click="onRedeemClick(c)"
              >
                {{ isRedeeming(c) ? `出示中 ${formatRemain(c)}` : '現場使用' }}
              </el-button>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-empty v-if="!loading && coupons.length === 0" description="目前沒有優惠券" />

    <!-- 出示給店家看的畫面：全螢幕、大字、有倒數；時間到自動關閉並變成已使用 -->
    <el-dialog
        v-model="showVisible"
        fullscreen
        :show-close="false"
        :close-on-click-modal="false"
        :close-on-press-escape="false"
        class="show-dialog"
    >
      <div v-if="showing" class="show">
        <p class="show__tip">請出示此畫面給店家確認</p>
        <div class="show__card">
          <div class="show__value">
            {{ showing.discountType === 'FIXED' ? `折抵 NT$${showing.discountValue}` : `${showing.discountValue}% 折扣` }}
          </div>
          <div class="show__code">{{ showing.couponCode }}</div>
          <p>{{ showing.minAmount > 0 ? `最低消費 NT$ ${showing.minAmount}` : '無最低消費' }}</p>
          <p>有效至 {{ showing.endDate }}</p>
          <p v-if="authStore.username">會員：{{ authStore.username }}</p>
          <!-- 每秒變動的時鐘與跑動色塊，證明是即時畫面而不是截圖 -->
          <div class="show__live">
            <span class="show__pulse" />
            {{ clock }}
          </div>
        </div>
        <div class="show__timer">
          <span>剩餘時間</span>
          <strong>{{ formatRemain(showing) }}</strong>
        </div>
        <p class="show__warn">倒數結束後，此優惠券會自動變成「已使用」</p>
        <el-button size="large" class="show__close" @click="closeShow">先關閉畫面（倒數仍會繼續）</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listMyCoupons, redeemCoupon } from '@/api/coupon'
import { useAuthStore } from '@/store/auth'

const authStore = useAuthStore()
const coupons = ref([])
const loading = ref(false)
const showingId = ref(null)
const showVisible = ref(false)
const now = ref(Date.now())
const clock = ref('')
let ticker = null

// 後端回傳的是「還剩幾秒」，收到當下換算成本機的到期時間，之後用本機時鐘倒數
function withDeadline(c) {
  return { ...c, deadline: c.redeemRemainingSeconds != null ? Date.now() + c.redeemRemainingSeconds * 1000 : null }
}

const showing = computed(() => coupons.value.find((c) => c.id === showingId.value))

function isRedeeming(c) {
  return c.isUsed === 0 && c.deadline != null && c.deadline > now.value
}

function remainSeconds(c) {
  return c.deadline ? Math.max(Math.ceil((c.deadline - now.value) / 1000), 0) : 0
}

function formatRemain(c) {
  const s = remainSeconds(c)
  return `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`
}

function tagType(c) {
  if (c.isUsed === 1) return 'info'
  return isRedeeming(c) ? 'danger' : 'warning'
}
function tagText(c) {
  if (c.isUsed === 1) return '已使用'
  return isRedeeming(c) ? '出示中' : '可使用'
}

async function fetchCoupons() {
  const list = await listMyCoupons(authStore.userId)
  coupons.value = list.map(withDeadline)
}

async function onRedeemClick(c) {
  if (isRedeeming(c)) {
    // 已經開始倒數：直接重新打開出示畫面，不重新計時
    openShow(c.id)
    return
  }
  await ElMessageBox.confirm(
      '請在店家面前再按「確定」。\n按下後會開始 10 分鐘倒數，時間到優惠券會自動變成已使用，無法復原。',
      '現場使用優惠券',
      { type: 'warning', confirmButtonText: '確定，開始出示', cancelButtonText: '取消' }
  )
  const updated = withDeadline(await redeemCoupon(c.id, authStore.userId))
  const idx = coupons.value.findIndex((x) => x.id === c.id)
  if (idx >= 0) coupons.value[idx] = { ...coupons.value[idx], ...updated }
  openShow(c.id)
}

function openShow(id) {
  showingId.value = id
  showVisible.value = true
}

function closeShow() {
  showVisible.value = false
}

function tick() {
  now.value = Date.now()
  clock.value = new Date().toLocaleTimeString('zh-TW', { hour12: false })
  // 倒數結束：關閉出示畫面，重新向後端取得「已使用」狀態
  if (showVisible.value && showing.value && !isRedeeming(showing.value)) {
    showVisible.value = false
    ElMessage.info('時間到，優惠券已變成已使用')
    fetchCoupons()
    return
  }
  // 背景中有倒數結束的券（沒開著畫面）也要刷新狀態
  const expired = coupons.value.some((c) => c.isUsed === 0 && c.deadline != null && c.deadline <= now.value)
  if (expired) fetchCoupons()
}

onMounted(async () => {
  loading.value = true
  try {
    await fetchCoupons()
  } finally {
    loading.value = false
  }
  tick()
  ticker = setInterval(tick, 1000)
})

onBeforeUnmount(() => clearInterval(ticker))
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
  gap: 12px;
}
.coupon {
  display: flex;
  border: 1px solid var(--paper-dim);
  border-radius: var(--radius-md);
  overflow: hidden;
  background: #fff;
  box-shadow: var(--shadow-card);
}
.coupon__value {
  flex-shrink: 0;
  width: 92px;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  background: var(--wax);
  color: #fff;
  font-family: var(--font-display);
  font-weight: 700;
  font-size: 18px;
  padding: 8px;
  border-right: 2px dashed rgba(255, 255, 255, 0.6);
}
.coupon__body {
  padding: 14px 16px;
  flex: 1;
}
.coupon__body h3 {
  margin: 0 0 4px;
  font-size: 15px;
  letter-spacing: 0.02em;
}
.coupon__body p {
  margin: 0 0 8px;
  font-size: 12px;
  color: var(--ink-text-secondary);
}
.coupon__threshold {
  margin-top: -4px;
}
.coupon__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.coupon--used {
  opacity: 0.55;
}
.coupon--used .coupon__value {
  background: var(--ink-text-secondary);
}

/* ---------- 出示畫面 ---------- */
.show {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  text-align: center;
  padding: 8px 0 24px;
}
.show__tip {
  margin: 0;
  font-size: 16px;
  color: var(--ink-text-secondary);
}
.show__card {
  width: 100%;
  max-width: 420px;
  padding: 28px 20px;
  background: #fff;
  border: 3px solid var(--wax);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
}
.show__card p {
  margin: 6px 0;
  font-size: 15px;
  color: var(--ink-text-secondary);
}
.show__value {
  font-family: var(--font-display);
  font-size: 34px;
  font-weight: 700;
  color: var(--wax);
}
.show__code {
  margin: 12px 0;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--ink);
}
.show__live {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 14px;
  font-size: 18px;
  font-variant-numeric: tabular-nums;
  color: var(--ink);
}
.show__pulse {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #2fb36b;
  animation: show-pulse 1s ease-in-out infinite;
}
@keyframes show-pulse {
  0%, 100% { transform: scale(0.8); opacity: 0.5; }
  50% { transform: scale(1.3); opacity: 1; }
}
.show__timer {
  display: flex;
  flex-direction: column;
  gap: 2px;
  color: var(--ink-text-secondary);
}
.show__timer strong {
  font-size: 48px;
  font-variant-numeric: tabular-nums;
  color: var(--danger);
}
.show__warn {
  margin: 0;
  font-size: 13px;
  color: var(--ink-text-secondary);
}
.show__close {
  width: 100%;
  max-width: 420px;
}
</style>
