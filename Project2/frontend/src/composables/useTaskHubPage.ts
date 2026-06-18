import { computed, onActivated, onMounted, ref } from 'vue'
import { getTaskList } from '../api/task'
import { getMyTeams } from '../api/team'
import { getApiErrorMessage } from '../api/http'
import { useNoticeStore } from '../stores/notice'
import type { Task } from '../types/task'
import type { PageResult, TeamRole } from '../types/team'

export function useTaskHubPage() {
  const noticeStore = useNoticeStore()

  const taskList = ref<Task[]>([])
  const showForm = ref(false)
  const teamRoleMap = ref<Record<number, TeamRole>>({})

  const scope = ref<'all' | 'personal' | 'team'>('all')
  const filterStatus = ref('')
  const filterPriority = ref('')
  const filterDueStart = ref('')
  const filterDueEnd = ref('')
  const keyword = ref('')

  const page = ref(1)
  const pageSize = 10
  const pageResult = ref<PageResult<Task> | null>(null)
  const loadError = ref('')
  const loading = ref(false)

  const totalPages = computed(() => {
    if (!pageResult.value) {
      return 1
    }
    return Math.max(1, Math.ceil(pageResult.value.total / pageResult.value.pageSize))
  })

  async function fetchTaskList() {
    loading.value = true
    loadError.value = ''
    try {
      const res = await getTaskList({
        scope: scope.value,
        status: filterStatus.value || undefined,
        priority: filterPriority.value || undefined,
        dueStart: filterDueStart.value || undefined,
        dueEnd: filterDueEnd.value || undefined,
        keyword: keyword.value.trim() || undefined,
        page: page.value,
        pageSize,
      })
      taskList.value = res.data?.items ?? []
      pageResult.value = res.data ?? null
      if (res.data) {
        page.value = res.data.page
      }
    } catch (e) {
      loadError.value = getApiErrorMessage(e)
      taskList.value = []
      pageResult.value = null
    } finally {
      loading.value = false
    }
  }

  async function fetchTeamRoles() {
    const res = await getMyTeams(1, 200)
    const nextMap: Record<number, TeamRole> = {}
    for (const team of res.data.items) {
      nextMap[team.id] = team.role
    }
    teamRoleMap.value = nextMap
  }

  async function refreshPageData() {
    loadError.value = ''
    try {
      await fetchTeamRoles()
    } catch (e) {
      noticeStore.show(getApiErrorMessage(e))
      teamRoleMap.value = {}
    }
    await fetchTaskList()
  }

  function selectScope(type: 'all' | 'personal' | 'team') {
    scope.value = type
    page.value = 1
    void fetchTaskList()
  }

  function onFilterChange() {
    page.value = 1
    void fetchTaskList()
  }

  function clearKeyword() {
    keyword.value = ''
    page.value = 1
    void fetchTaskList()
  }

  function changePage(next: number) {
    if (next < 1 || next > totalPages.value) {
      return
    }
    page.value = next
    void fetchTaskList()
  }

  onMounted(() => {
    void refreshPageData()
  })

  onActivated(() => {
    void refreshPageData()
  })

  return {
    taskList,
    showForm,
    teamRoleMap,
    scope,
    filterStatus,
    filterPriority,
    filterDueStart,
    filterDueEnd,
    keyword,
    page,
    pageResult,
    loadError,
    loading,
    totalPages,
    fetchTaskList,
    selectScope,
    onFilterChange,
    clearKeyword,
    changePage,
  }
}
