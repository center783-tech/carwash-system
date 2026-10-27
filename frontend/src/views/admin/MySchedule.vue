<template>
  <div>
    <AdminNav />
    <div class="page-container">
      <h2>總班表</h2>
      <ScheduleOverview />
    </div>
  </div>
</template>

<script setup>
import AdminNav from '@/components/AdminNav.vue'
import ScheduleOverview from '@/components/ScheduleOverview.vue'
</script>

<style scoped>
.page-container {
  max-width: 1100px;
  margin: 24px auto;
  padding: 0 16px;
}
</style>
