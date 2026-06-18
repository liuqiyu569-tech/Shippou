import axios, { AxiosError } from 'axios'
import router from '../router'
import { AUTHORIZATION_SCHEME } from '../constants/auth'
import { pinia } from '../stores'
import { useAuthStore } from '../stores/auth'
import { useNoticeStore } from '../stores/notice'
import type { ApiResponse } from '../types/api'

const http = axios.create({
  // 修复：使用相对路径 /api，走 Vite 代理，不再直接访问 8080
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
  timeout: 10000,
})

function isAuthRequest(url?: string) {
  return Boolean(url?.includes('/v1/auth/login') || url?.includes('/v1/auth/register'))
}

export function getApiErrorMessage(error: unknown) {
  if (axios.isAxiosError<ApiResponse<null>>(error)) {
    return error.response?.data?.message || '请求失败，请稍后再试'
  }

  return '请求失败，请稍后再试'
}

http.interceptors.request.use((config) => {
  const authStore = useAuthStore(pinia)
  authStore.restoreSession()

  if (authStore.token) {
    config.headers.Authorization = `${AUTHORIZATION_SCHEME} ${authStore.token}`
  }

  return config
})

http.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiResponse<null>>) => {
    const authStore = useAuthStore(pinia)
    const noticeStore = useNoticeStore(pinia)
    const status = error.response?.status
    const requestUrl = error.config?.url

    if (!error.response) {
      if (!isAuthRequest(requestUrl)) {
        noticeStore.show('网络连接失败，请检查后端服务是否启动')
      }

      return Promise.reject(error)
    }

    if (status === 401 && !isAuthRequest(requestUrl)) {
      authStore.clearSession()
      noticeStore.show('登录已失效，请重新登录')

      if (router.currentRoute.value.name !== 'login') {
        await router.push({
          name: 'login',
          query: { redirect: router.currentRoute.value.fullPath },
        })
      }
    }

    if (status === 403) {
      noticeStore.show('无权限执行该操作')
    }

    return Promise.reject(error)
  },
)

export default http