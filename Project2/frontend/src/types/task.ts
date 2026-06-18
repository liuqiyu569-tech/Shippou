// 任务状态（const + 类型别名，兼容 erasableSyntaxOnly）
export const TaskStatus = {
  TODO: 'TODO',
  IN_PROGRESS: 'IN_PROGRESS',
  DONE: 'DONE',
} as const

export type TaskStatus = (typeof TaskStatus)[keyof typeof TaskStatus]

// 任务优先级
export const TaskPriority = {
  LOW: 'LOW',
  MEDIUM: 'MEDIUM',
  HIGH: 'HIGH',
} as const

export type TaskPriority = (typeof TaskPriority)[keyof typeof TaskPriority]

export type DueStatus = 'NONE' | 'NORMAL' | 'UPCOMING_DUE' | 'OVERDUE' | 'DONE'

// 任务数据类型
export interface Task {
  id: number;
  title: string;
  description: string;
  status: 'TODO' | 'IN_PROGRESS' | 'DONE';
  priority: 'LOW' | 'MEDIUM' | 'HIGH';
  dueAt?: string | null;
  dueStatus: DueStatus;
  createdAt: string;
  updatedAt: string;
  teamId?: number | null; // 新增
}

export interface TaskDependencyItem {
  id: number | null
  title: string
  status: TaskStatus
}

export interface TaskDependencyList {
  prerequisites: TaskDependencyItem[]
  successors: TaskDependencyItem[]
}

export interface TaskOption {
  id: number
  title: string
}
