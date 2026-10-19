import request from './request'

export function listEmployees() {
  return request.get('/employees')
}

export function getEmployee(id) {
  return request.get(`/employees/${id}`)
}

export function createEmployee(data) {
  return request.post('/employees', data)
}

export function assignServices(id, serviceIds) {
  return request.put(`/employees/${id}/services`, { serviceIds })
}

export function listAvailableEmployees(serviceId) {
  return request.get('/employees/available', { params: { serviceId } })
}

// 排班
export function listSchedules(employeeId) {
  return request.get(`/employees/${employeeId}/schedules`)
}

export function listSchedulesByDate(employeeId, date) {
  return request.get(`/employees/${employeeId}/schedules/by-date`, { params: { date } })
}

export function createSchedule(employeeId, data) {
  // data: { workDate, startTime, endTime }
  return request.post(`/employees/${employeeId}/schedules`, data)
}

export function markLeave(employeeId, scheduleId) {
  return request.put(`/employees/${employeeId}/schedules/${scheduleId}/leave`)
}
