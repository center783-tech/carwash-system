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

// 到店出示：開始 10 分鐘倒數，時間到自動變成已使用；已在出示中再呼叫會沿用原倒數
export function redeemCoupon(memberCouponId, memberId) {
  return request.post(`/coupons/member-coupons/${memberCouponId}/redeem`, null, { params: { memberId } })
}

// 後台：分頁查詢會員（姓名、電話、Email），keyword 可搜尋帳號/姓名/電話/Email
export function listMembersForAdmin(params) {
  // params: { keyword, page, size } -> { content, totalElements, last }
  return request.get('/admin/members', { params })
}

// 後台：勾選多位會員一次發放 -> { issued, emailFailed }
export function issueCouponToMembers(couponId, memberIds) {
  return request.post(`/coupons/${couponId}/issue-batch`, { memberIds })
}
