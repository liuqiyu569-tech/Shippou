import { computed, onMounted, ref, watch, type ComputedRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getTeamDetail, getTeamTaskStats, getTeamTasks } from '../api/team'
import { getApiErrorMessage } from '../api/http'
import { useAuthStore } from '../stores/auth'
import type { MemberInfo, PageResult, TeamRole, TeamTask, TeamTaskQueryParams, TeamTaskStats } from '../types/team'

export function useTeamSpace(
  teamId: ComputedRef<number>,
  callbacks?: {
    onRecovering?: () => void
    onLoadFailed?: () => void
  },
) {
  const route = useRoute()
  const router = useRouter()
  const authStore = useAuthStore()
  authStore.restoreSession()

  const loading = ref(false)
  const loadError = ref('')
  const memberPanelBusy = ref(false)
  const teamInfo = ref({ name: '', createdAt: '' })
  const userRole = ref<TeamRole>('MEMBER')
  const memberList = ref<MemberInfo[]>([])
  const teamTasks = ref<TeamTask[]>([])
  const taskStats = ref<TeamTaskStats | null>(null)
  const statsError = ref('')
  const pageResult = ref<PageResult<TeamTask> | null>(null)
  const page = ref(1)
  const filterStatus = ref('')
  const filterPriority = ref('')
  const filterAssignee = ref('')
  const filterDueStart = ref('')
  const filterDueEnd = ref('')
  const keyword = ref('')

  const PAGE_SIZE = 10

  const currentUserId = computed(() => authStore.user?.id)
  const canManageTasks = computed(() => userRole.value === 'OWNER' || userRole.value === 'ADMIN')
  const totalPages = computed(() => {
    if (!pageResult.value) {
      return 1
    }
    return Math.max(1, Math.ceil(pageResult.value.total / pageResult.value.pageSize))
  })

  function ensureValidTeamId(): boolean {
    if (!Number.isFinite(teamId.value) || teamId.value <= 0) {
      void router.replace({ name: 'my-teams' })
      return false
    }
    return true
  }

  function resetTeamState() {
    teamInfo.value = { name: '', createdAt: '' }
    userRole.value = 'MEMBER'
    memberList.value = []
    teamTasks.value = []
    taskStats.value = null
    statsError.value = ''
    pageResult.value = null
  }

  function isAssignedToMe(task: TeamTask) {
    const uid = currentUserId.value
    if (uid == null) {
      return false
    }
    return task.assignees.some((a) => (a.userId ?? a.id) === uid)
  }

  async function loadTeamDetail() {
    const res = await getTeamDetail(teamId.value)
    const detail = res.data
    teamInfo.value = { name: detail.name, createdAt: detail.createdAt }
    userRole.value = detail.myRole
    memberList.value = detail.members
  }

  async function fetchTeamTasks(targetPage = page.value, options?: { silent?: boolean }) {
    if (!options?.silent) {
      loading.value = true
      loadError.value = ''
    }
    try {
      const params: TeamTaskQueryParams = {
        status: filterStatus.value || undefined,
        priority: filterPriority.value || undefined,
        assigneeId: filterAssignee.value ? Number(filterAssignee.value) : undefined,
        dueStart: filterDueStart.value || undefined,
        dueEnd: filterDueEnd.value || undefined,
        keyword: keyword.value.trim() || undefined,
        page: targetPage,
        pageSize: PAGE_SIZE,
      }
      const res = await getTeamTasks(teamId.value, params)
      const result = res.data
      if (result && result.items.length === 0 && result.total > 0 && targetPage > 1) {
        await fetchTeamTasks(1, options)
        return
      }
      teamTasks.value = result?.items ?? []
      pageResult.value = result ?? null
      if (result) {
        page.value = result.page
      }
    } catch (e) {
      loadError.value = getApiErrorMessage(e)
      teamTasks.value = []
      pageResult.value = null
    } finally {
      if (!options?.silent) {
        loading.value = false
      }
    }
  }

  async function fetchTaskStats(options?: { silent?: boolean }) {
    if (!options?.silent) {
      statsError.value = ''
    }
    try {
      const res = await getTeamTaskStats(teamId.value)
      taskStats.value = res.data
    } catch (e) {
      taskStats.value = null
      statsError.value = getApiErrorMessage(e)
    }
  }

  function onFilterChange() {
    page.value = 1
    void fetchTeamTasks(1)
  }

  function clearKeyword() {
    keyword.value = ''
    page.value = 1
    void fetchTeamTasks(1)
  }

  function changePage(next: number) {
    if (next < 1 || next > totalPages.value) {
      return
    }
    page.value = next
    void fetchTeamTasks(next)
  }

  async function reloadAll() {
    const recovering = !!loadError.value
    loading.value = true
    loadError.value = ''
    if (recovering) {
      callbacks?.onRecovering?.()
    }
    try {
      await Promise.all([
        loadTeamDetail(),
        fetchTeamTasks(page.value, { silent: true }),
        fetchTaskStats({ silent: true }),
      ])
    } catch (e) {
      loadError.value = getApiErrorMessage(e)
      resetTeamState()
      callbacks?.onLoadFailed?.()
    } finally {
      loading.value = false
    }
  }

  function goBack() {
    void router.push({ name: 'my-teams' })
  }

  watch(
    () => route.params.teamId,
    () => {
      if (ensureValidTeamId()) {
        void reloadAll()
      }
    },
  )

  onMounted(() => {
    if (ensureValidTeamId()) {
      void reloadAll()
    }
  })

  return {
    loading,
    loadError,
    memberPanelBusy,
    teamInfo,
    userRole,
    memberList,
    teamTasks,
    taskStats,
    statsError,
    page,
    pageResult,
    filterStatus,
    filterPriority,
    filterAssignee,
    filterDueStart,
    filterDueEnd,
    keyword,
    canManageTasks,
    totalPages,
    fetchTeamTasks,
    fetchTaskStats,
    onFilterChange,
    clearKeyword,
    changePage,
    reloadAll,
    resetTeamState,
    isAssignedToMe,
    goBack,
  }
}
