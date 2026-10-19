<template>
  <div class="page-container">
    <h2>我的評價</h2>

    <!-- 從預約列表帶 reservationId 過來時，顯示評價表單 -->
    <el-card v-if="reservationId" style="margin-bottom: 16px">
      <h3>為這次服務評分</h3>
      <el-rate v-model="newReview.rating" />
      <el-input
          v-model="newReview.comment"
          type="textarea"
          :rows="3"
          placeholder="分享這次的服務體驗（選填）"
          style="margin: 12px 0"
      />
      <el-button type="primary" :loading="submitting" @click="handleSubmitReview">送出評價</el-button>
    </el-card>

    <el-table :data="reviews" v-loading="loading">
      <el-table-column prop="rating" label="評分" width="100">
        <template #default="{ row }"><el-rate :model-value="row.rating" disabled /></template>
      </el-table-column>
      <el-table-column prop="comment" label="評論" />
      <el-table-column prop="createdAt" label="時間" width="180" />
    </el-table>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createReview, listReviewsByMember } from '@/api/review'
import { useAuthStore } from '@/store/auth'

const route = useRoute()
const authStore = useAuthStore()

const reservationId = route.query.reservationId ? Number(route.query.reservationId) : null
const reviews = ref([])
const loading = ref(false)
const submitting = ref(false)
const newReview = reactive({ rating: 5, comment: '' })

async function fetchReviews() {
  loading.value = true
  try {
    const result = await listReviewsByMember(authStore.userId)
    reviews.value = result.content
  } finally {
    loading.value = false
  }
}

async function handleSubmitReview() {
  submitting.value = true
  try {
    await createReview({
      reservationId,
      memberId: authStore.userId,
      rating: newReview.rating,
      comment: newReview.comment
    })
    ElMessage.success('感謝您的評價')
    fetchReviews()
  } finally {
    submitting.value = false
  }
}

onMounted(fetchReviews)
</script>

<style scoped>
.page-container {
  max-width: 800px;
  margin: 24px auto;
  padding: 0 16px;
}
</style>
