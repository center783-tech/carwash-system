import request from './request'

export function listServices(category) {
  return request.get('/services', { params: { category } })
}

export function getService(id) {
  return request.get(`/services/${id}`)
}

// 以下為後台管理用
export function createService(data) {
  return request.post('/services', data)
}

export function updateService(id, data) {
  return request.put(`/services/${id}`, data)
}

export function deleteService(id) {
  return request.delete(`/services/${id}`)
}
