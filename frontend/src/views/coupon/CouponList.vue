<template>
  <div class="page-container">
    <h2>我的優惠券</h2>

    <el-row :gutter="16" v-loading="loading">
      <el-col :span="8" v-for="c in coupons" :key="c.id" style="margin-bottom: 16px">
        <el-card :class="{ used: c.isUsed === 1 }">
          <h3>{{ c.couponCode }}</h3>
          <p>
            {{ c.discountType === 'FIXED' ? `折抵 NT$${c.discountValue}` : `${c.discountValue}% 折扣` }}
          </p>
          <p>有效期限：{{ c.endDate }}</p>
          <el-tag :type="c.isUsed === 1 ? 'info' : 'success'">
            {{ c.isUsed === 1 ? '已使用' : '可使用' }}
          </el-tag>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="!loading && coupons.length === 0" description="目前沒有優惠券" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listMyCoupons } from '@/api/coupon'
import { useAuthStore } from '@/store/auth'

const authStore = useAuthStore()
const coupons = ref([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    coupons.value = await listMyCoupons(authStore.userId)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.page-container {
  max-width: 1000px;
  margin: 24px auto;
  padding: 0 16px;
}
.used {
  opacity: 0.5;
}
</style>
