import { onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute } from 'vue-router'

// 各頁內容寬度不一（600～1100px），讓導覽列左右兩端的按鈕對齊「下方頁面內容」的左右邊緣：
// 量出頁面主要區塊（.page-container / 首頁 hero）的內容區，換算成導覽列的左右 padding。
const TARGETS = '.page-container, .hero'

export function useAlignNavWithPage(navRef) {
  const route = useRoute()
  const timers = []

  function align() {
    const nav = navRef.value
    if (!nav) return
    const target = document.querySelector(TARGETS)
    if (!target) {
      nav.style.removeProperty('--nav-left')
      nav.style.removeProperty('--nav-right')
      return
    }
    const navRect = nav.getBoundingClientRect()
    const rect = target.getBoundingClientRect()
    const style = getComputedStyle(target)
    const left = rect.left + parseFloat(style.paddingLeft) - navRect.left
    const right = navRect.right - (rect.right - parseFloat(style.paddingRight))
    nav.style.setProperty('--nav-left', `${Math.max(left, 0)}px`)
    nav.style.setProperty('--nav-right', `${Math.max(right, 0)}px`)
  }

  // 頁面元件是非同步載入的，路由切換後多量幾次，等內容渲染完再對齊
  function alignSoon() {
    timers.forEach(clearTimeout)
    timers.length = 0
    ;[0, 100, 300, 800].forEach((ms) => timers.push(setTimeout(align, ms)))
  }

  onMounted(() => {
    alignSoon()
    window.addEventListener('resize', align)
  })
  onBeforeUnmount(() => {
    timers.forEach(clearTimeout)
    window.removeEventListener('resize', align)
  })
  watch(() => route.fullPath, alignSoon)
}
