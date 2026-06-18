import { reactive, ref, watch, type ComputedRef, type Ref } from 'vue'
import axios from 'axios'
import {
  assignTeamTask,
  createTeamTask,
  deleteTeamTask,
  updateTeamTask,
} from '../api/team'
import { getApiErrorMessage } from '../api/http'
import { useNoticeStore } from '../stores/notice'
import { formatDueAtInput, normalizeDueAt } from '../utils/team'
import type { TeamTask } from '../types/team'

function shouldShowLocalApiError(err: unknown): boolean {
  const status = axios.isAxiosError(err) ? err.response?.status : undefined
  return status !== 401 && status !== 403
}

export function useTeamTaskModals(options: {
  teamId: ComputedRef<number>
  reloadAll: () => Promise<void>
  loadError?: Ref<string>
}) {
  const { teamId, reloadAll, loadError } = options
  const noticeStore = useNoticeStore()

  const busy = ref(false)
  const editingTaskId = ref<number | null>(null)
  const assigningTaskId = ref<number | null>(null)
  const memberEditingTaskId = ref<number | null>(null)
  const memberStatusDraft = ref<TeamTask['status']>('TODO')
  const selectedAssigneeIds = ref<number[]>([])
  const showCreate = ref(false)
  const showEdit = ref(false)
  const showAssign = ref(false)
  const showTaskDetail = ref(false)
  const detailTask = ref<TeamTask | null>(null)

  const taskForm = reactive({
    title: '',
    description: '',
    status: 'TODO' as TeamTask['status'],
    priority: 'MEDIUM' as TeamTask['priority'],
    dueAtInput: '',
  })

  function resetTaskForm() {
    taskForm.title = ''
    taskForm.description = ''
    taskForm.status = 'TODO'
    taskForm.priority = 'MEDIUM'
    taskForm.dueAtInput = ''
  }

  function setMemberStatusDraft(status: TeamTask['status']) {
    memberStatusDraft.value = status
  }

  function openTaskDetail(task: TeamTask) {
    detailTask.value = task
    showTaskDetail.value = true
  }

  function closeTaskDetail() {
    showTaskDetail.value = false
    detailTask.value = null
  }

  function openCreate() {
    resetTaskForm()
    showCreate.value = true
  }

  function closeCreate() {
    showCreate.value = false
  }

  async function submitCreate() {
    if (!taskForm.title.trim()) {
      noticeStore.show('任务标题不能为空')
      return
    }
    if (busy.value) {
      return
    }
    busy.value = true
    try {
      await createTeamTask(teamId.value, {
        title: taskForm.title.trim(),
        description: taskForm.description.trim() || undefined,
        status: taskForm.status,
        priority: taskForm.priority,
        dueAt: normalizeDueAt(taskForm.dueAtInput),
      })
      closeCreate()
      await reloadAll()
      noticeStore.show('创建成功', 'success')
    } catch (e) {
      if (shouldShowLocalApiError(e)) {
        noticeStore.show(getApiErrorMessage(e))
      }
    } finally {
      busy.value = false
    }
  }

  function openEdit(task: TeamTask) {
    editingTaskId.value = task.id
    taskForm.title = task.title
    taskForm.description = task.description || ''
    taskForm.status = task.status
    taskForm.priority = task.priority
    taskForm.dueAtInput = formatDueAtInput(task.dueAt)
    showEdit.value = true
  }

  function closeEdit() {
    showEdit.value = false
    editingTaskId.value = null
  }

  async function submitEdit() {
    if (!editingTaskId.value) {
      return
    }
    if (!taskForm.title.trim()) {
      noticeStore.show('任务标题不能为空')
      return
    }
    if (busy.value) {
      return
    }
    busy.value = true
    try {
      await updateTeamTask(teamId.value, editingTaskId.value, {
        title: taskForm.title.trim(),
        description: taskForm.description.trim() || undefined,
        status: taskForm.status,
        priority: taskForm.priority,
        dueAt: normalizeDueAt(taskForm.dueAtInput),
      })
      closeEdit()
      await reloadAll()
      noticeStore.show('修改成功', 'success')
    } catch (e) {
      if (shouldShowLocalApiError(e)) {
        noticeStore.show(getApiErrorMessage(e))
      }
    } finally {
      busy.value = false
    }
  }

  async function removeTask(taskId: number) {
    if (!window.confirm('确定删除该团队任务？')) {
      return
    }
    if (busy.value) {
      return
    }
    busy.value = true
    try {
      await deleteTeamTask(teamId.value, taskId)
      await reloadAll()
      noticeStore.show('删除成功', 'success')
    } catch (e) {
      if (shouldShowLocalApiError(e)) {
        noticeStore.show(getApiErrorMessage(e))
      }
    } finally {
      busy.value = false
    }
  }

  function openAssign(task: TeamTask) {
    assigningTaskId.value = task.id
    selectedAssigneeIds.value = task.assignees
      .map((a) => a.userId ?? a.id)
      .filter((id): id is number => id != null)
    showAssign.value = true
  }

  function closeAssign() {
    showAssign.value = false
    assigningTaskId.value = null
    selectedAssigneeIds.value = []
  }

  async function submitAssign() {
    if (!assigningTaskId.value) {
      return
    }
    if (busy.value) {
      return
    }
    busy.value = true
    try {
      await assignTeamTask(teamId.value, assigningTaskId.value, selectedAssigneeIds.value)
      closeAssign()
      await reloadAll()
      noticeStore.show('分配已更新', 'success')
    } catch (e) {
      if (shouldShowLocalApiError(e)) {
        noticeStore.show(getApiErrorMessage(e))
      }
    } finally {
      busy.value = false
    }
  }

  function startAssignedTaskStatus(task: TeamTask) {
    memberEditingTaskId.value = task.id
    memberStatusDraft.value = task.status
  }

  function cancelAssignedTaskStatus() {
    memberEditingTaskId.value = null
  }

  async function saveAssignedTaskStatus(task: TeamTask) {
    if (busy.value) {
      return
    }
    busy.value = true
    try {
      await updateTeamTask(teamId.value, task.id, { status: memberStatusDraft.value })
      memberEditingTaskId.value = null
      await reloadAll()
      noticeStore.show('状态已更新', 'success')
    } catch (e) {
      if (shouldShowLocalApiError(e)) {
        noticeStore.show(getApiErrorMessage(e))
      }
    } finally {
      busy.value = false
    }
  }

  function closeAllTaskModals() {
    showCreate.value = false
    showEdit.value = false
    showAssign.value = false
    showTaskDetail.value = false
    detailTask.value = null
    editingTaskId.value = null
    assigningTaskId.value = null
    memberEditingTaskId.value = null
    selectedAssigneeIds.value = []
  }

  if (loadError) {
    watch(loadError, (err) => {
      if (err) {
        closeAllTaskModals()
      }
    })
  }

  return reactive({
    busy,
    memberEditingTaskId,
    memberStatusDraft,
    selectedAssigneeIds,
    showCreate,
    showEdit,
    showAssign,
    showTaskDetail,
    detailTask,
    taskForm,
    setMemberStatusDraft,
    openTaskDetail,
    closeTaskDetail,
    openCreate,
    closeCreate,
    submitCreate,
    openEdit,
    closeEdit,
    submitEdit,
    removeTask,
    openAssign,
    closeAssign,
    submitAssign,
    startAssignedTaskStatus,
    cancelAssignedTaskStatus,
    saveAssignedTaskStatus,
    closeAllTaskModals,
  })
}
