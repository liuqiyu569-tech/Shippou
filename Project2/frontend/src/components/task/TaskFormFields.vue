<template>
  <div class="task-form-fields" :class="`task-form-fields--${layout}`">
    <div v-if="showTitle" class="form-item">
      <label>{{ labels.title }}</label>
      <input
        v-model="form.title"
        type="text"
        :placeholder="labels.titlePlaceholder"
        :disabled="fieldDisabled"
        :maxlength="titleMaxLength"
        :required="requireTitle"
      />
    </div>
    <div v-if="showDescription" class="form-item">
      <label>{{ labels.description }}</label>
      <textarea
        v-model="form.description"
        :placeholder="labels.descriptionPlaceholder"
        :disabled="fieldDisabled"
        :maxlength="descriptionMaxLength"
      ></textarea>
    </div>
    <div v-if="showStatus" class="form-item">
      <label>{{ labels.status }}</label>
      <select v-model="form.status">
        <option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">
          {{ opt.label }}
        </option>
      </select>
    </div>
    <div v-if="showPriority" class="form-item">
      <label>{{ labels.priority }}</label>
      <select v-model="form.priority" :disabled="fieldDisabled">
        <option v-for="opt in priorityOptions" :key="opt.value" :value="opt.value">
          {{ opt.label }}
        </option>
      </select>
    </div>
    <div v-if="showDue" class="form-item">
      <label>{{ labels.due }}</label>
      <input
        v-model="dueModel"
        :type="dueInputType"
        :step="dueInputType === 'datetime-local' ? 1 : undefined"
        :disabled="fieldDisabled"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

export type TaskFormFieldsModel = {
  title: string
  description: string
  status?: string
  priority: string
  dueDate?: string
  dueAtInput?: string
}

const props = withDefaults(
  defineProps<{
    form: TaskFormFieldsModel
    layout?: 'stack' | 'grid'
    showStatus?: boolean
    showTitle?: boolean
    showDescription?: boolean
    showPriority?: boolean
    showDue?: boolean
    statusOnly?: boolean
    dueInputType?: 'date' | 'datetime-local'
    statusVariant?: 'cn' | 'enum'
    priorityVariant?: 'cn' | 'enum'
    titleMaxLength?: number
    descriptionMaxLength?: number
    requireTitle?: boolean
  }>(),
  {
    layout: 'stack',
    showStatus: false,
    showTitle: true,
    showDescription: true,
    showPriority: true,
    showDue: true,
    statusOnly: false,
    dueInputType: 'date',
    statusVariant: 'cn',
    priorityVariant: 'cn',
    titleMaxLength: 100,
    descriptionMaxLength: 2000,
    requireTitle: true,
  },
)

const fieldDisabled = computed(() => props.statusOnly)

const labels = computed(() => ({
  title: '任务标题',
  titlePlaceholder: '请输入标题',
  description: '任务描述',
  descriptionPlaceholder: '请输入描述',
  status: '状态',
  priority: '优先级',
  due: props.dueInputType === 'datetime-local' ? '截止时间' : '截止日期',
}))

const statusOptions = computed(() => {
  if (props.statusVariant === 'enum') {
    return [
      { value: 'TODO', label: 'TODO' },
      { value: 'IN_PROGRESS', label: 'IN_PROGRESS' },
      { value: 'DONE', label: 'DONE' },
    ]
  }
  return [
    { value: 'TODO', label: '待办' },
    { value: 'IN_PROGRESS', label: '进行中' },
    { value: 'DONE', label: '已完成' },
  ]
})

const priorityOptions = computed(() => {
  if (props.priorityVariant === 'enum') {
    return [
      { value: 'LOW', label: 'LOW' },
      { value: 'MEDIUM', label: 'MEDIUM' },
      { value: 'HIGH', label: 'HIGH' },
    ]
  }
  return [
    { value: 'HIGH', label: '高' },
    { value: 'MEDIUM', label: '中' },
    { value: 'LOW', label: '低' },
  ]
})

const dueModel = computed({
  get() {
    if (props.dueInputType === 'datetime-local') {
      return props.form.dueAtInput ?? ''
    }
    return props.form.dueDate ?? ''
  },
  set(value: string) {
    if (props.dueInputType === 'datetime-local') {
      props.form.dueAtInput = value
    } else {
      props.form.dueDate = value
    }
  },
})
</script>

<style scoped>
.task-form-fields--stack {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.task-form-fields--grid {
  display: grid;
  gap: 8px;
}

.form-item {
  margin-bottom: 16px;
}

.task-form-fields--grid .form-item {
  margin-bottom: 0;
}

.form-item label {
  display: block;
  margin-bottom: 4px;
  font-weight: bold;
  color: #333;
}

.task-form-fields--grid .form-item label {
  font-weight: 700;
  color: #29473d;
  font-size: 0.9rem;
}

.form-item input,
.form-item textarea,
.form-item select {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  box-sizing: border-box;
  font-family: inherit;
  font-size: 14px;
}

.form-item textarea {
  min-height: 80px;
  resize: vertical;
}

.task-form-fields--grid .form-item textarea {
  min-height: 90px;
}
</style>
