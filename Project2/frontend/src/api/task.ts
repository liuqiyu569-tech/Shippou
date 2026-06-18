import http from './http'
import type { ApiResponse } from '../types/api'
import type { Task, TaskDependencyList, TaskOption } from '../types/task'
import type { PageResult } from '../types/team'

export interface TaskCreateData {
  title: string
  description: string
  status: string
  priority: string
  dueAt: string
}

export interface TaskQueryParams {
  scope?: string
  status?: string
  priority?: string
  dueStart?: string
  dueEnd?: string
  keyword?: string
  page?: number
  pageSize?: number
}

export async function getTaskList(
  params?: TaskQueryParams,
): Promise<ApiResponse<PageResult<Task>>> {
  const { data } = await http.get<ApiResponse<PageResult<Task>>>('/v1/user/tasks', { params })
  return data
}

export async function getTaskById(id: number): Promise<ApiResponse<Task>> {
  const { data } = await http.get<ApiResponse<Task>>(`/v1/user/tasks/${id}`)
  return data
}

export async function createTask(data: TaskCreateData): Promise<ApiResponse<null>> {
  const { data: body } = await http.post<ApiResponse<null>>('/v1/user/tasks', data)
  return body
}

export async function updateTask(
  id: number,
  data: Partial<TaskCreateData>,
): Promise<ApiResponse<null>> {
  const { data: body } = await http.put<ApiResponse<null>>(`/v1/user/tasks/${id}`, data)
  return body
}

export async function deleteTask(id: number): Promise<ApiResponse<null>> {
  const { data } = await http.delete<ApiResponse<null>>(`/v1/user/tasks/${id}`)
  return data
}

export async function getPersonalTaskOptions(keyword?: string): Promise<ApiResponse<TaskOption[]>> {
  const { data } = await http.get<ApiResponse<TaskOption[]>>('/v1/user/tasks/options', {
    params: keyword ? { keyword } : undefined,
  })
  return data
}

// ==================== Lab3 新增：任务依赖相关接口 ====================
/**
 * 获取个人任务的依赖关系（前置/后继任务）
 */
export async function getTaskDependencies(taskId: number): Promise<ApiResponse<TaskDependencyList>> {
  const { data } = await http.get<ApiResponse<TaskDependencyList>>(`/v1/user/tasks/${taskId}/dependencies`)
  return data
}

/**
 * 添加个人任务依赖
 */
export async function addTaskDependency(taskId: number, dependTaskId: number): Promise<ApiResponse<null>> {
  const { data } = await http.post<ApiResponse<null>>(`/v1/user/tasks/${taskId}/dependencies`, {
    dependsOnTaskId: dependTaskId,
  })
  return data
}

/**
 * 删除个人任务依赖
 */
export async function deleteTaskDependency(taskId: number, dependTaskId: number): Promise<ApiResponse<null>> {
  const { data } = await http.delete<ApiResponse<null>>(`/v1/user/tasks/${taskId}/dependencies/${dependTaskId}`)
  return data
}

/**
 * 获取个人任务依赖图谱（DAG可视化）
 */
export async function getTaskDependencyGraph(): Promise<ApiResponse<any>> {
  const { data } = await http.get<ApiResponse<any>>('/v1/user/tasks/dependency-graph')
  return data
}

/**
 * 获取团队任务依赖图谱
 */
export async function getTeamTaskDependencyGraph(teamId: number): Promise<ApiResponse<any>> {
  const { data } = await http.get<ApiResponse<any>>(`/v1/teams/${teamId}/tasks/dependency-graph`)
  return data
}

/**
 * 获取团队任务依赖关系
 */
export async function getTeamTaskDependencies(
  teamId: number,
  taskId: number,
): Promise<ApiResponse<TaskDependencyList>> {
  const { data } = await http.get<ApiResponse<TaskDependencyList>>(
    `/v1/teams/${teamId}/tasks/${taskId}/dependencies`,
  )
  return data
}

/**
 * 添加团队任务依赖
 */
export async function addTeamTaskDependency(teamId: number, taskId: number, dependTaskId: number): Promise<ApiResponse<null>> {
  const { data } = await http.post<ApiResponse<null>>(
    `/v1/teams/${teamId}/tasks/${taskId}/dependencies`,
    { dependsOnTaskId: dependTaskId },
  )
  return data
}

/**
 * 删除团队任务依赖
 */
export async function deleteTeamTaskDependency(teamId: number, taskId: number, dependTaskId: number): Promise<ApiResponse<null>> {
  const { data } = await http.delete<ApiResponse<null>>(`/v1/teams/${teamId}/tasks/${taskId}/dependencies/${dependTaskId}`)
  return data
}
