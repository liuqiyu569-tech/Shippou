<template>
  <div class="task-list">
    <div v-if="!tasks.length" class="empty">暂无任务</div>
    <div v-else v-for="task in tasks" :key="task.id" class="task-item">
      <div class="task-title">{{ truncate(task.title, 50) }}</div>
      <div class="task-desc">{{ truncate(task.description, 100) || '无描述' }}</div>
      <div class="task-info">
        <RouterLink
          v-if="task.teamId"
          :to="{ name: 'team-space', params: { teamId: String(task.teamId) } }"
          class="source-badge"
        >
          来自团队 #{{ task.teamId }}
        </RouterLink>
        <span v-else class="source-badge source-badge--personal">个人任务</span>
        <span :class="['priority', task.priority.toLowerCase()]">{{ task.priority }}</span>
        <span class="status">{{ task.status }}</span>
        <span class="time">截止：{{ formatDate(task.dueAt, '无') }}</span>
        <span
          v-if="shouldShowDueStatus(task.dueStatus)"
          :class="['due-status', `due-status--${task.dueStatus.toLowerCase()}`]"
        >
          {{ getDueStatusLabel(task.dueStatus) }}
        </span>
      </div>
      <div class="task-actions">
        <RouterLink
          v-if="actionsFor(task).showGoTeamSpace"
          :to="{ name: 'team-space', params: { teamId: String(task.teamId) } }"
          class="team-link-btn"
        >
          团队空间
        </RouterLink>
        <button type="button" class="detail-btn" @click="handleViewDetail(task)">查看详情</button>
        <button
          v-if="actionsFor(task).canEditFull || actionsFor(task).canEditStatus"
          type="button"
          class="edit-btn"
          @click="handleEdit(task)"
        >
          {{ actionsFor(task).canEditStatus && !actionsFor(task).canEditFull ? '修改状态' : '编辑' }}
        </button>
        <button
          v-if="!task.teamId"
          type="button"
          class="dependency-btn"
          @click="emit('open-dependency', task.id)"
        >
          任务依赖
        </button>
        <button
          v-if="actionsFor(task).canDelete"
          type="button"
          class="del-btn"
          @click="handleDelete(task, () => emit('refresh'))"
        >
          删除
        </button>
      </div>
    </div>

    <TaskHubDetailDialog
      :visible="showDetail"
      :task="selectedTask"
      @close="showDetail = false"
    />

    <TaskHubEditDialog
      :visible="showEdit"
      :status-only="editingStatusOnly"
      :form="editForm"
      @close="closeEdit"
      @submit="onSubmitEdit"
    />
  </div>
</template>

<script setup lang="ts">
import { toRef } from 'vue'
import { RouterLink } from 'vue-router'
import TaskHubDetailDialog from './TaskHubDetailDialog.vue'
import TaskHubEditDialog from './TaskHubEditDialog.vue'
import { useTaskHubListEdit } from '../../composables/useTaskHubListEdit'
import { formatDate, truncate } from '../../utils/format'
import { getDueStatusLabel, shouldShowDueStatus } from '../../utils/dueStatus'
import type { Task } from '../../types/task'
import type { TeamRole } from '../../types/team'

const props = withDefaults(
  defineProps<{
    tasks: Task[]
    teamRoleMap?: Record<number, TeamRole>
  }>(),
  {
    teamRoleMap: () => ({}),
  },
)

const emit = defineEmits<{
  (e: 'refresh'): void
  (e: 'open-dependency', taskId: number): void
}>()

const teamRoleMapRef = toRef(props, 'teamRoleMap')
const {
  showDetail,
  showEdit,
  selectedTask,
  editingStatusOnly,
  editForm,
  actionsFor,
  handleViewDetail,
  handleEdit,
  closeEdit,
  submitEdit,
  handleDelete,
} = useTaskHubListEdit(() => teamRoleMapRef.value)

function onSubmitEdit() {
  void submitEdit(() => emit('refresh'))
}
</script>

<style scoped>
.task-list {
  width: 100%;
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}
.empty {
  text-align: center;
  padding: 40px 0;
  color: #999;
  grid-column: 1 / -1;
}
.task-item {
  padding: 15px;
  border: 1px solid #eee;
  border-radius: 8px;
  background: #f9f9f9;
  font-size: 14px;
}
.task-title {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 5px;
  word-break: break-all;
}
.task-desc {
  color: #666;
  margin-bottom: 8px;
  word-break: break-all;
  font-size: 14px;
}
.task-info {
  display: flex;
  gap: 10px;
  font-size: 13px;
  align-items: center;
  flex-wrap: wrap;
}
.task-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
  margin-top: 10px;
}
.source-badge {
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  background: #e8f3ff;
  color: var(--color-primary);
  border: 1px solid #c0d9f0;
  text-decoration: none;
}
.source-badge--personal {
  background: #f0faf0;
  color: #4caf50;
  border-color: #b2dfb2;
  font-size: 13px;
}
.team-link-btn {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 13px;
  color: var(--color-primary);
  border: 1px solid var(--color-primary-border);
  text-decoration: none;
  font-weight: 600;
}
.priority {
  padding: 2px 6px;
  border-radius: 4px;
  color: #fff;
  font-size: 14px;
}
.high {
  background: #f56c6c;
}
.medium {
  background: #e6a23c;
}
.low {
  background: #67c23a;
}
.status {
  color: var(--color-primary);
  font-size: 14px;
}
.time {
  color: #999;
}
.due-status {
  padding: 2px 7px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}
.due-status--upcoming_due {
  color: #9a5b00;
  background: #fff4d7;
  border: 1px solid #f4cf7a;
}
.due-status--overdue {
  color: #a72525;
  background: #ffe6e6;
  border: 1px solid #f3a6a6;
}
.due-status--done {
  color: #217a3a;
  background: #e7f6eb;
  border: 1px solid #a8d8b5;
}
.detail-btn,
.edit-btn,
.del-btn,
.dependency-btn,
.team-link-btn {
  min-height: 32px;
  padding: 5px 12px;
  font-size: 14px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.detail-btn,
.edit-btn,
.dependency-btn {
  background: var(--color-primary);
  color: #fff;
}
.team-link-btn {
  background: #fff;
  color: var(--color-primary);
  border: 1px solid var(--color-primary-border);
}
.del-btn {
  background: var(--color-danger);
  color: #fff;
}

@media (max-width: 720px) {
  .task-list {
    grid-template-columns: 1fr;
  }
}
</style>
