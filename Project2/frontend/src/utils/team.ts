import type { TeamRole, TeamTask } from '../types/team'

export function roleLabel(role: TeamRole) {
  const map: Record<TeamRole, string> = {
    OWNER: '负责人',
    ADMIN: '管理员',
    MEMBER: '成员',
  }
  return map[role] ?? role
}

export function normalizeDueAt(input: string) {
  if (!input.trim()) {
    return undefined
  }
  return input.length === 16 ? `${input}:00` : input
}

export function formatDueAtInput(value?: string | null) {
  if (!value) {
    return ''
  }
  return value.length >= 19 ? value.substring(0, 19) : value.substring(0, 16)
}

export function assigneeNames(task: TeamTask) {
  if (!task.assignees.length) {
    return '未分配'
  }
  return task.assignees.map((a) => a.username).join('、')
}
