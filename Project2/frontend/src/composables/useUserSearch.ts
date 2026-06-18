import axios from 'axios'
import { ref } from 'vue'
import { getApiErrorMessage } from '../api/http'
import { queryUsers } from '../api/user'
import { useNoticeStore } from '../stores/notice'
import type { UserBrief } from '../types/user'

export type PickedUser = { id: number; username: string }

const PAGE_SIZE = 10

export function useUserSearch() {
  const noticeStore = useNoticeStore()

  const searchInput = ref('')
  const users = ref<UserBrief[]>([])
  const searchPage = ref(1)
  const listLoading = ref(false)
  const listError = ref('')
  const hasMore = ref(false)
  const picked = ref<PickedUser[]>([])

  let debounceTimer: ReturnType<typeof setTimeout> | undefined
  let userListRequestId = 0

  function isPicked(userId: number) {
    return picked.value.some((p) => p.id === userId)
  }

  function onUserCheck(u: UserBrief, ev: Event, options?: { blocked?: (userId: number) => boolean }) {
    if (options?.blocked?.(u.id)) {
      return
    }
    const el = ev.target as HTMLInputElement
    if (el.checked) {
      if (!isPicked(u.id)) {
        picked.value = [...picked.value, { id: u.id, username: u.username }]
      }
    } else {
      picked.value = picked.value.filter((p) => p.id !== u.id)
    }
  }

  function removePicked(id: number) {
    picked.value = picked.value.filter((p) => p.id !== id)
  }

  function reset() {
    searchInput.value = ''
    users.value = []
    searchPage.value = 1
    listError.value = ''
    hasMore.value = false
    picked.value = []
    if (debounceTimer) {
      window.clearTimeout(debounceTimer)
      debounceTimer = undefined
    }
  }

  async function loadUsers(replace: boolean) {
    const my = ++userListRequestId
    listLoading.value = true
    listError.value = ''
    const keyword = searchInput.value.trim()
    try {
      const res = await queryUsers(keyword || undefined, searchPage.value, PAGE_SIZE)
      if (my !== userListRequestId) {
        return
      }
      const items = res.data.items
      if (replace) {
        users.value = items
      } else {
        const seen = new Set(users.value.map((u) => u.id))
        for (const u of items) {
          if (!seen.has(u.id)) {
            seen.add(u.id)
            users.value.push(u)
          }
        }
      }
      const total = res.data.total
      const loadedThrough = searchPage.value * PAGE_SIZE
      hasMore.value = loadedThrough < total
    } catch (e) {
      if (my !== userListRequestId) {
        return
      }
      listError.value = getApiErrorMessage(e)
      if (replace) {
        users.value = []
      }
      const status = axios.isAxiosError(e) ? e.response?.status : undefined
      if (status !== 401 && status !== 403) {
        noticeStore.show(listError.value)
      }
    } finally {
      if (my === userListRequestId) {
        listLoading.value = false
      }
    }
  }

  function scheduleSearch() {
    if (debounceTimer) {
      window.clearTimeout(debounceTimer)
    }
    debounceTimer = window.setTimeout(() => {
      debounceTimer = undefined
      searchPage.value = 1
      void loadUsers(true)
    }, 300)
  }

  async function loadNextPage() {
    if (!hasMore.value || listLoading.value) {
      return
    }
    searchPage.value += 1
    await loadUsers(false)
  }

  function initOnOpen() {
    reset()
    void loadUsers(true)
  }

  return {
    searchInput,
    users,
    picked,
    listLoading,
    listError,
    hasMore,
    isPicked,
    onUserCheck,
    removePicked,
    scheduleSearch,
    loadNextPage,
    reset,
    initOnOpen,
  }
}
