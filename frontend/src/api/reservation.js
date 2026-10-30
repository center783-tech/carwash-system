import request from './request'

export function createReservation(data) {
  // data: { memberId, vehicleId, serviceId, employeeId, reservationDate, timeSlotStart, couponId, remark }
  return request.post('/reservations', data)
}

export function getReservation(id) {
  return request.get(`/reservations/${id}`)
}

export function confirmReservation(id, employeeId) {
  return request.put(`/reservations/${id}/confirm`, null, { params: { employeeId } })
}

// 後台指派（改派）員工；employeeId 傳 null 代表取消指派
export function assignEmployee(id, employeeId) {
  return request.put(`/reservations/${id}/assign`, { employeeId })
}

// 這筆預約目前可指派的員工（在職、會該服務、當天有班、時段不衝突）
export function listAssignableEmployees(id) {
  return request.get(`/reservations/${id}/assignable-employees`)
}

export function cancelReservation(id) {
  return request.put(`/reservations/${id}/cancel`)
}

export function completeReservation(id) {
  return request.put(`/reservations/${id}/complete`)
}

// statuses: 狀態陣列（例如 ['PENDING', 'CONFIRMED']），空陣列/不傳代表不篩選
export function listReservationsByMember(memberId, page = 0, size = 10, statuses = []) {
  const params = { page, size }
  if (statuses.length) params.statuses = statuses.join(',')
  return request.get(`/reservations/member/${memberId}`, { params })
}

export function listReservationsByDateRange(startDate, endDate) {
  return request.get('/reservations/by-date-range', { params: { startDate, endDate } })
}
