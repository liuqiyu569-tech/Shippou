import { reactive, ref } from 'vue'
import axios from 'axios'
import { deleteTask, updateTask } from '../api/task'
import { updateTeamTask } from '../api/team'
import { getApiErrorMessage } from '../api/http'
import { useNoticeStore } from '../stores/notice'
import { getTaskHubActions } from '../utils/taskHubPolicy'
import { formatDueAtInput, normalizeDueAt } from '../utils/team'
import type { Task } from '../types/task'
import type { TeamRole } from '../types/team'

export function useTaskHubListEdit(teamRoleMap: () => Record<number, TeamRole>) {
  const noticeStore = useNoticeStore()

  const showDetail = ref(false)
  const showEdit = ref(false)
  const selectedTask = ref<Task | null>(null)
  const editingTaskId = ref<number | null>(null)
  const editingTaskTeamId = ref<number | null>(null)
  const editingStatusOnly = ref(false)

  const editForm = reactive<{
    title: string
    description: string
    status: Task['status']
    priority: Task['priority']
    dueAtInput: string
  }>({
    title: '',
    description: '',
    status: 'TODO',
    priority: 'MEDIUM',
    dueAtInput: '',
  })

  function actionsFor(task: Task) {
    return getTaskHubActions(task, teamRoleMap())
  }

  function handleViewDetail(task: Task) {
    selectedTask.value = task
    showDetail.value = true
  }

  function handleEdit(task: Task) {
    const actions = actionsFor(task)
    if (!actions.canEditFull && !actions.canEditStatus) {
      return
    }
    editingTaskId.value = task.id
    editingTaskTeamId.value = task.teamId ?? null
    editingStatusOnly.value = actions.canEditStatus && !actions.canEditFull
    editForm.title = task.title
    editForm.description = task.description || ''
    editForm.status = task.status
    editForm.priority = task.priority
    editForm.dueAtInput = formatDueAtInput(task.dueAt)
    showEdit.value = true
  }

  function closeEdit() {
    showEdit.value = false
    editingTaskId.value = null
    editingTaskTeamId.value = null
    editingStatusOnly.value = false
  }

  async function submitEdit(onRefresh: () => void) {
    if (!editingStatusOnly.value && !editForm.title.trim()) {
      noticeStore.show('任务标题不能为空')
      return
    }
    if (!editingStatusOnly.value && editForm.title.length > 100) {
      noticeStore.show('任务标题不能超过 100 个字符')
      return
    }
    if (!editingStatusOnly.value && editForm.description.length > 2000) {
      noticeStore.show('任务描述不能超过 2000 个字符')
      return
    }
    if (!editingTaskId.value) {
      return
    }

    try {
      const payload = editingStatusOnly.value
        ? { status: editForm.status }
        : {
            title: editForm.title,
            description: editForm.description,
            status: editForm.status,
            priority: editForm.priority,
            dueAt: normalizeDueAt(editForm.dueAtInput),
          }
      if (editingTaskTeamId.value) {
        await updateTeamTask(editingTaskTeamId.value, editingTaskId.value, payload)
      } else {
        await updateTask(editingTaskId.value, payload)
      }
      noticeStore.show('修改成功', 'success')
      closeEdit()
      onRefresh()
    } catch (err) {
      const status = axios.isAxiosError(err) ? err.response?.status : undefined
      if (status !== 401 && status !== 403) {
        noticeStore.show(getApiErrorMessage(err))
      }
    }
  }

  async function handleDelete(task: Task, onRefresh: () => void) {
    if (!actionsFor(task).canDelete) {
      return
    }
    if (!confirm('确定删除该任务？')) {
      return
    }
    try {
      await deleteTask(task.id)
      noticeStore.show('删除成功', 'success')
      onRefresh()
    } catch (err) {
      const status = axios.isAxiosError(err) ? err.response?.status : undefined
      if (status !== 401 && status !== 403) {
        noticeStore.show(getApiErrorMessage(err))
      }
    }
  }

  return {
    showDetail,
    showEdit,
    selectedTask,
    editingStatusOnly,
    editForm,
    actionsFor,
    handleViewDetail,
    handleEdit,
    closeEdit,
    submitEdit,
    handleDelete,
  }
}
