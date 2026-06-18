import http from './http'
import type { ApiResponse } from '../types/api'
import type {
  PageResult,
  TeamCreateRequest,
  TeamDetail,
  TeamListItem,
  TeamTask,
  TeamTaskCreatePayload,
  TeamTaskQueryParams,
  TeamTaskStats,
  TeamTaskUpdatePayload,
} from '../types/team'
import type { TaskOption } from '../types/task'

export async function createTeam(payload: TeamCreateRequest): Promise<ApiResponse<null>> {
  const { data } = await http.post<ApiResponse<null>>('/v1/teams', payload)
  return data
}

export async function dissolveTeam(teamId: number): Promise<ApiResponse<null>> {
  const { data } = await http.delete<ApiResponse<null>>(`/v1/teams/${teamId}`)
  return data
}

export async function getMyTeams(
  page: number,
  pageSize: number,
): Promise<ApiResponse<PageResult<TeamListItem>>> {
  const { data } = await http.get<ApiResponse<PageResult<TeamListItem>>>('/v1/teams', {
    params: { page, pageSize },
  })
  return data
}

export async function getTeamDetail(teamId: number): Promise<ApiResponse<TeamDetail>> {
  const { data } = await http.get<ApiResponse<TeamDetail>>(`/v1/teams/${teamId}`)
  return data
}

export async function getTeamTaskStats(
  teamId: number,
  upcomingDays = 3,
): Promise<ApiResponse<TeamTaskStats>> {
  const { data } = await http.get<ApiResponse<TeamTaskStats>>(
    `/v1/teams/${teamId}/task-stats`,
    { params: { upcomingDays } },
  )
  return data
}

export async function addTeamMember(
  teamId: number,
  userId: number,
): Promise<ApiResponse<null>> {
  const { data } = await http.post<ApiResponse<null>>(`/v1/teams/${teamId}/members`, {
    userId,
  })
  return data
}

export async function updateTeamMemberRole(
  teamId: number,
  userId: number,
  role: 'ADMIN' | 'MEMBER',
): Promise<ApiResponse<null>> {
  const { data } = await http.put<ApiResponse<null>>(
    `/v1/teams/${teamId}/members/${userId}/role`,
    { role },
  )
  return data
}

export async function removeTeamMember(
  teamId: number,
  userId: number,
): Promise<ApiResponse<null>> {
  const { data } = await http.delete<ApiResponse<null>>(
    `/v1/teams/${teamId}/members/${userId}`,
  )
  return data
}

export async function getTeamTasks(
  teamId: number,
  params?: TeamTaskQueryParams,
): Promise<ApiResponse<PageResult<TeamTask>>> {
  const { data } = await http.get<ApiResponse<PageResult<TeamTask>>>(
    `/v1/teams/${teamId}/tasks`,
    { params },
  )
  return data
}

export async function getTeamTaskOptions(
  teamId: number,
  keyword?: string,
): Promise<ApiResponse<TaskOption[]>> {
  const { data } = await http.get<ApiResponse<TaskOption[]>>(
    `/v1/teams/${teamId}/tasks/options`,
    { params: keyword ? { keyword } : undefined },
  )
  return data
}

export async function createTeamTask(
  teamId: number,
  payload: TeamTaskCreatePayload,
): Promise<ApiResponse<TeamTask>> {
  const { data } = await http.post<ApiResponse<TeamTask>>(
    `/v1/teams/${teamId}/tasks`,
    payload,
  )
  return data
}

export async function updateTeamTask(
  teamId: number,
  taskId: number,
  payload: TeamTaskUpdatePayload,
): Promise<ApiResponse<TeamTask>> {
  const { data } = await http.put<ApiResponse<TeamTask>>(
    `/v1/teams/${teamId}/tasks/${taskId}`,
    payload,
  )
  return data
}

export async function deleteTeamTask(
  teamId: number,
  taskId: number,
): Promise<ApiResponse<null>> {
  const { data } = await http.delete<ApiResponse<null>>(
    `/v1/teams/${teamId}/tasks/${taskId}`,
  )
  return data
}

export async function assignTeamTask(
  teamId: number,
  taskId: number,
  userIds: number[],
): Promise<ApiResponse<null>> {
  const { data } = await http.put<ApiResponse<null>>(
    `/v1/teams/${teamId}/tasks/${taskId}/assignees`,
    { userIds },
  )
  return data
}

export async function leaveTeam(teamId: number): Promise<ApiResponse<null>> {
  const { data } = await http.delete<ApiResponse<null>>(`/v1/teams/${teamId}/leave`)
  return data
}

export async function transferOwnership(
  teamId: number,
  newOwnerId: number,
): Promise<ApiResponse<null>> {
  const { data } = await http.post<ApiResponse<null>>(`/v1/teams/${teamId}/transfer`, {
    newOwnerId,
  })
  return data
}
