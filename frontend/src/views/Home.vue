<template>
  <div class="home">
    <!-- ===== Hero ===== -->
    <section class="hero">
      <div class="hero__text">
        <h1 class="hero__headline">
          洗去塵囂，<br />
          讓愛車重新發亮。
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
      </div>

      <div class="hero__art" aria-hidden="true">
        <svg viewBox="0 0 440 360" xmlns="http://www.w3.org/2000/svg">
          <!-- 洗車門架 -->
          <rect x="40" y="50" width="18" height="230" rx="9" fill="#2a4350" />
          <rect x="382" y="50" width="18" height="230" rx="9" fill="#2a4350" />
          <rect x="40" y="42" width="360" height="18" rx="9" fill="#2a4350" />

          <!-- 地面 -->
          <line x1="20" y1="300" x2="420" y2="300" stroke="#2a4350" stroke-width="3" stroke-linecap="round" />

          <!-- 水珠 -->
          <g fill="#3fb3c2" opacity="0.9">
            <ellipse cx="140" cy="100" rx="4" ry="7" />
            <ellipse cx="170" cy="130" rx="3" ry="6" />
            <ellipse cx="300" cy="95" rx="4" ry="7" />
            <ellipse cx="260" cy="125" rx="3" ry="6" />
            <ellipse cx="210" cy="80" rx="4" ry="7" />
          </g>

          <!-- 車身 -->
          <path
              d="M150,232 L178,188 Q220,170 262,188 L290,232 Z"
              fill="#f3f6f6"
              stroke="#1c7c8c"
              stroke-width="4"
              stroke-linejoin="round"
          />
          <rect x="110" y="228" width="220" height="50" rx="22" fill="#f3f6f6" stroke="#1c7c8c" stroke-width="4" />
          <circle cx="170" cy="282" r="26" fill="#2a4350" />
          <circle cx="170" cy="282" r="10" fill="#f3f6f6" />
          <circle cx="270" cy="282" r="26" fill="#2a4350" />
          <circle cx="270" cy="282" r="10" fill="#f3f6f6" />

          <!-- 泡泡（蠟光強調色） -->
          <g fill="#e8a33d">
            <circle cx="100" cy="190" r="7" opacity="0.9" />
            <circle cx="340" cy="210" r="5" opacity="0.8" />
            <circle cx="320" cy="160" r="4" opacity="0.7" />
          </g>
        </svg>
      </div>
    </section>

    <!-- ===== 服務亮點 ===== -->
    <section class="stats">
      <div class="stat">
        <span class="stat__value">7</span>
        <span class="stat__label">種服務項目</span>
      </div>
      <div class="stat">
        <span class="stat__value">30 分鐘起</span>
        <span class="stat__label">精緻洗車最快時長</span>
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

    <!-- ===== 關於我們 ===== -->
    <section id="about" class="about">
      <h2 class="about__title">為什麼選擇線上預約</h2>
      <p class="about__lead">
        我們把傳統洗車場常見的現場排隊、打電話問時段、到場才知道要等多久，
        整個搬到線上——選服務、選時段、選車輛，三步驟送出預約。
      </p>

      <div class="features">
        <div class="feature">
          <div class="feature__mark">選</div>
          <div>
            <h3>選好時段再出門</h3>
            <p>即時看到每個時段還有沒有空位，不用賭運氣現場排隊。</p>
          </div>
        </div>
        <div class="feature">
          <div class="feature__mark">付</div>
          <div>
            <h3>線上完成付款</h3>
            <p>信用卡或第三方金流線上結清，到場直接把車開進去。</p>
          </div>
        </div>
        <div class="feature">
          <div class="feature__mark">評</div>
          <div>
            <h3>服務完成後評分</h3>
            <p>每次服務都能留下評價，幫下一位車主做更好的選擇。</p>
          </div>
        </div>
      </div>
    </section>

    <footer class="footer">
      <span>洗享快 © {{ year }}</span>
      <span class="footer__divider"></span>
      <span>洗車場預約管理系統</span>
    </footer>
  </div>
</template>

<script setup>
const year = new Date().getFullYear()
</script>

<style scoped>
.home {
  background: var(--paper);
}

/* ---------- Hero ---------- */
.hero {
  background: var(--ink);
  color: var(--paper);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 40px;
  padding: 72px 56px;
  min-height: 480px;
}

.hero__text {
  max-width: 480px;
}

.hero__headline {
  font-size: 40px;
  line-height: 1.3;
  margin: 0 0 20px;
  color: var(--paper);
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

.hero__art {
  flex-shrink: 0;
  width: 380px;
}
.hero__art svg {
  width: 100%;
  height: auto;
}

/* ---------- Stats ---------- */
.stats {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  padding: 36px 56px;
  border-bottom: 1px solid var(--paper-dim);
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

/* ---------- About ---------- */
.about {
  padding: 64px 56px;
  max-width: 920px;
}

.about__title {
  font-size: 28px;
  margin: 0 0 16px;
}

.about__lead {
  font-size: 16px;
  line-height: 1.8;
  color: var(--ink-text-secondary);
  max-width: 62ch;
  margin: 0 0 40px;
}

.features {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.feature {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.feature__mark {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--rinse);
  color: var(--paper);
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-display);
  font-weight: 700;
}

.feature h3 {
  margin: 4px 0 6px;
  font-size: 17px;
}

.feature p {
  margin: 0;
  color: var(--ink-text-secondary);
  line-height: 1.7;
}

/* ---------- Footer ---------- */
.footer {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 24px 56px;
  color: var(--ink-text-secondary);
  font-size: 13px;
  border-top: 1px solid var(--paper-dim);
}

.footer__divider {
  width: 1px;
  height: 12px;
  background: var(--paper-dim);
}

@media (max-width: 860px) {
  .hero {
    flex-direction: column;
    text-align: left;
    padding: 48px 24px;
  }
  .hero__art {
    width: 260px;
  }
  .stats,
  .about,
  .footer {
    padding-left: 24px;
    padding-right: 24px;
  }
}
</style>
