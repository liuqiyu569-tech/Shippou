import { computed, ref } from 'vue'
import {
  getCurrentUserTaskLogs,
  getPersonalTaskLogs,
  getTeamAggregateTaskLogs,
  getTeamTaskLogs,
} from '../api/taskLog'
import { getApiErrorMessage } from '../api/http'
import type {
  LogQueryMode,
  TaskLogObjectType,
  TaskLogOperationType,
  TaskOperationLog,
} from '../types/taskLog'
import type { PageResult } from '../types/team'

export function useTaskOperationLogs(mode: () => LogQueryMode, options?: { pageSize?: number }) {
  const pageSize = options?.pageSize ?? 10

  const loading = ref(false)
  const loadError = ref('')
  const page = ref(1)
  const pageResult = ref<PageResult<TaskOperationLog> | null>(null)

  const filterOperationType = ref<TaskLogOperationType | ''>('')
  const filterObjectType = ref<TaskLogObjectType | ''>('')
  const filterKeyword = ref('')
  const filterStartTime = ref('')
  const filterEndTime = ref('')

  const items = computed(() => pageResult.value?.items ?? [])

  const totalPages = computed(() => {
    if (!pageResult.value) {
      return 1
    }
    return Math.max(1, Math.ceil(pageResult.value.total / pageResult.value.pageSize))
  })

  const isEmpty = computed(
    () => !loading.value && !loadError.value && items.value.length === 0,
  )

  function buildQueryParams() {
    const base = {
      page: page.value,
      pageSize,
      operationType: filterOperationType.value || undefined,
      objectType: filterObjectType.value || undefined,
    }

    const currentMode = mode()
    if (currentMode.kind === 'personal-all' || currentMode.kind === 'team-all') {
      return {
        ...base,
        keyword: filterKeyword.value || undefined,
        startTime: filterStartTime.value || undefined,
        endTime: filterEndTime.value || undefined,
      }
    }

    return base
  }

  async function fetchLogs() {
    loading.value = true
    loadError.value = ''
    try {
      const params = buildQueryParams()
      const currentMode = mode()
      let res

      switch (currentMode.kind) {
        case 'personal-task':
          res = await getPersonalTaskLogs(currentMode.taskId, params)
          break
        case 'personal-all':
          res = await getCurrentUserTaskLogs(params)
          break
        case 'team-task':
          res = await getTeamTaskLogs(currentMode.teamId, currentMode.taskId, params)
          break
        case 'team-all':
          res = await getTeamAggregateTaskLogs(currentMode.teamId, params)
          break
      }

      pageResult.value = res.data ?? null
      if (res.data) {
        page.value = res.data.page
      }
    } catch (e) {
      loadError.value = getApiErrorMessage(e)
      pageResult.value = null
    } finally {
      loading.value = false
    }
  }

  function onFilterChange() {
    page.value = 1
    void fetchLogs()
  }

  function changePage(next: number) {
    if (next < 1 || next > totalPages.value) {
      return
    }
    page.value = next
    void fetchLogs()
  }

  function reset() {
    loading.value = false
    loadError.value = ''
    page.value = 1
    pageResult.value = null
    filterOperationType.value = ''
    filterObjectType.value = ''
    filterKeyword.value = ''
    filterStartTime.value = ''
    filterEndTime.value = ''
  }

  function clearFilters() {
    filterOperationType.value = ''
    filterObjectType.value = ''
    filterKeyword.value = ''
    filterStartTime.value = ''
    filterEndTime.value = ''
    onFilterChange()
  }

  return {
    loading,
    loadError,
    page,
    pageSize,
    pageResult,
    filterOperationType,
    filterObjectType,
    filterKeyword,
    filterStartTime,
    filterEndTime,
    items,
    totalPages,
    isEmpty,
    fetchLogs,
    onFilterChange,
    changePage,
    reset,
    clearFilters,
  }
}
