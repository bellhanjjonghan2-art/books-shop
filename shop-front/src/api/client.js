import axios from 'axios'
import { useAuthStore } from '../stores/useAuthStore'

export const api = axios.create({
  baseURL: '/api',
  timeout: 10_000,
})

// 요청마다 토큰 자동 첨부
api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 401 응답 시 인증 상태 초기화 후 로그인 페이지로 이동
api.interceptors.response.use(
  (res) => res,
  (error) => {
    if (error.response?.status === 401) {
      useAuthStore.getState().logout()
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  },
)
