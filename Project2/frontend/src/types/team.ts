import type { DueStatus } from './task'

/** 团队角色，与后端 TeamRole 枚举一致 */
export type TeamRole = 'OWNER' | 'ADMIN' | 'MEMBER'

/** 可写入的成员角色（修改角色接口仅允许 ADMIN / MEMBER） */
export type TeamMemberWritableRole = 'ADMIN' | 'MEMBER'

export interface PageResult<T> {
  total: number
  page: number
  pageSize: number
  items: T[]
}

export interface TeamListItem {
  id: number
  name: string
  role: TeamRole
  memberCount: number
}

export interface MemberInfo {
  userId: number
  username: string
  role: TeamRole
  joinedAt: string
}

export interface TeamDetail {
  id: number
  name: string
  myRole: TeamRole
  members: MemberInfo[]
  createdAt: string
}

/** 创建团队请求体，与后端 TeamCreateRequest 一致 */
export interface TeamCreateRequest {
  name: string
  memberIds: number[]
}

/** 转让团队所有权请求体，与后端 TransferOwnershipRequest 一致 */
export interface TransferOwnershipRequest {
  newOwnerId: number
}

/** 团队任务中的创建者/被分配者简要信息 */
export interface TeamTaskUserBrief {
  id?: number
  userId?: number
  username: string
}

/** 团队任务详情，与后端 TeamTaskResponse 一致 */
export interface TeamTask {
  id: number
  title: string
  description: string | null
  status: 'TODO' | 'IN_PROGRESS' | 'DONE'
  priority: 'LOW' | 'MEDIUM' | 'HIGH'
  dueAt: string | null
  dueStatus: DueStatus
  creator: TeamTaskUserBrief
  assignees: TeamTaskUserBrief[]
  createdAt: string
  updatedAt: string
}

export interface TeamTaskQueryParams {
  status?: string
  priority?: string
  assigneeId?: number
  dueStart?: string
  dueEnd?: string
  keyword?: string
  page?: number
  pageSize?: number
}

export interface TeamTaskStats {
  total: number
  byStatus: Record<'TODO' | 'IN_PROGRESS' | 'DONE', number>
  byPriority: Record<'LOW' | 'MEDIUM' | 'HIGH', number>
  overdueCount: number
  upcomingDueCount: number
}

/** 创建团队任务请求体，与后端 TeamTaskCreateRequest 一致 */
export interface TeamTaskCreatePayload {
  title: string
  description?: string
  status: 'TODO' | 'IN_PROGRESS' | 'DONE'
  priority: 'LOW' | 'MEDIUM' | 'HIGH'
  dueAt?: string
}

/** 更新团队任务请求体，与后端 TeamTaskUpdateRequest 一致（所有字段可选） */
export interface TeamTaskUpdatePayload {
  title?: string
  description?: string
  status?: 'TODO' | 'IN_PROGRESS' | 'DONE'
  priority?: 'LOW' | 'MEDIUM' | 'HIGH'
  dueAt?: string | null
}
