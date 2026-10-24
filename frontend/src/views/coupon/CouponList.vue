<template>
  <div class="page-container">
    <h2>我的優惠券</h2>

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
            <el-tag size="small" :type="c.isUsed === 1 ? 'info' : 'warning'">
              {{ c.isUsed === 1 ? '已使用' : '可使用' }}
            </el-tag>
          </div>
        </div>
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
.coupon--used {
  opacity: 0.55;
}
.coupon--used .coupon__value {
  background: var(--ink-text-secondary);
}
</style>
