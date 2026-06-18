<template>
  <AppDialog
    :visible="visible"
    title="编辑任务"
    wide
    @close="$emit('close')"
  >
    <form class="edit-form" @submit.prevent="$emit('submit')">
      <TaskFormFields
        :form="form"
        show-status
        :status-only="statusOnly"
        due-input-type="datetime-local"
      />
    </form>
    <template #footer>
      <button type="button" class="btn btn--ghost" @click="$emit('close')">取消</button>
      <button type="button" class="btn btn--primary" @click="$emit('submit')">保存修改</button>
    </template>
  </AppDialog>
</template>

<script setup lang="ts">
import AppDialog from '../common/AppDialog.vue'
import TaskFormFields from './TaskFormFields.vue'
import type { Task } from '../../types/task'

type EditForm = {
  title: string
  description: string
  status: Task['status']
  priority: Task['priority']
  dueAtInput: string
}

defineProps<{
  visible: boolean
  statusOnly: boolean
  form: EditForm
}>()

defineEmits<{
  (e: 'close'): void
  (e: 'submit'): void
}>()
</script>

<style scoped>
.edit-form {
  margin: 0;
}
</style>
