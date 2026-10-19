import request from './request'

export function register(data) {
  // data: { username, password, email, phone, realName }
  return request.post('/members/register', data)
}

export function login(data) {
  // data: { username, password } -> 回傳 { token, userId, username, role }
  return request.post('/members/login', data)
}

export function getMember(id) {
  return request.get(`/members/${id}`)
}

export function updateProfile(id, data) {
  // data: { realName, phone }
  return request.put(`/members/${id}`, data)
}

export function changePassword(id, data) {
  // data: { oldPassword, newPassword }
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
