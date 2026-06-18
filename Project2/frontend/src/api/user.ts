import http from './http'
import type { ApiResponse } from '../types/api'
import type { PageResult } from '../types/team'
import type { UserBrief } from '../types/user'

export async function queryUsers(
  keyword: string | undefined,
  page: number,
  pageSize: number,
): Promise<ApiResponse<PageResult<UserBrief>>> {
  const params: Record<string, string | number> = { page, pageSize }
  const trimmed = keyword?.trim()
  if (trimmed) {
    params.keyword = trimmed
  }

  const { data } = await http.get<ApiResponse<PageResult<UserBrief>>>('/v1/users', {
    params,
  })
  return data
}
