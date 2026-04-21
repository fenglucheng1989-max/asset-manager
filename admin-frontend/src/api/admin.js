import request from './request'

export function login(data) {
  return request.post('/auth/login', data)
}

export function getDashboard() {
  return request.get('/admin/dashboard')
}

export function getUsers(params) {
  return request.get('/admin/users', { params })
}

export function updateUserRole(id, role) {
  return request.put(`/admin/users/${id}/role`, { role })
}

export function getAccounts(params) {
  return request.get('/admin/accounts', { params })
}

export function updateAccount(id, data) {
  return request.put(`/admin/accounts/${id}`, data)
}

export function deleteAccount(id) {
  return request.delete(`/admin/accounts/${id}`)
}
