import type { TaskLogObjectType, TaskLogOperationType } from '../types/taskLog'

const OPERATION_TYPE_LABELS: Record<TaskLogOperationType, string> = {
  CREATE: '创建',
  UPDATE: '更新',
  DELETE: '删除',
}

const OBJECT_TYPE_LABELS: Record<TaskLogObjectType, string> = {
  TASK_INFO: '任务信息',
  TASK_DEPENDENCY: '任务依赖',
  TASK_ASSIGNEE: '负责人',
}

export function formatOperationType(type: TaskLogOperationType): string {
  return OPERATION_TYPE_LABELS[type] ?? type
}

export function formatObjectType(type: TaskLogObjectType): string {
  return OBJECT_TYPE_LABELS[type] ?? type
}
