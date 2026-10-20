<template>
  <div class="page-container">
    <header class="page-header">
      <h2>洗車服務項目</h2>
      <p>選一項服務，接著就能直接選時段送出預約。</p>
    </header>

    <el-radio-group v-model="category" @change="fetchServices" class="filter">
      <el-radio-button value="">全部</el-radio-button>
      <el-radio-button value="洗車">洗車</el-radio-button>
      <el-radio-button value="美容">美容</el-radio-button>
      <el-radio-button value="保養">保養</el-radio-button>
    </el-radio-group>

    <el-row :gutter="20" v-loading="loading">
      <el-col :span="8" v-for="item in services" :key="item.id" style="margin-bottom: 20px">
        <el-card class="service-card">
          <span v-if="item.category" class="service-card__category">{{ item.category }}</span>
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
      </el-col>
    </el-row>

    <el-empty v-if="!loading && services.length === 0" description="目前沒有符合的服務項目" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listServices } from '@/api/service'

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
.page-header h2 {
  margin: 0 0 6px;
}
.page-header p {
  margin: 0 0 24px;
  color: var(--ink-text-secondary);
}
.filter {
  margin-bottom: 24px;
}
.service-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.service-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.service-card__category {
  display: inline-block;
  font-size: 12px;
  color: var(--rinse-deep);
  background: var(--el-color-primary-light-9);
  padding: 2px 10px;
  border-radius: 999px;
  margin-bottom: 10px;
  width: fit-content;
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
</style>
