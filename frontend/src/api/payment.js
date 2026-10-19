import request from './request'

export function createPayment(reservationId, method) {
  return request.post('/payments', null, { params: { reservationId, method } })
}

export function listPaymentsByReservation(reservationId) {
  return request.get(`/payments/reservation/${reservationId}`)
}
