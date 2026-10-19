<template>
  <el-config-provider>
    <router-view />
  </el-config-provider>
</template>

<script setup>
// 全域只負責渲染 router-view，版面（導覽列等）交給各自的 Layout 元件處理，
// 方便會員端跟後台管理端用不同的版面。
</script>

<style>
html, body, #app {
  height: 100%;
  margin: 0;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang TC', 'Microsoft JhengHei', Arial, sans-serif;
}
</style>
