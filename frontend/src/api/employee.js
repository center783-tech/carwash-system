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

export function updateEmployee(id, data) {
  // data: { name, phone }
  return request.put(`/employees/${id}`, data)
}

// status: 1 啟用、0 停用
export function updateEmployeeStatus(id, status) {
  return request.put(`/employees/${id}/status`, { status })
}

// 一次排多天：dates 為 ['YYYY-MM-DD', ...]，startTime / endTime 為 'HH:mm'
export function createSchedules(employeeId, dates, startTime, endTime) {
  return request.post(`/employees/${employeeId}/schedules/batch`, { workDates: dates, startTime, endTime })
}

export function cancelLeave(employeeId, scheduleId) {
  return request.put(`/employees/${employeeId}/schedules/${scheduleId}/leave/cancel`)
}

export function deleteSchedule(employeeId, scheduleId) {
  return request.delete(`/employees/${employeeId}/schedules/${scheduleId}`)
}

// 總班表：指定日期範圍內所有在職員工的排班（含請假），from / to 為 'YYYY-MM-DD'，最多 62 天
export function listScheduleOverview(from, to) {
  return request.get('/admin/schedules', { params: { from, to } })
}
