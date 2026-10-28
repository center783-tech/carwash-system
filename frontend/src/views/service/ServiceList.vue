services.value = await listServices()<template>
  <div class="page-container">
    <header class="page-header">
      <span class="eyebrow">SERVICES</span>
      <h2>洗車服務項目</h2>
      <p>選一項服務，接著就能直接選時段送出預約。</p>
    </header>

    <!-- 組合優惠：放最上面、不受分類篩選影響，讓會員一進來就看到 -->
    <section v-if="bundles.length" class="deals">
      <div class="deals__title"><span class="deals__fire">組合優惠</span><small>多項一起做，比單點更划算</small></div>
      <div class="deals__grid">
        <div v-for="b in bundles" :key="b.id" class="deal">
          <div v-if="saving(b) > 0" class="deal__ribbon">省 NT$ {{ saving(b) }}</div>
          <h3>{{ b.name }}</h3>
          <div class="deal__items"><span v-for="n in childNames(b)" :key="n" class="deal__chip">{{ n }}</span></div>
          <p v-if="b.description" class="deal__desc">{{ b.description }}</p>
          <div class="deal__bottom">
            <div class="deal__prices">
              <span v-if="b.originalPrice" class="deal__old">NT$ {{ b.originalPrice }}</span>
              <span class="deal__now">NT$ {{ b.price }}</span>
              <span class="deal__time">約 {{ b.durationMinutes }} 分鐘</span>
            </div>
            <el-button type="warning" round @click="goReserve(b.id)">立即預約</el-button>
          </div>
        </div>
      </div>
    </section>

    <el-radio-group v-model="category" class="filter">
      <el-radio-button value="">全部</el-radio-button>
      <el-radio-button value="洗車">洗車</el-radio-button>
      <el-radio-button value="美容">美容</el-radio-button>
      <el-radio-button value="保養">保養</el-radio-button>
    </el-radio-group>

    <div class="grid" v-loading="loading">
      <el-card v-for="item in shownServices" :key="item.id" class="service-card">
        <div class="service-card__top">
          <div class="service-card__icon"><ServiceIcon :name="item.name" :category="item.category" /></div>
          <span v-if="item.category" class="service-card__category">{{ item.category }}</span>
        </div>
        <h3>{{ item.name }}</h3>
        <p class="description">{{ item.description }}</p>
        <div class="service-card__meta">
          <span class="price-tag">NT$ {{ item.price }}</span>
          <span class="duration">約 {{ item.durationMinutes }} 分鐘</span>
        </div>
        <el-button type="primary" round style="width: 100%" @click="goReserve(item.id)">
          立即預約
        </el-button>
      </el-card>
    </div>

    <el-empty v-if="!loading && shownServices.length === 0 && bundles.length === 0" description="目前沒有符合的服務項目" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listServices } from '@/api/service'
import ServiceIcon from '@/components/ServiceIcon.vue'

const router = useRouter()
const services = ref([])
const category = ref('')
const loading = ref(false)

async function fetchServices() {
  loading.value = true
  try {
    services.value = await listServices(category.value || undefined)
  } finally {
    loading.value = false
  }
}

const bundles = computed(() => services.value.filter((s) => s.bundle))
const shownServices = computed(() =>
  services.value.filter((s) => !s.bundle && (!category.value || s.category === category.value))
)

function saving(b) {
  return b.originalPrice ? Math.round(b.originalPrice - b.price) : 0
}

function childNames(b) {
  return (b.bundleItemIds || []).map((id) => services.value.find((s) => s.id === id)?.name).filter(Boolean)
}

function goReserve(serviceId) {
  router.push({ path: '/reservations/create', query: { serviceId } })
}

onMounted(fetchServices)
</script>

<style scoped>
.page-container {
  max-width: 1040px;
  margin: 0 auto;
  padding: 40px 24px 64px;
}
.page-header {
  position: relative;
  margin-bottom: 24px;
  padding: 28px 28px 24px;
  border-radius: var(--radius-md);
  color: var(--paper);
  background:
    radial-gradient(500px 200px at 100% 0%, rgba(63, 179, 194, 0.35), transparent 60%),
    var(--ink);
  overflow: hidden;
}
.page-header::after {
  /* 右下角的泡泡裝飾 */
  content: '';
  position: absolute;
  right: 28px;
  bottom: -26px;
  width: 90px;
  height: 90px;
  border-radius: 50%;
  border: 2px solid rgba(127, 208, 218, 0.4);
  box-shadow: -44px -24px 0 -22px rgba(127, 208, 218, 0.3), -84px 8px 0 -30px rgba(232, 163, 61, 0.4);
}
.eyebrow {
  display: block;
  font-size: 12px;
  letter-spacing: 0.18em;
  color: var(--wax);
  margin-bottom: 6px;
}
.page-header h2 {
  margin: 0 0 6px;
  color: #fff;
}
.page-header p {
  margin: 0;
  color: #b7c4c9;
}
.filter {
  margin-bottom: 24px;
}
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(290px, 1fr));
  gap: 20px;
  min-height: 120px;
}
.service-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}
.service-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 28px rgba(20, 32, 43, 0.14) !important;
}
.service-card::after {
  content: '';
  position: absolute;
  inset: auto 0 0 0;
  height: 3px;
  background: linear-gradient(90deg, var(--rinse), var(--wax));
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.35s ease;
}
.service-card:hover::after {
  transform: scaleX(1);
}
.service-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  box-sizing: border-box;
}
.service-card__top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 14px;
}
.service-card__icon {
  width: 52px;
  height: 52px;
  padding: 11px;
  box-sizing: border-box;
  border-radius: 14px;
  color: var(--rinse-deep);
  background: var(--el-color-primary-light-9);
  transition: background 0.25s ease, color 0.25s ease;
}
.service-card:hover .service-card__icon {
  background: var(--rinse);
  color: #fff;
}
.service-card__category {
  font-size: 12px;
  color: var(--rinse-deep);
  background: var(--el-color-primary-light-9);
  padding: 2px 10px;
  border-radius: 999px;
}
.service-card h3 {
  margin: 0 0 8px;
  font-size: 17px;
}
.description {
  color: var(--ink-text-secondary);
  min-height: 44px;
  font-size: 14px;
  line-height: 1.6;
  flex: 1;
  margin: 0;
}
.service-card__meta {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin: 12px 0 16px;
}
.service-card__meta .price-tag {
  font-size: 20px;
}
.duration {
  font-size: 13px;
  color: var(--ink-text-secondary);
}

.deals {
  margin-bottom: 32px;
}
.deals__title {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 14px;
}
.deals__fire {
  font-size: 22px;
  font-weight: 800;
  color: #e6522c;
}
.deals__title small {
  color: var(--ink-text-secondary);
}
.deals__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}
.deal {
  position: relative;
  padding: 22px 20px 18px;
  border-radius: var(--radius-md);
  border: 2px solid #f0a53a;
  background: linear-gradient(135deg, #fff7e8, #ffeccc);
  box-shadow: 0 8px 24px rgba(230, 82, 44, 0.15);
}
.deal__ribbon {
  position: absolute;
  top: -2px;
  right: 16px;
  padding: 6px 12px 8px;
  border-radius: 0 0 10px 10px;
  background: #e6522c;
  color: #fff;
  font-weight: 700;
  font-size: 14px;
}
.deal h3 {
  margin: 0 70px 10px 0;
  font-size: 19px;
  color: #14202b;
}
.deal__items {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}
.deal__chip {
  padding: 2px 10px;
  border-radius: 999px;
  background: #fff;
  border: 1px solid #f0a53a;
  color: #8a5a00;
  font-size: 13px;
}
.deal__desc {
  margin: 0 0 10px;
  font-size: 14px;
  color: #5a4a2a;
}
.deal__bottom {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 12px;
}
.deal__prices {
  display: flex;
  flex-direction: column;
}
.deal__old {
  text-decoration: line-through;
  color: #909399;
  font-size: 14px;
}
.deal__now {
  font-size: 28px;
  font-weight: 800;
  color: #e6522c;
  line-height: 1.1;
}
.deal__time {
  font-size: 12px;
  color: #8a7a5a;
}

@media (prefers-reduced-motion: reduce) {
  .service-card,
  .service-card::after {
    transition: none;
  }
}
</style>
