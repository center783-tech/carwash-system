import request from './request'

export function listActiveCoupons() {
  return request.get('/coupons')
}

export function listMyCoupons(memberId) {
  return request.get(`/coupons/member/${memberId}`)
}

// 後台用
export function createCoupon(data) {
  return request.post('/coupons', data)
}

export function issueCouponToMember(couponId, memberId) {
  return request.post(`/coupons/${couponId}/issue`, null, { params: { memberId } })
}
