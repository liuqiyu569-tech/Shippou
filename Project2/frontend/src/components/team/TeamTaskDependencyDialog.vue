<template>
  <AppDialog
    :visible="visible && !!task"
    title="团队任务依赖关系"
    title-id="team-task-dependency-title"
    :disable-close="busy"
    wide
    @close="close"
  >
    <div v-if="task" class="dependency-panel">
      <div class="task-context">
        <span class="context-label">当前任务</span>
        <strong>{{ task.title }}</strong>
      </div>

      <p v-if="loadError" class="error-text">{{ loadError }}</p>
      <p v-else-if="loading" class="state-text">加载中...</p>

      <template v-else>
        <section class="dependency-section">
          <h3>前置依赖</h3>
          <p v-if="!prerequisites.length" class="state-text">暂无前置依赖</p>
          <div v-for="item in prerequisites" :key="`pre-${item.id}`" class="dependency-item">
            <div class="dependency-info">
              <strong>{{ item.title }}</strong>
              <span>{{ item.status }}</span>
            </div>
            <button
              type="button"
              class="danger-btn"
              :disabled="busy || item.id === null"
              @click="removeDependency(item.id)"
            >
              删除
            </button>
          </div>
        </section>

        <section class="dependency-section">
          <h3>后置依赖</h3>
          <p v-if="!successors.length" class="state-text">暂无后置依赖</p>
          <div v-for="item in successors" :key="`post-${item.id}`" class="dependency-item">
            <div class="dependency-info">
              <strong>{{ item.title }}</strong>
              <span>{{ item.status }}</span>
            </div>
          </div>
        </section>

        <form class="add-form" @submit.prevent="addDependency">
          <label for="team-dependency-task-id">选择前置依赖任务</label>
          <div class="add-form__row">
            <select
              id="team-dependency-task-id"
              v-model="selectedDependencyTaskId"
              :disabled="busy || loadingOptions || !availableTaskOptions.length"
            >
              <option value="" disabled>
                {{ loadingOptions ? '候选任务加载中...' : '请选择任务' }}
              </option>
              <option v-for="option in availableTaskOptions" :key="option.id" :value="String(option.id)">
                {{ option.title }}
              </option>
            </select>
            <button type="submit" class="primary-btn" :disabled="busy || !selectedDependencyTaskId">
              添加依赖关系
            </button>
          </div>
          <p v-if="!loadingOptions && !availableTaskOptions.length" class="state-text">
            暂无可添加的候选任务
          </p>
        </form>
      </template>
    </div>

    <template #footer>
      <button type="button" class="btn btn--ghost" :disabled="busy" @click="close">关闭</button>
    </template>
  </AppDialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import AppDialog from '../common/AppDialog.vue'
import {
  addTeamTaskDependency,
  deleteTeamTaskDependency,
  getTeamTaskDependencies,
} from '../../api/task'
import { getTeamTaskOptions } from '../../api/team'
import { getApiErrorMessage } from '../../api/http'
import { useNoticeStore } from '../../stores/notice'
import type { TaskDependencyItem, TaskOption } from '../../types/task'
import type { TeamTask } from '../../types/team'

const props = defineProps<{
  visible: boolean
  teamId: number
  task: TeamTask | null
}>()

const emit = defineEmits<{
  (e: 'close'): void
}>()

const noticeStore = useNoticeStore()
const loading = ref(false)
const loadingOptions = ref(false)
const busy = ref(false)
const loadError = ref('')
const prerequisites = ref<TaskDependencyItem[]>([])
const successors = ref<TaskDependencyItem[]>([])
const taskOptions = ref<TaskOption[]>([])
const selectedDependencyTaskId = ref('')

const availableTaskOptions = computed(() => {
  const currentTaskId = props.task?.id
  const prerequisiteIds = new Set(prerequisites.value.map((item) => item.id))

  return taskOptions.value.filter((option) =>
    option.id !== currentTaskId && !prerequisiteIds.has(option.id),
  )
})

watch(
  () => [props.visible, props.task?.id, props.teamId] as const,
  ([visible]) => {
    if (visible && props.task) {
      loadDialogData()
    } else {
      reset()
    }
  },
)

async function loadDialogData() {
  await Promise.all([loadDependencies(), loadTaskOptions()])
}

async function loadDependencies() {
  if (!props.task) return

  loading.value = true
  loadError.value = ''
  try {
    const res = await getTeamTaskDependencies(props.teamId, props.task.id)
    prerequisites.value = res.data.prerequisites || []
    successors.value = res.data.successors || []
  } catch (error) {
    loadError.value = getApiErrorMessage(error)
  } finally {
    loading.value = false
  }
}

async function loadTaskOptions() {
  loadingOptions.value = true
  try {
    const res = await getTeamTaskOptions(props.teamId)
    taskOptions.value = res.data || []
  } catch (error) {
    noticeStore.show(getApiErrorMessage(error))
    taskOptions.value = []
  } finally {
    loadingOptions.value = false
  }
}

async function addDependency() {
  if (!props.task) return

  const dependencyTaskId = Number(selectedDependencyTaskId.value)
  if (!Number.isInteger(dependencyTaskId) || dependencyTaskId <= 0) {
    noticeStore.show('请选择要添加的前置依赖任务')
    return
  }

  busy.value = true
  try {
    await addTeamTaskDependency(props.teamId, props.task.id, dependencyTaskId)
    selectedDependencyTaskId.value = ''
    noticeStore.show('依赖关系已添加', 'success')
    await loadDependencies()
  } catch (error) {
    noticeStore.show(getApiErrorMessage(error))
  } finally {
    busy.value = false
  }
}

async function removeDependency(dependencyTaskId: number | null) {
  if (!props.task || dependencyTaskId === null) return

  busy.value = true
  try {
    await deleteTeamTaskDependency(props.teamId, props.task.id, dependencyTaskId)
    noticeStore.show('依赖关系已删除', 'success')
    await loadDependencies()
  } catch (error) {
    noticeStore.show(getApiErrorMessage(error))
  } finally {
    busy.value = false
  }
}

function close() {
  if (busy.value) return
  emit('close')
}

function reset() {
  loadError.value = ''
  prerequisites.value = []
  successors.value = []
  taskOptions.value = []
  selectedDependencyTaskId.value = ''
}
</script>

<style scoped>
.dependency-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.task-context {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #f4f7f6;
  color: #203032;
}

.context-label {
  font-size: 12px;
  color: #66706f;
}

.dependency-section h3 {
  margin: 0 0 10px;
  font-size: 15px;
  color: #203032;
}

.dependency-item {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
  padding: 10px 12px;
  border: 1px solid #e4ebe8;
  border-radius: 8px;
  background: #fff;
  margin-bottom: 8px;
}

.dependency-info {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.dependency-info strong {
  word-break: break-all;
  color: #203032;
}

.dependency-info span {
  font-size: 12px;
  color: #66706f;
}

.add-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.add-form label {
  font-weight: 700;
  color: #203032;
}

.add-form__row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.add-form select {
  flex: 1 1 180px;
  min-height: 38px;
  padding: 0 10px;
  border: 1px solid #cfd8dc;
  border-radius: 8px;
}

.primary-btn,
.danger-btn {
  min-height: 36px;
  padding: 0 14px;
  border: none;
  border-radius: 8px;
  color: #fff;
  font-weight: 700;
  cursor: pointer;
}

.primary-btn {
  background: var(--color-primary);
}

.danger-btn {
  background: var(--color-danger);
}

.primary-btn:disabled,
.danger-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.state-text,
.error-text {
  margin: 0;
  color: #66706f;
}

.error-text {
  color: #c24141;
}
</style>
