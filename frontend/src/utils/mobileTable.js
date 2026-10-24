// 手機版把 el-table 顯示成卡片：Element Plus 的表格不會在儲存格上標示欄位名稱，
// 這裡把表頭文字複製到每個 td 的 data-label，讓 mobile.css 可以用 ::before 顯示。
function labelTables() {
  document.querySelectorAll('.el-table').forEach((table) => {
    const labels = Array.from(
      table.querySelectorAll('.el-table__header-wrapper th .cell')
    ).map((cell) => cell.textContent.trim())

    table.querySelectorAll('.el-table__body-wrapper tbody tr').forEach((tr) => {
      tr.querySelectorAll('td').forEach((td, i) => {
        const label = labels[i] || ''
        if (td.dataset.label !== label) td.dataset.label = label
      })
    })
  })
}

export function setupMobileTable() {
  let scheduled = false
  const observer = new MutationObserver(() => {
    if (scheduled) return
    scheduled = true
    requestAnimationFrame(() => {
      scheduled = false
      labelTables()
    })
  })
  observer.observe(document.body, { childList: true, subtree: true, characterData: true })
  labelTables()
}
