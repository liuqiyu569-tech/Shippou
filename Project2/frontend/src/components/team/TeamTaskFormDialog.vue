<template>
  <AppDialog
    :visible="visible"
    :title="mode === 'create' ? '新建团队任务' : '编辑团队任务'"
    :title-id="mode === 'create' ? 'team-task-create-title' : 'team-task-edit-title'"
    wide
    :disable-close="busy"
    @close="$emit('close')"
  >
    <form class="form" @submit.prevent="$emit('submit')">
      <TaskFormFields
        :form="form"
        layout="grid"
        show-status
        status-variant="enum"
        priority-variant="enum"
        due-input-type="datetime-local"
      />
    </form>
    <template #footer>
      <button type="button" class="btn btn--ghost" :disabled="busy" @click="$emit('close')">取消</button>
      <button type="button" class="btn btn--primary" :disabled="busy" @click="$emit('submit')">
        {{ mode === 'create' ? '创建' : '保存' }}
      </button>
    </template>
  </AppDialog>
</template>

<script setup lang="ts">
import AppDialog from '../common/AppDialog.vue'
import TaskFormFields from '../task/TaskFormFields.vue'
import type { TeamTask } from '../../types/team'

type TaskFormModel = {
  title: string
  description: string
  status: TeamTask['status']
  priority: TeamTask['priority']
  dueAtInput: string
}

defineProps<{
  visible: boolean
  mode: 'create' | 'edit'
  form: TaskFormModel
  busy: boolean
}>()

defineEmits<{
  (e: 'close'): void
  (e: 'submit'): void
}>()
</script>

<style scoped>
.form {
  margin: 0;
}
</style>
