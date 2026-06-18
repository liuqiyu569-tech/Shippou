import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

export function useAuthHeader() {
  const router = useRouter()
  const authStore = useAuthStore()
  authStore.restoreSession()

  const username = computed(() => authStore.user?.username ?? '未登录用户')
  const userInitial = computed(() => {
    const name = username.value.trim()
    return name ? name.substring(0, 1).toUpperCase() : '?'
  })

  function logout() {
    authStore.clearSession()
    void router.push('/login')
  }

  return { username, userInitial, logout }
}
