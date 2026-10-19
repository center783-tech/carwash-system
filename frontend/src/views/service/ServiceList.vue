<template>
  <div class="page-container">
    <h2>洗車服務項目</h2>

    <el-radio-group v-model="category" @change="fetchServices" style="margin-bottom: 16px">
      <el-radio-button value="">全部</el-radio-button>
      <el-radio-button value="洗車">洗車</el-radio-button>
      <el-radio-button value="美容">美容</el-radio-button>
      <el-radio-button value="保養">保養</el-radio-button>
    </el-radio-group>

    <el-row :gutter="16" v-loading="loading">
      <el-col :span="8" v-for="item in services" :key="item.id" style="margin-bottom: 16px">
        <el-card>
          <h3>{{ item.name }}</h3>
          <p class="description">{{ item.description }}</p>
          <p>價格：NT$ {{ item.price }}</p>
          <p>預估時長：{{ item.durationMinutes }} 分鐘</p>
          <el-button type="primary" @click="goReserve(item.id)">立即預約</el-button>
        </el-card>
      </el-col>
    </el-row>
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
  max-width: 1000px;
  margin: 24px auto;
  padding: 0 16px;
}
.description {
  color: var(--el-text-color-secondary);
  min-height: 40px;
}
</style>
