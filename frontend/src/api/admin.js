import request from './request'

export function adminLogin(data) {
  // data: { username, password } -> { token, userId, username, role }
  return request.post('/admin/login', data)
}
