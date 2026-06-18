import type { TeamRole } from '../types/team'

export interface TaskHubActions {
  canViewDetail: boolean
  canEditStatus: boolean
  canEditFull: boolean
  canDelete: boolean
  showGoTeamSpace: boolean
}

export function getTaskHubActions(
  task: { teamId?: number | null },
  teamRoleMap: Record<number, TeamRole>,
): TaskHubActions {
  const canViewDetail = true

  if (!task.teamId) {
    return {
      canViewDetail,
      canEditStatus: false,
      canEditFull: true,
      canDelete: true,
      showGoTeamSpace: false,
    }
  }

  const role = teamRoleMap[task.teamId]

  if (role === 'MEMBER') {
    return {
      canViewDetail,
      canEditStatus: true,
      canEditFull: false,
      canDelete: false,
      showGoTeamSpace: false,
    }
  }

  if (role === 'OWNER' || role === 'ADMIN') {
    return {
      canViewDetail,
      canEditStatus: false,
      canEditFull: false,
      canDelete: false,
      showGoTeamSpace: true,
    }
  }

  // 已离开团队或角色未知：只读 + 可跳转团队空间查看
  return {
    canViewDetail,
    canEditStatus: false,
    canEditFull: false,
    canDelete: false,
    showGoTeamSpace: true,
  }
}
