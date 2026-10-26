import request from './request'

// period: DAY / WEEK / MONTH / QUARTER；date: 'YYYY-MM-DD'，回傳該日期所在的區間（不帶就是今天）
export function getRevenueReport(period, date) {
  return request.get('/admin/reports/revenue', { params: { period, date } })
}
