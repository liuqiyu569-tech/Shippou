import http from './http'
import type { ApiResponse } from '../types/api'
import type {
  TaskLogAggregateQueryParams,
  TaskLogByTaskQueryParams,
  TaskOperationLog,
} from '../types/taskLog'
import type { PageResult } from '../types/team'

export async function getPersonalTaskLogs(
  taskId: number,
  params?: TaskLogByTaskQueryParams,
): Promise<ApiResponse<PageResult<TaskOperationLog>>> {
  const { data } = await http.get<ApiResponse<PageResult<TaskOperationLog>>>(
    `/v1/user/tasks/${taskId}/logs`,
    { params },
  )
  return data
}

export async function getCurrentUserTaskLogs(
  params?: TaskLogAggregateQueryParams,
): Promise<ApiResponse<PageResult<TaskOperationLog>>> {
  const { data } = await http.get<ApiResponse<PageResult<TaskOperationLog>>>(
    '/v1/user/task-logs',
    { params },
  )
  return data
}

export async function getTeamTaskLogs(
  teamId: number,
  taskId: number,
  params?: TaskLogByTaskQueryParams,
): Promise<ApiResponse<PageResult<TaskOperationLog>>> {
  const { data } = await http.get<ApiResponse<PageResult<TaskOperationLog>>>(
    `/v1/teams/${teamId}/tasks/${taskId}/logs`,
    { params },
  )
  return data
}

export async function getTeamAggregateTaskLogs(
  teamId: number,
  params?: TaskLogAggregateQueryParams,
): Promise<ApiResponse<PageResult<TaskOperationLog>>> {
  const { data } = await http.get<ApiResponse<PageResult<TaskOperationLog>>>(
    `/v1/teams/${teamId}/task-logs`,
    { params },
  )
  return data
}
