<template>
  <svg
    class="service-icon"
    viewBox="0 0 48 48"
    fill="none"
    stroke="currentColor"
    stroke-width="2.4"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
  >
    <template v-if="kind === 'foam'">
      <circle cx="17" cy="29" r="9" />
      <circle cx="31" cy="24" r="7" />
      <circle cx="26" cy="38" r="5" />
      <circle cx="36" cy="37" r="3" />
      <circle cx="14" cy="14" r="3" />
    </template>
    <template v-else-if="kind === 'wax'">
      <path d="M22 8 L26 20 L38 24 L26 28 L22 40 L18 28 L6 24 L18 20 Z" />
      <path d="M37 6 v8 M33 10 h8" />
    </template>
    <template v-else-if="kind === 'coating'">
      <path d="M24 5 L39 11 V23 C39 33 32 40 24 43 C16 40 9 33 9 23 V11 Z" />
      <path d="M17 24 l5 5 9-10" />
    </template>
    <template v-else-if="kind === 'interior'">
      <rect x="11" y="6" width="12" height="24" rx="5" transform="rotate(-8 17 18)" />
      <path d="M13 32 h24 q4 0 4 4 v2 h-32 v-2 q0-4 4-4z" />
    </template>
    <template v-else-if="kind === 'engine'">
      <circle cx="24" cy="24" r="7" />
      <path d="M24 6v6 M24 36v6 M6 24h6 M36 24h6 M11 11l4 4 M33 33l4 4 M37 11l-4 4 M15 33l-4 4" />
    </template>
    <template v-else-if="kind === 'detail'">
      <path d="M12 8 h24 l6 10 -18 24 -18 -24 z" />
      <path d="M6 18 h36 M18 18 l6 24 6 -24 M18 18 l-4 -10 M30 18 l4 -10" />
    </template>
    <template v-else>
      <path d="M24 6 C24 6 10 22 10 31 a14 14 0 0 0 28 0 C38 22 24 6 24 6 Z" />
      <path d="M18 32 a6 6 0 0 0 6 6" />
    </template>
  </svg>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  name: { type: String, default: '' },
  category: { type: String, default: '' }
})

// 服務名稱是後台可自行新增的，所以先看名稱關鍵字，再退回分類，都沒有就用水滴
const kind = computed(() => {
  const n = props.name || ''
  if (n.includes('泡沫')) return 'foam'
  if (n.includes('蠟')) return 'wax'
  if (n.includes('鍍膜')) return 'coating'
  if (n.includes('內裝')) return 'interior'
  if (n.includes('引擎')) return 'engine'
  if (n.includes('美容')) return 'detail'
  if (props.category === '美容') return 'wax'
  if (props.category === '保養') return 'engine'
  return 'drop'
})
</script>

<style scoped>
.service-icon {
  width: 100%;
  height: 100%;
  display: block;
}
</style>
