<template>
  <div class="home">
    <!-- ===== Hero ===== -->
    <section class="hero">
      <div class="hero__bubbles" aria-hidden="true">
        <span v-for="n in 10" :key="n" class="hero__bubble" :style="bubbleStyle(n)" />
      </div>

      <div class="hero__text">
        <span class="hero__badge">線上預約 · 免排隊</span>
        <h1 class="hero__headline">
          洗去塵囂，<br />
          讓愛車<span class="hero__shine">重新發亮</span>。
        </h1>
        <p class="hero__sub">
          線上預約洗車、打蠟、鍍膜與保養，選好時段直接開車過來，
          不用現場排隊等候。
        </p>
        <div class="hero__actions">
          <router-link to="/services">
            <el-button type="primary" size="large" round>瀏覽服務項目</el-button>
          </router-link>
          <router-link to="/register">
            <el-button size="large" round plain>會員註冊</el-button>
          </router-link>
          <a href="#about" class="hero__link">認識我們 ↓</a>
        </div>
        <ul class="hero__chips">
          <li>泡沫沖洗</li>
          <li>手工打蠟</li>
          <li>長效鍍膜</li>
        </ul>
      </div>

      <div class="hero__art">
        <div class="hero__stage">
          <CarWashScene />
        </div>
      </div>

      <svg class="hero__wave" viewBox="0 0 1440 60" preserveAspectRatio="none" aria-hidden="true">
        <path d="M0,30 C240,70 480,0 720,28 C960,56 1200,6 1440,32 L1440,60 L0,60 Z" fill="#f3f6f6" />
      </svg>
    </section>

    <!-- ===== 數字亮點 ===== -->
    <section class="stats">
      <div class="stat">
        <span class="stat__value">{{ stats.count }}</span>
        <span class="stat__label">種服務項目</span>
      </div>
      <div class="stat">
        <span class="stat__value">{{ stats.fastest }}</span>
        <span class="stat__label">最快完工時長</span>
      </div>
      <div class="stat">
        <span class="stat__value">線上付款</span>
        <span class="stat__label">免現場等候結帳</span>
      </div>
      <div class="stat">
        <span class="stat__value">會員優惠券</span>
        <span class="stat__label">老客戶回訪更划算</span>
      </div>
    </section>

    <!-- ===== 熱門服務 ===== -->
    <section class="section">
      <div class="section__head reveal">
        <span class="eyebrow">OUR SERVICES</span>
        <h2>從日常清潔到深度美容</h2>
        <p>不管是日常洗去灰塵，還是想讓車漆重回新車亮度，都能找到適合的項目。</p>
      </div>

      <div class="svc-grid">
        <template v-if="loadingServices">
          <div v-for="n in 3" :key="n" class="svc svc--skeleton" />
        </template>
        <router-link
          v-for="item in featured"
          :key="item.id"
          :to="{ path: '/reservations/create', query: { serviceId: item.id } }"
          class="svc reveal"
        >
          <div class="svc__icon"><ServiceIcon :name="item.name" :category="item.category" /></div>
          <span v-if="item.category" class="svc__tag">{{ item.category }}</span>
          <h3>{{ item.name }}</h3>
          <p>{{ item.description }}</p>
          <div class="svc__meta">
            <span class="price-tag">NT$ {{ item.price }}</span>
            <span>約 {{ item.durationMinutes }} 分鐘</span>
          </div>
        </router-link>
      </div>

      <div class="section__more reveal">
        <router-link to="/services">
          <el-button round plain>查看全部服務 →</el-button>
        </router-link>
      </div>
    </section>

    <!-- ===== 預約流程 ===== -->
    <section id="about" class="section section--tint">
      <div class="section__head reveal">
        <span class="eyebrow">HOW IT WORKS</span>
        <h2>四步驟，輕鬆搞定洗車</h2>
        <p>把傳統洗車場常見的現場排隊、打電話問時段，整個搬到線上。</p>
      </div>

      <ol class="steps">
        <li v-for="(s, i) in steps" :key="s.title" class="step reveal" :style="{ transitionDelay: i * 90 + 'ms' }">
          <div class="step__num">{{ i + 1 }}</div>
          <h3>{{ s.title }}</h3>
          <p>{{ s.text }}</p>
        </li>
      </ol>
    </section>

    <!-- ===== 為什麼選擇我們 ===== -->
    <section class="section">
      <div class="section__head reveal">
        <span class="eyebrow">WHY US</span>
        <h2>為什麼選擇洗享快</h2>
      </div>

      <div class="why-grid">
        <div v-for="(w, i) in reasons" :key="w.title" class="why reveal" :style="{ transitionDelay: i * 80 + 'ms' }">
          <div class="why__icon">
            <svg viewBox="0 0 48 48" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path v-for="p in w.paths" :key="p" :d="p" />
            </svg>
          </div>
          <h3>{{ w.title }}</h3>
          <p>{{ w.text }}</p>
        </div>
      </div>
    </section>

    <!-- ===== CTA ===== -->
    <section class="cta reveal">
      <div class="cta__bubbles" aria-hidden="true">
        <span v-for="n in 8" :key="n" class="hero__bubble" :style="bubbleStyle(n + 3)" />
      </div>
      <h2>今天就讓愛車煥然一新</h2>
      <p>註冊會員、新增愛車，三分鐘完成第一筆預約。</p>
      <div class="cta__actions">
        <router-link to="/services">
          <el-button type="warning" size="large" round>立即預約</el-button>
        </router-link>
        <router-link v-if="!authStore.isLoggedIn" to="/register">
          <el-button size="large" round plain class="cta__ghost">免費註冊</el-button>
        </router-link>
      </div>
    </section>

    <footer class="footer">
      <div class="footer__brand">
        <strong>洗享快</strong>
        <span>洗車場預約管理系統</span>
      </div>
      <nav class="footer__links">
        <router-link to="/services">服務項目</router-link>
        <router-link to="/reservations">我的預約</router-link>
        <router-link to="/coupons">優惠券</router-link>
      </nav>
      <span class="footer__copy">© {{ year }} 洗享快</span>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import CarWashScene from '@/components/CarWashScene.vue'
import ServiceIcon from '@/components/ServiceIcon.vue'
import { listServices } from '@/api/service'
import { useAuthStore } from '@/store/auth'

const year = new Date().getFullYear()
const authStore = useAuthStore()

const services = ref([])
const loadingServices = ref(true)

const featured = computed(() => services.value.slice(0, 6))

// 數字從實際的服務資料算出來，後台新增或下架項目後首頁會跟著變
const stats = computed(() => {
  if (!services.value.length) return { count: '多種', fastest: '30 分鐘起' }
  const fastest = Math.min(...services.value.map((s) => s.durationMinutes))
  return { count: services.value.length, fastest: `${fastest} 分鐘起` }
})

const steps = [
  { title: '選擇服務', text: '瀏覽洗車、美容、保養項目，挑出愛車需要的那一項。' },
  { title: '挑時段與車位', text: '即時看到每個時段的空位，選好日期、時段與車位區。' },
  { title: '線上付款', text: '信用卡或轉帳線上結清，也可以到店現金付款。' },
  { title: '開車來、評價去', text: '準時到場直接交車，完成後留下評價，幫其他車主參考。' }
]

const reasons = [
  {
    title: '準時不用等',
    text: '預約制管理車位與人力，到場即可開始，不必現場排隊。',
    paths: ['M24 6 a18 18 0 1 0 0.01 0 Z', 'M24 14 v11 l7 4']
  },
  {
    title: '價格透明',
    text: '項目、時間、金額事先看得清楚，不會到場才加價。',
    paths: ['M9 14 h30 v22 h-30 z', 'M9 22 h30', 'M16 30 h6']
  },
  {
    title: '優惠回饋',
    text: '會員專屬優惠券，老客戶回訪更划算。',
    paths: ['M6 17 h36 v6 a4 4 0 0 0 0 8 v6 h-36 v-6 a4 4 0 0 0 0 -8 z', 'M18 17 v22']
  },
  {
    title: '真實評價',
    text: '完成服務後才能評價，讓下一位車主做更好的選擇。',
    paths: ['M24 6 l5.5 11.5 12.5 1.8 -9 8.8 2.1 12.4 -11.1 -5.9 -11.1 5.9 2.1 -12.4 -9 -8.8 12.5 -1.8 z']
  }
]

// 背景泡泡的位置、大小、速度：用固定公式產生，避免每次重新整理都不一樣
function bubbleStyle(n) {
  const size = 10 + ((n * 7) % 5) * 6
  return {
    left: `${(n * 37) % 100}%`,
    width: `${size}px`,
    height: `${size}px`,
    animationDelay: `${(n * 0.9) % 6}s`,
    animationDuration: `${7 + ((n * 3) % 6)}s`
  }
}

// 捲動到畫面時才淡入；使用者偏好減少動態時直接顯示
let observer = null
function setupReveal() {
  const targets = document.querySelectorAll('.home .reveal:not(.is-visible)')
  const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  if (reduce || !('IntersectionObserver' in window)) {
    targets.forEach((el) => el.classList.add('is-visible'))
    return
  }
  observer = observer || new IntersectionObserver(
    (entries) => {
      entries.forEach((e) => {
        if (e.isIntersecting) {
          e.target.classList.add('is-visible')
          observer.unobserve(e.target)
        }
      })
    },
    { threshold: 0.15 }
  )
  targets.forEach((el) => observer.observe(el))
}

onMounted(async () => {
  setupReveal()
  try {
    services.value = await listServices()
  } catch {
    services.value = []
  } finally {
    loadingServices.value = false
  }
  // 服務卡片是資料回來後才出現，要再掛一次
  await nextTick()
  setupReveal()
})

onBeforeUnmount(() => observer?.disconnect())
</script>

<style scoped>
.home {
  background: var(--paper);
  overflow-x: hidden;
}

/* ---------- Hero ---------- */
.hero {
  position: relative;
  background:
    radial-gradient(900px 400px at 85% 10%, rgba(28, 124, 140, 0.35), transparent 60%),
    radial-gradient(600px 300px at 0% 100%, rgba(232, 163, 61, 0.12), transparent 60%),
    var(--ink);
  color: var(--paper);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 40px;
  padding: 72px 56px 110px;
  min-height: 520px;
  overflow: hidden;
}

.hero__text {
  position: relative;
  z-index: 1;
  max-width: 500px;
}

.hero__badge {
  display: inline-block;
  font-size: 13px;
  letter-spacing: 0.08em;
  color: var(--wax);
  border: 1px solid rgba(232, 163, 61, 0.45);
  background: rgba(232, 163, 61, 0.1);
  padding: 4px 14px;
  border-radius: 999px;
  margin-bottom: 20px;
}

.hero__headline {
  font-size: 44px;
  line-height: 1.3;
  margin: 0 0 20px;
  color: var(--paper);
}

.hero__shine {
  background: linear-gradient(100deg, #7fd0da 0%, #ffffff 45%, #e8a33d 100%);
  background-size: 200% 100%;
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  animation: shine-text 5s ease-in-out infinite;
}
@keyframes shine-text {
  0%,
  100% {
    background-position: 0% 0;
  }
  50% {
    background-position: 100% 0;
  }
}

.hero__sub {
  font-size: 16px;
  line-height: 1.8;
  color: #b7c4c9;
  margin: 0 0 32px;
  max-width: 38ch;
}

.hero__actions {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.hero__actions :deep(.el-button--large.is-plain) {
  color: var(--paper);
  border-color: #3f5563;
  background: transparent;
}
.hero__actions :deep(.el-button--large.is-plain:hover) {
  border-color: var(--rinse);
  color: var(--rinse);
}

.hero__link {
  color: #b7c4c9;
  text-decoration: none;
  font-size: 15px;
}
.hero__link:hover {
  color: var(--wax);
}

.hero__chips {
  list-style: none;
  display: flex;
  gap: 10px;
  padding: 0;
  margin: 28px 0 0;
  flex-wrap: wrap;
}
.hero__chips li {
  font-size: 13px;
  color: #b7c4c9;
  padding: 4px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.hero__art {
  position: relative;
  z-index: 1;
  flex-shrink: 0;
  width: 460px;
}
.hero__stage {
  padding: 24px 20px 12px;
  border-radius: 24px;
  background: linear-gradient(160deg, rgba(255, 255, 255, 0.07), rgba(255, 255, 255, 0.02));
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.35);
}

.hero__wave {
  position: absolute;
  left: 0;
  bottom: -1px;
  width: 100%;
  height: 54px;
}

/* 往上飄的背景泡泡（Hero 與 CTA 共用） */
.hero__bubbles,
.cta__bubbles {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}
.hero__bubble {
  position: absolute;
  bottom: -40px;
  border-radius: 50%;
  border: 1.5px solid rgba(127, 208, 218, 0.55);
  background: radial-gradient(circle at 30% 30%, rgba(255, 255, 255, 0.35), rgba(63, 179, 194, 0.08));
  opacity: 0;
  animation: float-up linear infinite;
}
@keyframes float-up {
  0% {
    transform: translateY(0) translateX(0);
    opacity: 0;
  }
  10% {
    opacity: 0.8;
  }
  100% {
    transform: translateY(-620px) translateX(18px);
    opacity: 0;
  }
}

/* ---------- Stats ---------- */
.stats {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  padding: 28px 56px 36px;
  flex-wrap: wrap;
}
.stat {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.stat__value {
  font-family: var(--font-display);
  font-size: 24px;
  font-weight: 700;
  color: var(--rinse-deep);
}
.stat__label {
  font-size: 13px;
  color: var(--ink-text-secondary);
}

/* ---------- 區塊共用 ---------- */
.section {
  padding: 64px 56px;
}
.section--tint {
  background: linear-gradient(180deg, #e9f1f2, var(--paper));
}
.section__head {
  max-width: 640px;
  margin-bottom: 36px;
}
.eyebrow {
  display: block;
  font-size: 12px;
  letter-spacing: 0.18em;
  color: var(--rinse);
  font-weight: 700;
  margin-bottom: 8px;
}
.section__head h2 {
  font-size: 30px;
  margin: 0 0 12px;
}
.section__head p {
  margin: 0;
  color: var(--ink-text-secondary);
  line-height: 1.8;
}
.section__more {
  margin-top: 28px;
}

/* ---------- 服務卡 ---------- */
.svc-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}
.svc {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 24px;
  background: #fff;
  border: 1px solid var(--paper-dim);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
  text-decoration: none;
  color: inherit;
  overflow: hidden;
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;
}
.svc::after {
  content: '';
  position: absolute;
  inset: auto 0 0 0;
  height: 3px;
  background: linear-gradient(90deg, var(--rinse), var(--wax));
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.35s ease;
}
.svc:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 28px rgba(20, 32, 43, 0.14);
  border-color: var(--el-color-primary-light-5);
}
.svc:hover::after {
  transform: scaleX(1);
}
.svc--skeleton {
  min-height: 210px;
  background: linear-gradient(100deg, #eef2f2 30%, #f8fafa 50%, #eef2f2 70%);
  background-size: 200% 100%;
  animation: skeleton 1.4s linear infinite;
}
@keyframes skeleton {
  to {
    background-position: -200% 0;
  }
}
.svc__icon {
  width: 52px;
  height: 52px;
  padding: 11px;
  border-radius: 14px;
  color: var(--rinse-deep);
  background: var(--el-color-primary-light-9);
  margin-bottom: 14px;
  box-sizing: border-box;
  transition: background 0.25s ease, color 0.25s ease;
}
.svc:hover .svc__icon {
  background: var(--rinse);
  color: #fff;
}
.svc__tag {
  position: absolute;
  top: 24px;
  right: 24px;
  font-size: 12px;
  color: var(--rinse-deep);
  background: var(--el-color-primary-light-9);
  padding: 2px 10px;
  border-radius: 999px;
}
.svc h3 {
  margin: 0 0 8px;
  font-size: 18px;
}
.svc p {
  margin: 0 0 16px;
  flex: 1;
  color: var(--ink-text-secondary);
  font-size: 14px;
  line-height: 1.7;
}
.svc__meta {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  font-size: 13px;
  color: var(--ink-text-secondary);
}
.svc__meta .price-tag {
  font-size: 20px;
}

/* ---------- 流程 ---------- */
.steps {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
  position: relative;
}
.steps::before {
  content: '';
  position: absolute;
  top: 24px;
  left: 12%;
  right: 12%;
  border-top: 2px dashed var(--el-color-primary-light-5);
}
.step {
  position: relative;
  text-align: center;
}
.step__num {
  width: 48px;
  height: 48px;
  margin: 0 auto 16px;
  border-radius: 50%;
  background: var(--rinse);
  color: #fff;
  font-family: var(--font-display);
  font-size: 20px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 0 0 6px #e9f1f2;
}
.step h3 {
  margin: 0 0 8px;
  font-size: 17px;
}
.step p {
  margin: 0;
  color: var(--ink-text-secondary);
  font-size: 14px;
  line-height: 1.7;
}

/* ---------- 為什麼選擇 ---------- */
.why-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}
.why {
  padding: 24px;
  border-radius: var(--radius-md);
  background: var(--ink);
  color: var(--paper);
  transition: transform 0.25s ease;
}
.why:hover {
  transform: translateY(-4px);
}
.why__icon {
  width: 44px;
  height: 44px;
  color: var(--wax);
  margin-bottom: 14px;
}
.why__icon svg {
  width: 100%;
  height: 100%;
}
.why h3 {
  margin: 0 0 8px;
  font-size: 17px;
  color: var(--paper);
}
.why p {
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
  color: #b7c4c9;
}

/* ---------- CTA ---------- */
.cta {
  position: relative;
  margin: 0 56px 64px;
  padding: 56px 40px;
  border-radius: 20px;
  text-align: center;
  color: var(--paper);
  background: linear-gradient(135deg, var(--rinse-deep), var(--ink) 80%);
  overflow: hidden;
}
.cta h2 {
  position: relative;
  margin: 0 0 10px;
  font-size: 30px;
  color: #fff;
}
.cta p {
  position: relative;
  margin: 0 0 24px;
  color: #cfe0e3;
}
.cta__actions {
  position: relative;
  display: flex;
  justify-content: center;
  gap: 14px;
  flex-wrap: wrap;
}
.cta__actions :deep(.cta__ghost) {
  color: #fff;
  background: transparent;
  border-color: rgba(255, 255, 255, 0.5);
}
.cta__actions :deep(.cta__ghost:hover) {
  border-color: var(--wax);
  color: var(--wax);
}

/* ---------- Footer ---------- */
.footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding: 28px 56px;
  color: var(--ink-text-secondary);
  font-size: 13px;
  border-top: 1px solid var(--paper-dim);
}
.footer__brand {
  display: flex;
  align-items: baseline;
  gap: 10px;
}
.footer__brand strong {
  font-family: var(--font-display);
  font-size: 16px;
  color: var(--ink);
}
.footer__links {
  display: flex;
  gap: 20px;
}
.footer__links a {
  color: var(--ink-text-secondary);
  text-decoration: none;
}
.footer__links a:hover {
  color: var(--rinse);
}

/* ---------- 捲動淡入 ---------- */
.reveal {
  opacity: 0;
  transform: translateY(24px);
  transition: opacity 0.6s ease, transform 0.6s ease;
}
.reveal.is-visible {
  opacity: 1;
  transform: none;
}
/* 卡片自己的 hover transition 要保留，淡入完成後讓 hover 的位移生效 */
.svc.reveal.is-visible,
.why.reveal.is-visible {
  transition: opacity 0.6s ease, transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;
}

@media (max-width: 1000px) {
  .steps,
  .why-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .steps::before {
    display: none;
  }
}

@media (max-width: 860px) {
  .hero {
    flex-direction: column;
    text-align: left;
    padding: 40px 24px 90px;
    min-height: 0; /* 手機不用撐滿 520px，避免上下大片空白 */
    gap: 28px;
  }
  .hero__headline {
    font-size: 34px;
  }
  .hero__art {
    width: 100%;
    max-width: 420px;
    align-self: center;
  }
  .stats,
  .section,
  .footer {
    padding-left: 24px;
    padding-right: 24px;
  }
  .cta {
    margin: 0 24px 48px;
    padding: 40px 20px;
  }
  .section__head h2,
  .cta h2 {
    font-size: 25px;
  }
}

@media (max-width: 560px) {
  .steps,
  .why-grid {
    grid-template-columns: 1fr;
  }
  .step {
    text-align: left;
    display: grid;
    grid-template-columns: 48px 1fr;
    column-gap: 16px;
  }
  .step__num {
    grid-row: span 2;
    margin: 0;
  }
}

@media (prefers-reduced-motion: reduce) {
  .hero__bubble,
  .hero__shine,
  .svc--skeleton {
    animation: none;
  }
  .reveal {
    opacity: 1;
    transform: none;
    transition: none;
  }
}
</style>
