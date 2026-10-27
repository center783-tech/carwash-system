import request from './request'

// 後台帳號管理（僅 ADMIN）
export function listAccounts() {
  return request.get('/admin/accounts')
}

export function createAccount(data) {
  // data: { username, password, role: 'ADMIN' | 'STAFF' | 'EMPLOYEE' }
  return request.post('/admin/accounts', data)
}

// status: 1 啟用、0 停用
export function updateAccountStatus(id, status) {
  return request.put(`/admin/accounts/${id}/status`, { status })
}
