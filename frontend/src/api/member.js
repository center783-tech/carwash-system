import request from './request'

export function register(data) {
  // data: { username, password, email, phone, realName }
  return request.post('/members/register', data)
}

export function login(data) {
  // data: { username, password } -> 回傳 { token, userId, username, role }
  return request.post('/members/login', data)
}

// 忘記帳號 / 忘記密碼（公開端點）：寄信一律回成功，不透露 Email 是否已註冊
export function forgotUsername(email) {
  return request.post('/members/forgot-username', { email })
}

export function recoverUsername(token) {
  // 憑信件連結上的 token 取得帳號（回傳字串），token 只能用一次
  return request.post('/members/recover-username', { token })
}

export function forgotPassword(email) {
  return request.post('/members/forgot-password', { email })
}

export function resetPassword(token, newPassword) {
  return request.post('/members/reset-password', { token, newPassword })
}

export function getMember(id) {
  return request.get(`/members/${id}`)
}

export function updateProfile(id, data) {
  // data: { realName, phone }
  return request.put(`/members/${id}`, data)
}

export function sendPasswordCode(id) {
  // 寄 6 位數驗證碼到會員註冊的 Gmail / Email（5 分鐘有效，60 秒內不可重寄）
  return request.post(`/members/${id}/password/code`)
}

export function changePassword(id, data) {
  // data: { oldPassword, newPassword, verificationCode }
  return request.put(`/members/${id}/password`, data)
}

// 車輛管理（掛在會員底下）
export function listVehicles(memberId) {
  return request.get(`/members/${memberId}/vehicles`)
}

export function addVehicle(memberId, data) {
  // data: { plateNumber, brand, model, color, carType }
  return request.post(`/members/${memberId}/vehicles`, data)
}

export function updateVehicle(memberId, vehicleId, data) {
  return request.put(`/members/${memberId}/vehicles/${vehicleId}`, data)
}

export function deleteVehicle(memberId, vehicleId) {
  return request.delete(`/members/${memberId}/vehicles/${vehicleId}`)
}
