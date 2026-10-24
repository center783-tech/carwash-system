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
