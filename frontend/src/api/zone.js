import request from './request'

// 會員預約：指定日期時段下各車位區的剩餘車位 -> [{ id, name, capacity, available }]
export function getZoneAvailability(date, start, serviceId) {
  return request.get('/zones/availability', { params: { date, start, serviceId } })
}

// 後台用
export function listZones() {
  return request.get('/admin/zones')
}

export function createZone(data) {
  // data: { name, capacity, status }
  return request.post('/admin/zones', data)
}

export function updateZone(id, data) {
  return request.put(`/admin/zones/${id}`, data)
}

// 休息日：會員預約用（今天起的休息日清單） -> [{ id, closedDate, reason }]
export function listClosedDates() {
  return request.get('/closed-dates')
}

export function createClosedDate(data) {
  // data: { closedDate: 'YYYY-MM-DD', reason }
  return request.post('/admin/closed-dates', data)
}

export function createClosedDates(dates) {
  // dates: ['YYYY-MM-DD', ...]，一次設定多天，備註之後再逐筆補
  return request.post('/admin/closed-dates/batch', { dates })
}

export function updateClosedDateReason(id, reason) {
  return request.put(`/admin/closed-dates/${id}`, { reason })
}

export function deleteClosedDate(id) {
  return request.delete(`/admin/closed-dates/${id}`)
}
