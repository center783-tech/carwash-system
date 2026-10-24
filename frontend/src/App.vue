<template>
  <el-config-provider :locale="zhTw">
    <NavBar v-if="showNavBar" />
    <router-view />
  </el-config-provider>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import zhTw from 'element-plus/es/locale/lang/zh-tw'
import NavBar from '@/components/NavBar.vue'

const route = useRoute()
// 登入/註冊頁跟後台（有自己的導覽列）不顯示這個會員端 NavBar
const showNavBar = computed(() => !route.meta.hideNavBar)
</script>
