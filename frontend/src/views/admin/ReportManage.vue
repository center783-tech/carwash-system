<template>
  <div class="report-page">
    <AdminNav class="no-print" />

    <div class="page-container">
      <header class="head">
        <div>
          <h2>營收報表</h2>
          <p class="range">{{ rangeText }}</p>
        </div>
        <div class="head__actions no-print">
          <el-button @click="exportCsv" :disabled="!report">匯出 CSV</el-button>
          <el-button @click="printPage" :disabled="!report">列印／存成 PDF</el-button>
        </div>
      </header>

      <!-- 篩選列：統一放在內容上方，一次作用於下面所有數字與圖表 -->
      <div class="filters no-print">
        <el-radio-group v-model="period" @change="fetchReport">
          <el-radio-button v-for="p in periods" :key="p.value" :value="p.value">{{ p.label }}</el-radio-button>
        </el-radio-group>
        <div class="filters__date">
          <el-button :icon="ArrowLeft" aria-label="上一期" @click="shift(-1)" />
          <el-date-picker
              v-model="date"
              type="date"
              value-format="YYYY-MM-DD"
              :clearable="false"
              :editable="false"
              :disabled-date="(d) => d > new Date()"
              style="width: 150px"
              @change="fetchReport"
          />
          <el-button :icon="ArrowRight" aria-label="下一期" :disabled="isCurrentPeriod" @click="shift(1)" />
          <el-button @click="goToday" :disabled="isCurrentPeriod">回到今天</el-button>
        </div>
      </div>
      <p class="hint no-print">報表涵蓋所選日期所在的{{ periodLabel }}；週從週一算到週日。</p>

      <div :class="{ 'is-refetching': loading }">
        <template v-if="report">
          <!-- KPI -->
          <section class="kpis">
            <div class="kpi kpi--hero">
              <span class="kpi__label">{{ periodLabel }}營收</span>
              <span class="kpi__hero">{{ money(report.summary.revenue) }}</span>
              <span class="kpi__delta" :class="deltaClass">
                <template v-if="report.summary.growthRate === null">
                  {{ report.summary.previousRevenue > 0 ? '' : '上一期無營收可比較' }}
                </template>
                <template v-else>
                  <span aria-hidden="true">{{ report.summary.growthRate >= 0 ? '▲' : '▼' }}</span>
                  {{ report.summary.growthRate >= 0 ? '成長' : '衰退' }}
                  {{ Math.abs(report.summary.growthRate) }}%
                  <small>（上一期 {{ money(report.summary.previousRevenue) }}）</small>
                </template>
              </span>
            </div>
            <div class="kpi">
              <span class="kpi__label">付款筆數</span>
              <span class="kpi__value">{{ report.summary.paymentCount }}</span>
            </div>
            <div class="kpi">
              <span class="kpi__label">平均客單價</span>
              <span class="kpi__value">{{ money(report.summary.averageOrderValue) }}</span>
            </div>
            <div class="kpi">
              <span class="kpi__label">已退款</span>
              <span class="kpi__value">{{ money(report.summary.refundAmount) }}</span>
              <span class="kpi__sub">{{ report.summary.refundCount }} 筆（不計入營收）</span>
            </div>
          </section>

          <section class="resv">
            預約 <strong>{{ report.reservations.total }}</strong> 筆
            <span class="dot" />已完成 <strong>{{ report.reservations.completed }}</strong>
            <span class="dot" />已取消 <strong>{{ report.reservations.cancelled }}</strong>
            <small>（依預約日期統計）</small>
          </section>

          <!-- 趨勢 -->
          <section class="card">
            <div class="card__head">
              <h3>{{ trendTitle }}</h3>
              <el-button text size="small" class="no-print" @click="showTable = !showTable">
                {{ showTable ? '看圖表' : '看表格' }}
              </el-button>
            </div>

            <div v-if="!showTable" class="chart" role="img" :aria-label="trendTitle">
              <div class="chart__y">
                <span v-for="t in yTicks" :key="t" :style="{ bottom: (t / yMax) * 100 + '%' }">{{ compact(t) }}</span>
              </div>
              <div class="chart__plot" @pointerleave="tip = null">
                <div v-for="t in yTicks" :key="'g' + t" class="chart__grid" :style="{ bottom: (t / yMax) * 100 + '%' }" />
                <div class="chart__cols">
                  <div
                      v-for="(p, i) in report.trend"
                      :key="p.label"
                      class="col"
                      tabindex="0"
                      :aria-label="`${p.label} 營收 ${money(p.revenue)}，${p.count} 筆`"
                      @pointermove="showTip($event, p)"
                      @focus="showTip($event, p)"
                      @blur="tip = null"
                  >
                    <span
                        v-if="i === peakIndex && p.revenue > 0"
                        class="col__peak"
                        :style="{ bottom: `calc(${(p.revenue / yMax) * 100}% + 4px)` }"
                    >{{ compact(p.revenue) }}</span>
                    <span class="col__bar" :style="{ height: (p.revenue / yMax) * 100 + '%' }" />
                    <span v-if="showLabel(i)" class="col__label">{{ shortLabel(p.label) }}</span>
                  </div>
                </div>
                <div v-if="tip" class="tip" :style="{ left: tip.x + 'px', top: tip.y + 'px' }">
                  <strong>{{ money(tip.p.revenue) }}</strong>
                  <span>{{ tip.p.label }} · {{ tip.p.count }} 筆</span>
                </div>
              </div>
            </div>
            <p v-if="!showTable && totalRevenue === 0" class="empty">這段期間沒有已付款的營收</p>

            <el-table v-if="showTable" :data="report.trend" size="small" max-height="360">
              <el-table-column prop="label" :label="trendColumn" />
              <el-table-column label="營收" align="right">
                <template #default="{ row }">{{ money(row.revenue) }}</template>
              </el-table-column>
              <el-table-column prop="count" label="筆數" align="right" />
            </el-table>
          </section>

          <div class="two">
            <section class="card">
              <h3>付款方式</h3>
              <ul v-if="report.byMethod.length" class="bars">
                <li v-for="m in report.byMethod" :key="m.method">
                  <span class="bars__name">{{ methodName(m.method) }}</span>
                  <span class="bars__track"><span class="bars__fill" :style="{ width: share(m.revenue, methodMax) + '%' }" /></span>
                  <span class="bars__val">{{ money(m.revenue) }}<small>{{ m.count }} 筆</small></span>
                </li>
              </ul>
              <p v-else class="empty">沒有資料</p>
            </section>

            <section class="card">
              <h3>服務項目營收</h3>
              <ul v-if="report.byService.length" class="bars">
                <li v-for="s in report.byService" :key="s.name">
                  <span class="bars__name">{{ s.name }}</span>
                  <span class="bars__track"><span class="bars__fill" :style="{ width: share(s.revenue, serviceMax) + '%' }" /></span>
                  <span class="bars__val">{{ money(s.revenue) }}<small>{{ s.count }} 筆</small></span>
                </li>
              </ul>
              <p v-else class="empty">沒有資料</p>
            </section>
          </div>

          <p class="note">
            營收以「已付款」付款單的付款時間認列（現金收款、信用卡、轉帳皆同）；已退款的款項不計入營收，另列於「已退款」。
          </p>
        </template>
        <el-skeleton v-else-if="loading" :rows="8" animated />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import AdminNav from '@/components/AdminNav.vue'
import { getRevenueReport } from '@/api/report'

const periods = [
  { value: 'DAY', label: '當日', name: '日' },
  { value: 'WEEK', label: '週', name: '週' },
  { value: 'MONTH', label: '月', name: '月' },
  { value: 'QUARTER', label: '季', name: '季' }
]
const METHOD_NAMES = { CREDIT_CARD: '信用卡', ATM: 'ATM 轉帳', NEWEBPAY: '藍新金流', CASH: '現金' }

// 不要用 toISOString()：那是 UTC，台灣凌晨會變成前一天
function localDateString(d = new Date()) {
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${m}-${day}`
}

const period = ref('MONTH')
const date = ref(localDateString())
const report = ref(null)
const loading = ref(false)
const showTable = ref(false)
const tip = ref(null)

const periodLabel = computed(() => (period.value === 'DAY' ? '當日' : '本' + periods.find((p) => p.value === period.value).name))
const trendColumn = computed(() => ({ DAY: '時段', QUARTER: '月份' })[period.value] || '日期')
const trendTitle = computed(
  () => ({ DAY: '每小時營收', WEEK: '每日營收', MONTH: '每日營收', QUARTER: '每月營收' })[period.value]
)

const rangeText = computed(() => {
  if (!report.value) return ''
  const { startDate, endDate } = report.value
  const f = (s) => s.replaceAll('-', '/')
  return startDate === endDate ? f(startDate) : `${f(startDate)} – ${f(endDate)}`
})

// 目前選的區間是否已包含今天（包含就不能再往後翻）
const isCurrentPeriod = computed(() => {
  if (!report.value) return true
  const today = localDateString()
  return report.value.startDate <= today && today <= report.value.endDate
})

const deltaClass = computed(() => {
  const g = report.value?.summary.growthRate
  if (g === null || g === undefined) return ''
  return g >= 0 ? 'is-up' : 'is-down'
})

// ---------------------------------------------------------------- 圖表

const totalRevenue = computed(() => Number(report.value?.summary.revenue ?? 0))
const maxRevenue = computed(() => Math.max(0, ...(report.value?.trend ?? []).map((p) => Number(p.revenue))))
const peakIndex = computed(() => (report.value?.trend ?? []).findIndex((p) => Number(p.revenue) === maxRevenue.value))

// Y 軸上限取「漂亮的整數」，刻度才會是 0 / 1K / 2K 這種好讀的值
const yMax = computed(() => niceCeil(maxRevenue.value))
const yTicks = computed(() => [0, 1, 2, 3, 4].map((i) => (yMax.value / 4) * i))

function niceCeil(v) {
  if (v <= 0) return 1000
  const pow = 10 ** Math.floor(Math.log10(v))
  for (const m of [1, 2, 2.5, 5, 10]) {
    if (m * pow >= v) return m * pow
  }
  return 10 * pow
}

function compact(n) {
  n = Number(n)
  if (n >= 1000) return `${+(n / 1000).toFixed(1)}K`
  return String(n)
}

function showLabel(i) {
  const n = report.value.trend.length
  if (n <= 12) return true
  // 日資料點多時，X 軸只標一部分，避免文字擠在一起
  return n > 24 ? i % 5 === 0 : i % 3 === 0
}

function shortLabel(label) {
  return period.value === 'DAY' ? label.slice(0, 2) : label
}

function showTip(e, p) {
  const plot = e.currentTarget.closest('.chart__plot').getBoundingClientRect()
  const col = e.currentTarget.getBoundingClientRect()
  const x = Math.min(Math.max(col.left + col.width / 2 - plot.left, 70), plot.width - 70)
  tip.value = { p, x, y: 4 }
}

// ---------------------------------------------------------------- 資料

const methodMax = computed(() => Math.max(1, ...(report.value?.byMethod ?? []).map((m) => Number(m.revenue))))
const serviceMax = computed(() => Math.max(1, ...(report.value?.byService ?? []).map((s) => Number(s.revenue))))
const share = (v, max) => Math.max(2, (Number(v) / max) * 100)
const methodName = (m) => METHOD_NAMES[m] || m
const money = (n) => `NT$ ${Number(n).toLocaleString('zh-TW')}`

let requestSeq = 0
async function fetchReport() {
  const seq = ++requestSeq
  loading.value = true
  try {
    const result = await getRevenueReport(period.value, date.value)
    // 快速連點切換時，只採用最後一次請求的結果
    if (seq === requestSeq) report.value = result
  } finally {
    if (seq === requestSeq) loading.value = false
  }
}

function shift(n) {
  const d = new Date(report.value.startDate + 'T00:00:00')
  if (period.value === 'DAY') d.setDate(d.getDate() + n)
  else if (period.value === 'WEEK') d.setDate(d.getDate() + 7 * n)
  else d.setMonth(d.getMonth() + (period.value === 'MONTH' ? n : 3 * n))
  date.value = localDateString(d)
  fetchReport()
}

function goToday() {
  date.value = localDateString()
  fetchReport()
}

// ---------------------------------------------------------------- 匯出

// 後台自訂的名稱可能以 = + - @ 開頭，Excel 會當成公式執行，開頭補一個單引號
function csvCell(v) {
  let s = String(v ?? '')
  if (/^[=+\-@\t\r]/.test(s)) s = "'" + s
  return `"${s.replaceAll('"', '""')}"`
}

function exportCsv() {
  const r = report.value
  const rows = [
    ['洗享快 營收報表'],
    ['統計區間', periodLabel.value, rangeText.value],
    [],
    ['總覽'],
    ['營收', r.summary.revenue],
    ['付款筆數', r.summary.paymentCount],
    ['平均客單價', r.summary.averageOrderValue],
    ['已退款金額', r.summary.refundAmount],
    ['已退款筆數', r.summary.refundCount],
    ['上一期營收', r.summary.previousRevenue],
    ['成長率(%)', r.summary.growthRate ?? ''],
    ['預約數', r.reservations.total],
    ['已完成', r.reservations.completed],
    ['已取消', r.reservations.cancelled],
    [],
    [trendTitle.value],
    [trendColumn.value, '營收', '筆數'],
    ...r.trend.map((p) => [p.label, p.revenue, p.count]),
    [],
    ['付款方式'],
    ['方式', '營收', '筆數'],
    ...r.byMethod.map((m) => [methodName(m.method), m.revenue, m.count]),
    [],
    ['服務項目'],
    ['項目', '營收', '筆數'],
    ...r.byService.map((s) => [s.name, s.revenue, s.count])
  ]
  // 開頭加 BOM，Excel 才不會把中文開成亂碼
  const csv = '﻿' + rows.map((row) => row.map(csvCell).join(',')).join('\r\n')
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }))
  const a = document.createElement('a')
  a.href = url
  a.download = `營收報表_${period.value}_${r.startDate}.csv`
  a.click()
  URL.revokeObjectURL(url)
}

function printPage() {
  window.print()
}

onMounted(fetchReport)
</script>

<style scoped>
.page-container {
  max-width: 1100px;
  margin: 24px auto;
  padding: 0 16px 48px;
}
.head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  flex-wrap: wrap;
}
.head h2 {
  margin: 0;
}
.range {
  margin: 4px 0 0;
  color: var(--ink-text-secondary);
  font-size: 15px;
}
.head__actions {
  display: flex;
  gap: 8px;
}
.filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px 20px;
  margin: 16px 0 4px;
}
.filters__date {
  display: flex;
  gap: 8px;
  align-items: center;
}
.hint {
  margin: 4px 0 16px;
  font-size: 13px;
  color: var(--ink-text-secondary);
}

/* 重新載入時保留畫面框架，只降低透明度，不閃爍 */
.is-refetching {
  opacity: 0.55;
  transition: opacity 0.15s;
  pointer-events: none;
}

/* ---------- KPI ---------- */
.kpis {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr 1fr;
  gap: 16px;
  margin-bottom: 12px;
}
.kpi {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 18px 20px;
  background: #fff;
  border: 1px solid var(--paper-dim);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
}
.kpi--hero {
  background: var(--ink);
  border-color: var(--ink);
  color: var(--paper);
}
.kpi__label {
  font-size: 13px;
  color: var(--ink-text-secondary);
}
.kpi--hero .kpi__label {
  color: #b7c4c9;
}
.kpi__hero {
  /* 全頁唯一的主數字 */
  font-size: 48px;
  font-weight: 700;
  line-height: 1.15;
  font-family: var(--font-body);
}
.kpi__value {
  font-size: 24px;
  font-weight: 700;
  color: var(--ink);
}
.kpi__sub {
  font-size: 12px;
  color: var(--ink-text-secondary);
}
.kpi__delta {
  font-size: 14px;
  color: #b7c4c9;
}
.kpi__delta small {
  color: #8fa2aa;
}
.kpi__delta.is-up {
  color: #7fd9a6;
}
.kpi__delta.is-down {
  color: #ff9d94;
}

.resv {
  font-size: 14px;
  color: var(--ink-text-secondary);
  margin-bottom: 16px;
}
.resv strong {
  color: var(--ink);
  font-size: 16px;
}
.resv small {
  margin-left: 8px;
}
.dot {
  display: inline-block;
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: var(--ink-text-secondary);
  margin: 0 10px;
  vertical-align: middle;
}

/* ---------- 卡片 ---------- */
.card {
  background: #fff;
  border: 1px solid var(--paper-dim);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
  padding: 18px 20px 20px;
  margin-bottom: 16px;
}
.card h3 {
  margin: 0 0 14px;
  font-size: 16px;
}
.card__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card__head h3 {
  margin-bottom: 8px;
}
.two {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.empty {
  margin: 8px 0;
  color: var(--ink-text-secondary);
  font-size: 14px;
}

/* ---------- 直條圖（單一系列，不需要圖例，標題已說明） ---------- */
.chart {
  display: flex;
  height: 260px;
  padding-bottom: 28px; /* 留給 X 軸文字 */
}
.chart__y {
  position: relative;
  width: 44px;
  flex-shrink: 0;
}
.chart__y span {
  position: absolute;
  right: 8px;
  transform: translateY(50%);
  font-size: 12px;
  color: var(--ink-text-secondary);
  font-variant-numeric: tabular-nums;
}
.chart__plot {
  position: relative;
  flex: 1;
  min-width: 0;
}
.chart__grid {
  position: absolute;
  left: 0;
  right: 0;
  height: 0;
  border-top: 1px solid var(--paper-dim);
}
.chart__cols {
  position: absolute;
  inset: 0;
  display: flex;
}
.col {
  position: relative;
  flex: 1;
  min-width: 0;
  display: flex;
  justify-content: center;
  align-items: flex-end;
  outline: none;
  cursor: default;
}
.col__bar {
  width: min(100%, 24px); /* 直條最粗 24px，剩下的留白 */
  max-width: calc(100% - 4px);
  border-radius: 4px 4px 0 0;
  background: var(--rinse);
  transition: filter 0.15s;
  min-height: 0;
}
.col:hover .col__bar,
.col:focus-visible .col__bar {
  filter: brightness(1.18);
}
.col:focus-visible {
  outline: 2px solid var(--rinse);
  outline-offset: -2px;
  border-radius: 4px;
}
.col__peak {
  position: absolute;
  font-size: 12px;
  font-weight: 700;
  color: var(--ink);
  white-space: nowrap;
}
.col__label {
  position: absolute;
  top: 100%;
  margin-top: 8px;
  font-size: 12px;
  color: var(--ink-text-secondary);
  white-space: nowrap;
}
.tip {
  position: absolute;
  transform: translateX(-50%);
  pointer-events: none;
  z-index: 2;
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 8px 12px;
  border-radius: 8px;
  background: var(--ink);
  color: var(--paper);
  font-size: 12px;
  white-space: nowrap;
  box-shadow: 0 6px 18px rgba(20, 32, 43, 0.25);
}
.tip strong {
  font-size: 15px;
}
.tip span {
  color: #b7c4c9;
}

/* ---------- 橫條排行 ---------- */
.bars {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.bars li {
  display: grid;
  grid-template-columns: 84px 1fr auto;
  align-items: center;
  gap: 12px;
  font-size: 14px;
}
.bars__name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.bars__track {
  height: 14px;
  border-radius: 4px;
  background: var(--el-color-primary-light-9);
  overflow: hidden;
}
.bars__fill {
  display: block;
  height: 100%;
  border-radius: 0 4px 4px 0;
  background: var(--rinse);
}
.bars__val {
  min-width: 96px;
  text-align: right;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}
.bars__val small {
  display: block;
  font-weight: 400;
  font-size: 12px;
  color: var(--ink-text-secondary);
}
.note {
  font-size: 12px;
  color: var(--ink-text-secondary);
  line-height: 1.7;
}

@media (max-width: 860px) {
  .kpis {
    grid-template-columns: 1fr 1fr;
  }
  .kpi--hero {
    grid-column: 1 / -1;
  }
  .kpi__hero {
    font-size: 40px;
  }
  .two {
    grid-template-columns: 1fr;
  }
  .bars li {
    grid-template-columns: 70px 1fr auto;
  }
}
</style>

<style>
/* 列印：只留報表內容，拿掉導覽與操作按鈕，讓使用者可以直接存成 PDF 當財報 */
@media print {
  .no-print {
    display: none !important;
  }
  .report-page .page-container {
    max-width: none;
    margin: 0;
  }
  .report-page .kpi--hero {
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }
  .report-page .col__bar,
  .report-page .bars__fill {
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }
}
</style>
