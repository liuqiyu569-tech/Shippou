<template>
  <AppDialog
    :visible="visible"
    title="新建任务"
    @close="close"
    @update:visible="emit('update:visible', $event)"
  >
    <form class="form" @submit.prevent="submit">
      <TaskFormFields :form="formModel" :show-status="false" due-input-type="datetime-local" />
    </form>
    <template #footer>
      <button type="button" class="btn btn--ghost" @click="close">取消</button>
      <button type="button" class="btn btn--primary" @click="submit">确认创建</button>
    </template>
  </AppDialog>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import axios from 'axios'
import AppDialog from './common/AppDialog.vue'
import TaskFormFields from './task/TaskFormFields.vue'
import { createTask } from '../api/task'
import { getApiErrorMessage } from '../api/http'
import { useNoticeStore } from '../stores/notice'
import { normalizeDueAt } from '../utils/team'

defineProps<{ visible: boolean }>()
const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void
  (e: 'refresh'): void
}>()

const noticeStore = useNoticeStore()

const formModel = reactive({
  title: '',
  description: '',
  priority: 'MEDIUM',
  dueAtInput: '',
})

function resetForm() {
  formModel.title = ''
  formModel.description = ''
  formModel.priority = 'MEDIUM'
  formModel.dueAtInput = ''
}

function close() {
  emit('update:visible', false)
  resetForm()
}

async function submit() {
  if (!formModel.title.trim()) {
    noticeStore.show('任务标题不能为空')
    return
  }
  if (formModel.title.length > 100) {
    noticeStore.show('任务标题不能超过 100 个字符')
    return
  }
  if (formModel.description.length > 2000) {
    noticeStore.show('任务描述不能超过 2000 个字符')
    return
  }
  try {
    await createTask({
      title: formModel.title,
      description: formModel.description,
      status: 'TODO',
      priority: formModel.priority,
      dueAt: normalizeDueAt(formModel.dueAtInput) ?? '',
    })
    noticeStore.show('创建成功', 'success')
    close()
    emit('refresh')
  } catch (error) {
    const status = axios.isAxiosError(error) ? error.response?.status : undefined
    if (status !== 401 && status !== 403) {
      noticeStore.show(getApiErrorMessage(error))
    }
  }
}
</script>
