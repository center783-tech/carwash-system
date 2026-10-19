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

export function cancelReservation(id) {
  return request.put(`/reservations/${id}/cancel`)
}

export function completeReservation(id) {
  return request.put(`/reservations/${id}/complete`)
}

export function listReservationsByMember(memberId, page = 0, size = 10) {
  return request.get(`/reservations/member/${memberId}`, { params: { page, size } })
}

export function listReservationsByDate(date) {
  return request.get('/reservations/by-date', { params: { date } })
}
