import http from './http'
import type { ApiResponse } from '../types/api'
import type { AuthPayload, AuthSession } from '../types/auth'

/**
 * 调用后端登录接口，返回 token、tokenType、expiresIn 和用户信息。
 */
export async function login(payload: AuthPayload) {
  // 移除 /api 前缀，由 http.ts 的 baseURL 自动拼接
  const { data } = await http.post<ApiResponse<AuthSession>>('/v1/auth/login', payload)
  return data
}

/**
 * 调用后端注册接口。当前后端契约下，注册成功仅返回 message 和 null data。
 */
export async function register(payload: AuthPayload) {
  // 移除 /api 前缀，由 http.ts 的 baseURL 自动拼接
  const { data } = await http.post<ApiResponse<null>>('/v1/auth/register', payload)
  return data
}