<template>
  <div class="page-container">
    <header class="page-header">
      <span class="eyebrow">SERVICES</span>
      <h2>洗車服務項目</h2>
      <p>選一項服務，接著就能直接選時段送出預約。</p>
    </header>

    <el-radio-group v-model="category" @change="fetchServices" class="filter">
      <el-radio-button value="">全部</el-radio-button>
      <el-radio-button value="洗車">洗車</el-radio-button>
      <el-radio-button value="美容">美容</el-radio-button>
      <el-radio-button value="保養">保養</el-radio-button>
    </el-radio-group>

    <div class="grid" v-loading="loading">
      <el-card v-for="item in services" :key="item.id" class="service-card">
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

    <el-empty v-if="!loading && services.length === 0" description="目前沒有符合的服務項目" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
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

@media (prefers-reduced-motion: reduce) {
  .service-card,
  .service-card::after {
    transition: none;
  }
}
</style>
