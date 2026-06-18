import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { AUTH_TOKEN_KEY, AUTH_USER_KEY } from '../constants/auth'
import type { AuthUser } from '../types/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(null)
  const user = ref<AuthUser | null>(null)
  const hydrated = ref(false)

  const isAuthenticated = computed(() => Boolean(token.value))

  /**
   * 将登录成功后的认证信息写入 Pinia 和 localStorage。
   */
  function setSession(nextToken: string, nextUser: AuthUser) {
    token.value = nextToken
    user.value = nextUser
    hydrated.value = true

    window.localStorage.setItem(AUTH_TOKEN_KEY, nextToken)
    window.localStorage.setItem(AUTH_USER_KEY, JSON.stringify(nextUser))
  }

  /**
   * 从 localStorage 恢复登录态，供刷新页面后的路由守卫和页面初始化使用。
   */
  function restoreSession() {
    if (hydrated.value) {
      return
    }

    const storedToken = window.localStorage.getItem(AUTH_TOKEN_KEY)
    const storedUser = window.localStorage.getItem(AUTH_USER_KEY)

    if (!storedToken || !storedUser) {
      hydrated.value = true
      return
    }

    try {
      token.value = storedToken
      user.value = JSON.parse(storedUser) as AuthUser
    } catch {
      clearSession()
      return
    }

    hydrated.value = true
  }

  /**
   * 清理所有本地认证信息，用于退出登录或 401 后的会话失效处理。
   */
  function clearSession() {
    token.value = null
    user.value = null
    hydrated.value = true

    window.localStorage.removeItem(AUTH_TOKEN_KEY)
    window.localStorage.removeItem(AUTH_USER_KEY)
  }

  return {
    token,
    user,
    hydrated,
    isAuthenticated,
    setSession,
    restoreSession,
    clearSession,
  }
})
