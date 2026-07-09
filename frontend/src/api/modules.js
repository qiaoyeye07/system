import api from './index'

// 认证
export const authAPI = {
  register: (data) => api.post('/auth/register', data),
  login: (data) => api.post('/auth/login', data),
  logout: () => api.post('/auth/logout'),
  initAdmin: (data) => api.post('/init', data)
}

// 用户
export const userAPI = {
  getProfile: (id) => api.get(`/users/${id}`),
  getProducts: (id, params) => api.get(`/users/${id}/products`, { params }),
  getRatings: (id) => api.get(`/users/${id}/ratings`)
}

// 分类
export const categoryAPI = {
  getEnabled: () => api.get('/categories'),
  getAll: () => api.get('/admin/categories'),
  create: (data) => api.post('/admin/categories', data),
  update: (id, data) => api.put(`/admin/categories/${id}`, data),
  toggleStatus: (id, data) => api.patch(`/admin/categories/${id}/status`, data)
}

// 商品
export const productAPI = {
  getList: (params) => api.get('/products', { params }),
  search: (params) => api.get('/products/search', { params }),
  getDetail: (id) => api.get(`/products/${id}`),
  create: (formData) => api.post('/products', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
  offShelf: (id) => api.patch(`/products/${id}/off`),
  getMyProducts: (params) => api.get('/my/products', { params })
}

// 订单
export const orderAPI = {
  create: (data) => api.post('/orders', data),
  getMyOrders: (params) => api.get('/orders', { params }),
  getDetail: (id) => api.get(`/orders/${id}`),
  pay: (id) => api.post(`/orders/${id}/pay`),
  ship: (id, data) => api.post(`/orders/${id}/ship`, data),
  receive: (id) => api.post(`/orders/${id}/receive`),
  cancel: (id, data) => api.post(`/orders/${id}/cancel`, data),
  agreeCancel: (id) => api.post(`/orders/${id}/agree-cancel`),
  rejectCancel: (id) => api.post(`/orders/${id}/reject-cancel`),
  refund: (id, data) => api.post(`/orders/${id}/refund`, data),
  agreeRefund: (id) => api.post(`/orders/${id}/refund/agree`),
  rejectRefund: (id) => api.post(`/orders/${id}/refund/reject`),
  escalate: (id, data) => api.post(`/orders/${id}/escalate`, data),
  cancelRefund: (id) => api.post(`/orders/${id}/cancel-refund`)
}

// 交换
export const swapAPI = {
  propose: (data) => api.post('/swap', data),
  agree: (id) => api.post(`/swap/${id}/agree`),
  reject: (id) => api.post(`/swap/${id}/reject`),
  withdraw: (id) => api.post(`/swap/${id}/withdraw`),
  cancel: (id, data) => api.post(`/swap/${id}/cancel`, data),
  ship: (id, data) => api.post(`/swap/${id}/ship`, data),
  receive: (id) => api.post(`/swap/${id}/receive`),
  getMySwaps: (params) => api.get('/swap', { params }),
  getDetail: (id) => api.get(`/swap/${id}`)
}

// 聊天
export const chatAPI = {
  getContacts: () => api.get('/chat/contacts'),
  getMessages: (contactId, params) => api.get(`/chat/conversation/${contactId}`, { params }),
  send: (data) => api.post('/chat/send', data),
  markRead: (contactId, params) => api.patch(`/chat/conversation/${contactId}/read`, null, { params }),
  getUnreadCount: () => api.get('/chat/unread-count')
}

// 评价
export const ratingAPI = {
  submit: (data) => api.post('/ratings', { score: data.score }, { params: { orderId: data.orderId } })
}

// 举报
export const reportAPI = {
  submit: (data) => api.post('/reports', data),
  getMyReports: (params) => api.get('/my/reports', { params }),
  getDetail: (id) => api.get(`/reports/${id}`),
  appeal: (id, data) => api.post(`/reports/${id}/appeal`, data)
}

// 管理端
export const adminAPI = {
  getUsers: (params) => api.get('/admin/users', { params }),
  toggleUserStatus: (id, enabled) => api.put(`/admin/users/${id}/toggle-enabled`, null, { params: { enabled } }),
  getAllOrders: (params) => api.get('/admin/orders', { params }),
  getDisputes: (params) => api.get('/admin/disputes', { params }),
  getDisputeDetail: (id) => api.get(`/admin/orders/${id}`),
  getOrderDetail: (id) => api.get(`/admin/orders/${id}`),
  judgeDispute: (id, data) => api.put(`/admin/disputes/${id}/judge`, data),
  getReports: (params) => api.get('/admin/reports', { params }),
  getReportDetail: (id) => api.get(`/admin/reports/${id}`),
  handleReport: (id, data) => api.put(`/admin/reports/${id}/handle`, data),
  handleAppeal: (id, appealResult) => api.put(`/admin/reports/${id}/handle-appeal`, null, { params: { appealResult } }),
  offProduct: (id, params) => api.put(`/admin/products/${id}/off`, null, { params })
}
