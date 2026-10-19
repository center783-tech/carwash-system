import request from './request'

export function createReview(data) {
  // data: { reservationId, memberId, rating, comment }
  return request.post('/reviews', data)
}

export function listReviewsByMember(memberId, page = 0, size = 10) {
  return request.get(`/reviews/member/${memberId}`, { params: { page, size } })
}
