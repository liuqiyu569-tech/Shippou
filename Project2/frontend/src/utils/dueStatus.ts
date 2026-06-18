import type { DueStatus } from '../types/task'

const LABELS: Record<DueStatus, string> = {
  NONE: '',
  NORMAL: '',
  UPCOMING_DUE: '即将到期',
  OVERDUE: '已逾期',
  DONE: '已完成',
}

export function getDueStatusLabel(status?: DueStatus | null): string {
  return status ? LABELS[status] : ''
}

export function shouldShowDueStatus(status?: DueStatus | null): boolean {
  return status === 'UPCOMING_DUE' || status === 'OVERDUE' || status === 'DONE'
}
