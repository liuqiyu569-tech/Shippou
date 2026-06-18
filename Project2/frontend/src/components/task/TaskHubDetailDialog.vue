<template>
  <AppDialog :visible="visible" title="任务详情" wide tall @close="$emit('close')">
    <div v-if="task" class="detail-content">
      <div class="detail-item">
        <label>标题：</label>
        <p class="title-text">{{ task.title }}</p>
      </div>
      <div class="detail-item">
        <label>描述：</label>
        <p class="desc-text">{{ task.description || '无' }}</p>
      </div>
      <div class="detail-item">
        <label>状态：</label>
        <span>{{ task.status }}</span>
      </div>
      <div class="detail-item">
        <label>优先级：</label>
        <span>{{ task.priority }}</span>
      </div>
      <div class="detail-item">
        <label>截止时间：</label>
        <span>{{ formatDate(task.dueAt, '无') }}</span>
        <span
          v-if="shouldShowDueStatus(task.dueStatus)"
          :class="['due-status', `due-status--${task.dueStatus.toLowerCase()}`]"
        >
          {{ getDueStatusLabel(task.dueStatus) }}
        </span>
      </div>
      <div class="detail-item">
        <label>创建时间：</label>
        <span>{{ formatDate(task.createdAt, '无') }}</span>
      </div>
      <div class="detail-item">
        <label>更新时间：</label>
        <span>{{ formatDate(task.updatedAt, '无') }}</span>
      </div>

      <section v-if="!task.teamId" class="log-section">
        <h3 class="log-section__title">操作日志</h3>
        <TaskOperationLogPanel
          :mode="{ kind: 'personal-task', taskId: task.id }"
          variant="embedded"
          :active="visible && !task.teamId"
        />
      </section>
      <p v-else class="log-hint">团队任务的操作日志请前往对应团队空间查看。</p>
    </div>
    <template #footer>
      <button type="button" class="btn btn--ghost" @click="$emit('close')">关闭</button>
    </template>
  </AppDialog>
</template>

<script setup lang="ts">
import AppDialog from '../common/AppDialog.vue'
import TaskOperationLogPanel from './TaskOperationLogPanel.vue'
import { formatDate } from '../../utils/format'
import { getDueStatusLabel, shouldShowDueStatus } from '../../utils/dueStatus'
import type { Task } from '../../types/task'

defineProps<{
  visible: boolean
  task: Task | null
}>()

defineEmits<{
  (e: 'close'): void
}>()
</script>

<style scoped>
.detail-content {
  margin: 0;
}

.detail-item {
  margin-bottom: 12px;
}

.detail-item label {
  font-weight: bold;
  color: #333;
  display: inline-block;
  min-width: 80px;
}

.detail-item span {
  color: #666;
}

.due-status {
  display: inline-flex;
  margin-left: 8px;
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

.title-text,
.desc-text {
  margin: 4px 0 0 0;
  padding: 8px;
  background: #f5f5f5;
  border-radius: 4px;
  word-break: break-all;
  white-space: pre-wrap;
}

.desc-text {
  max-height: 200px;
  overflow-y: auto;
}

.log-section {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #eee;
}

.log-section__title {
  margin: 0 0 8px;
  font-size: 15px;
  color: #333;
}

.log-hint {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #eee;
  color: #888;
  font-size: 13px;
}
</style>
