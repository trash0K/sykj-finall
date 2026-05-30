import axios from 'axios'
import {
  companies,
  departments,
  employees,
  businessTypes,
  cities,
  projects,
  costAttributions
} from '../mock/data'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000
})

api.interceptors.response.use(
  response => {
    const res = response.data
    if (res && res.code === 200) {
      return res.data
    }
    const msg = (res && (res.msg || res.message)) || '请求失败'
    return Promise.reject(new Error(msg))
  },
  error => {
    const data = error.response?.data
    const msg = (data && (data.msg || data.message)) || error.message || '网络请求失败'
    return Promise.reject(new Error(msg))
  }
)

/**
 * 详情接口返回的 subsidies + calendars 已经是扁平结构，直接透传
 */
function flattenDetail(d) {
  return {
    main: d?.main || {},
    itineraries: d?.itineraries || [],
    subsidies: d?.subsidies || [],
    calendars: d?.calendars || [],
    allocations: d?.allocations || []
  }
}

export const reimApi = {
  list: (query = {}) => {
    const params = {
      page: query.current || query.page || 1,
      size: query.size || 10,
      id: query.id || query.reimbursementNo || undefined,
      reimbursementTitle: query.reimbursementTitle || query.title || undefined,
      reimburserName: query.reimburserName || undefined,
      reimDepartmentName: query.reimDepartmentName || undefined,
      reimCompanyName: query.reimCompanyName || undefined,
      businessTripReason: query.businessTripReason || query.reason || undefined,
      businessTypeName: query.businessTypeName || undefined
    }
    return api.get('/reimbursement/list', { params })
  },
  detail: (id) => api.get(`/reimbursement/${id}`).then(flattenDetail),
  save: (data) => api.post('/reimbursement/save', data),
  update: (data) => api.put('/reimbursement/update', data),
  delete: (id) => api.delete(`/reimbursement/${id}`),
  copy: (id) => api.post(`/reimbursement/${id}/copy`)
}

/**
 * 字典接口：后端未提供，使用前端预置数据
 */
export const dictApi = {
  companies: () => Promise.resolve(companies),
  departments: () => Promise.resolve(departments),
  employees: () => Promise.resolve(employees),
  businessTypes: () => Promise.resolve(businessTypes),
  cities: () => Promise.resolve(cities),
  projects: () => Promise.resolve(projects),
  attributions: () => Promise.resolve(costAttributions)
}
