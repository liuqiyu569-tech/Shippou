<template>
  <section class="task-section">
    <div class="task-toolbar">
      <h3>团队任务</h3>
      <div class="toolbar-actions">
        <button
          v-if="canManageTasks"
          type="button"
          class="create-task-btn"
          :disabled="actionsDisabled"
          @click="$emit('create')"
        >
          + 新建任务
        </button>
        <button
          type="button"
          class="graph-btn"
          :disabled="actionsDisabled"
          @click="$emit('view-graph')"
        >
          依赖图谱
        </button>
        <button
          type="button"
          class="graph-btn"
          :disabled="actionsDisabled"
          @click="$emit('view-logs')"
        >
          团队任务日志
        </button>
      </div>
    </div>

    <p v-if="loading" class="state">加载中…</p>
    <p v-else-if="!tasks.length" class="state">暂无团队任务</p>

    <div v-else class="task-list">
      <div v-for="task in tasks" :key="task.id" class="task-item">
        <div class="task-title">{{ truncate(task.title, 50) }}</div>
        <div class="task-desc">{{ truncate(task.description, 100) || '无描述' }}</div>
        <div class="task-meta">
          <span>状态：{{ task.status }}</span>
          <span>优先级：{{ task.priority }}</span>
          <span>截止：{{ formatDate(task.dueAt) }}</span>
          <span
            v-if="shouldShowDueStatus(task.dueStatus)"
            :class="['due-status', `due-status--${task.dueStatus.toLowerCase()}`]"
          >
            {{ getDueStatusLabel(task.dueStatus) }}
          </span>
          <span>指派：{{ assigneeNames(task) }}</span>
        </div>

        <div v-if="canManageTasks" class="task-actions">
          <button
            type="button"
            class="detail-btn"
            :disabled="actionsDisabled"
            @click="$emit('view-detail', task)"
          >
            查看详情
          </button>
          <button type="button" class="edit-btn" :disabled="actionsDisabled" @click="$emit('edit', task)">
            编辑
          </button>
          <button
            type="button"
            class="detail-btn"
            :disabled="actionsDisabled"
            @click="$emit('assign', task)"
          >
            分配成员
          </button>
          <button
            type="button"
            class="dependency-btn"
            :disabled="actionsDisabled"
            @click="$emit('manage-dependencies', task)"
          >
            依赖关系
          </button>
          <button
            type="button"
            class="del-btn"
            :disabled="actionsDisabled"
            @click="$emit('remove', task.id)"
          >
            删除
          </button>
        </div>
        <div v-else-if="isAssignedToMe(task)" class="task-actions">
          <button
            type="button"
            class="detail-btn"
            :disabled="actionsDisabled"
            @click="$emit('view-detail', task)"
          >
            查看详情
          </button>
          <button
            type="button"
            class="dependency-btn"
            :disabled="actionsDisabled"
            @click="$emit('manage-dependencies', task)"
          >
            依赖关系
          </button>
          <template v-if="memberEditingTaskId === task.id">
            <select
              :model-value="memberStatusDraft"
              :disabled="actionsDisabled"
              :class="['member-status-select', `member-status-select--${memberStatusDraft.toLowerCase()}`]"
              @change="onStatusDraftChange($event)"
            >
              <option value="TODO">TODO</option>
              <option value="IN_PROGRESS">IN_PROGRESS</option>
              <option value="DONE">DONE</option>
            </select>
            <button
              type="button"
              class="member-status-btn member-status-btn--save"
              :disabled="actionsDisabled"
              @click="$emit('save-status', task)"
            >
              保存
            </button>
            <button
              type="button"
              class="member-status-btn member-status-btn--cancel"
              :disabled="actionsDisabled"
              @click="$emit('cancel-status')"
            >
              取消
            </button>
          </template>
          <button
            v-else
            type="button"
            class="edit-btn"
            :disabled="actionsDisabled"
            @click="$emit('start-status', task)"
          >
            修改状态
          </button>
        </div>
        <div v-else class="task-actions">
          <button
            type="button"
            class="detail-btn"
            :disabled="actionsDisabled"
            @click="$emit('view-detail', task)"
          >
            查看详情
          </button>
          <button
            type="button"
            class="dependency-btn"
            :disabled="actionsDisabled"
            @click="$emit('manage-dependencies', task)"
          >
            依赖关系
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { formatDate, truncate } from '../../utils/format'
import { getDueStatusLabel, shouldShowDueStatus } from '../../utils/dueStatus'
import { assigneeNames } from '../../utils/team'
import type { TeamTask } from '../../types/team'

const props = defineProps<{
  tasks: TeamTask[]
  loading: boolean
  canManageTasks: boolean
  actionsDisabled: boolean
  isAssignedToMe: (task: TeamTask) => boolean
  memberEditingTaskId: number | null
  memberStatusDraft: TeamTask['status']
}>()

const emit = defineEmits<{
  (e: 'create'): void
  (e: 'view-detail', task: TeamTask): void
  (e: 'edit', task: TeamTask): void
  (e: 'remove', taskId: number): void
  (e: 'assign', task: TeamTask): void
  (e: 'manage-dependencies', task: TeamTask): void
  (e: 'view-graph'): void
  (e: 'view-logs'): void
  (e: 'start-status', task: TeamTask): void
  (e: 'save-status', task: TeamTask): void
  (e: 'cancel-status'): void
  (e: 'update-status-draft', value: TeamTask['status']): void
}>()

function onStatusDraftChange(event: Event) {
  const target = event.target as HTMLSelectElement
  emit('update-status-draft', target.value as TeamTask['status'])
}
</script>

<style scoped>
.task-section {
  margin-top: 0;
  padding-top: 0;
  border-top: 0;
}

.task-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.toolbar-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.create-task-btn,
.graph-btn {
  padding: 8px 16px;
  border-radius: 999px;
  border: none;
  font-weight: 700;
  cursor: pointer;
}

.create-task-btn {
  color: #fffaf2;
  background: var(--color-primary);
}

.graph-btn {
  color: var(--color-primary);
  background: var(--color-primary-soft);
  border: 1px solid var(--color-primary-border);
}

.create-task-btn:disabled,
.graph-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.state {
  color: #666;
  margin: 10px 0;
}

.task-list {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.task-item {
  padding: 15px;
  border: 1px solid #eee;
  border-radius: 8px;
  background: #f9f9f9;
}

.task-title {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 5px;
  word-break: break-all;
}

.task-desc {
  color: #666;
  margin-bottom: 8px;
  word-break: break-all;
}

.task-meta {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  font-size: 12px;
  color: #555;
  align-items: center;
  margin-bottom: 10px;
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

.task-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.task-actions button {
  padding: 4px 10px;
  border: none;
  border-radius: 4px;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
}

.detail-btn {
  background: var(--color-primary);
}

.edit-btn {
  background: var(--color-primary);
}

.del-btn {
  background: var(--color-danger);
}

.dependency-btn {
  background: var(--color-primary);
}

.member-status-select {
  min-width: 130px;
  padding: 4px 8px;
  border-radius: 6px;
  border: 1px solid #cfd8dc;
  font-weight: 600;
  background: #fff;
}

.member-status-select--todo {
  border-color: #b0bec5;
  color: #455a64;
  background: #f5f7f8;
}

.member-status-select--in_progress {
  border-color: #90caf9;
  color: #1565c0;
  background: #e3f2fd;
}

.member-status-select--done {
  border-color: #a5d6a7;
  color: #2e7d32;
  background: #e8f5e9;
}

.member-status-btn {
  border-radius: 6px;
  font-weight: 600;
}

.member-status-btn--save {
  border: 1px solid var(--color-primary-border);
  color: #fffaf2;
  background: var(--color-primary);
}

.member-status-btn--cancel {
  border: 1px solid #b0bec5;
  color: #455a64;
  background: #fff;
}

@media (max-width: 720px) {
  .task-list {
    grid-template-columns: 1fr;
  }
}
</style>
