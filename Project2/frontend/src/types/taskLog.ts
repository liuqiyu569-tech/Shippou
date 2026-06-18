import type { UserBrief } from './user'

export type TaskLogOperationType = 'CREATE' | 'UPDATE' | 'DELETE'
export type TaskLogObjectType = 'TASK_INFO' | 'TASK_DEPENDENCY' | 'TASK_ASSIGNEE'
export type TaskLogScopeType = 'PERSONAL' | 'TEAM'

export interface TaskLogTaskSnapshot {
  id: number
  title: string
}

export interface TaskLogTeamSnapshot {
  id: number
  name: string
}

export interface TaskOperationLog {
  id: number
  operator: UserBrief
  operationType: TaskLogOperationType
  objectType: TaskLogObjectType
  scopeType: TaskLogScopeType
  task: TaskLogTaskSnapshot | null
  team: TaskLogTeamSnapshot | null
  summary: string
  operationTime: string
}

export interface TaskLogByTaskQueryParams {
  operationType?: TaskLogOperationType
  objectType?: TaskLogObjectType
  page?: number
  pageSize?: number
}

export interface TaskLogAggregateQueryParams extends TaskLogByTaskQueryParams {
  keyword?: string
  startTime?: string
  endTime?: string
}

export type LogQueryMode =
  | { kind: 'personal-task'; taskId: number }
  | { kind: 'personal-all' }
  | { kind: 'team-task'; teamId: number; taskId: number }
  | { kind: 'team-all'; teamId: number }
