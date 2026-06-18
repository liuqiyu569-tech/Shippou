<template>
  <div class="task-page">
    <AppPageHeader title="任务管理中心" />

    <div class="filter-bar">
      <div class="filter-bar__row">
        <button :class="{ active: scope === 'all' }" @click="selectScope('all')">全部</button>
        <button :class="{ active: scope === 'personal' }" @click="selectScope('personal')">个人任务</button>
        <button :class="{ active: scope === 'team' }" @click="selectScope('team')">团队任务</button>
      </div>
      <div class="filter-bar__row">
        <input
          v-model="keyword"
          class="filter-bar__keyword"
          type="search"
          placeholder="搜索标题或描述"
          @keyup.enter="onFilterChange"
        />
        <select v-model="filterStatus" @change="onFilterChange">
          <option value="">所有状态</option>
          <option value="TODO">待处理</option>
          <option value="IN_PROGRESS">进行中</option>
          <option value="DONE">已完成</option>
        </select>
        <select v-model="filterPriority" @change="onFilterChange">
          <option value="">所有优先级</option>
          <option value="HIGH">高</option>
          <option value="MEDIUM">中</option>
          <option value="LOW">低</option>
        </select>
        <div class="filter-bar__date">
          <span>截止时间</span>
          <input v-model="filterDueStart" type="datetime-local" step="1" />
          <span class="filter-bar__sep">-</span>
          <input v-model="filterDueEnd" type="datetime-local" step="1" />
        </div>
      </div>
    </div>

    <p v-if="scope === 'team'" class="scope-hint">
      此处仅显示<strong>已指派给你</strong>的团队任务；管理全部团队任务请前往
      <RouterLink :to="{ name: 'my-teams' }">我的团队</RouterLink>
      → 对应团队空间。
    </p>

    <div class="tool-bar">
      <template v-if="scope !== 'team'">
        <button class="add-btn" @click="showForm = true">+ 新建个人任务</button>
        <RouterLink class="graph-btn" to="/task-graph">依赖图谱</RouterLink>
        <RouterLink class="graph-btn" to="/task-logs">个人任务日志</RouterLink>
      </template>
      <button type="button" class="query-btn" @click="onFilterChange">查询</button>
      <button type="button" class="clear-btn" @click="clearKeyword">清空</button>
    </div>

    <div class="task-container">
      <LoadState :loading="loading" :error="loadError">
        <TaskList
          :tasks="taskList"
          :team-role-map="teamRoleMap"
          @refresh="fetchTaskList"
          @open-dependency="openDependency"
        />
        <ListPager
          v-if="pageResult"
          :page="page"
          :total-pages="totalPages"
          :total="pageResult.total"
          :loading="loading"
          @change="changePage"
        />
      </LoadState>
    </div>

    <TaskForm v-model:visible="showForm" @refresh="fetchTaskList" />

    <AppDialog
      :visible="depVisible"
      title="任务依赖管理"
      title-id="personal-task-dependency-title"
      wide
      @close="closeDependencyDialog"
    >
      <div class="dep-section">
        <h4>前置依赖（我依赖的任务）</h4>
        <div v-for="(item, index) in preDependencies" :key="item.id ?? `pre-${index}`" class="dep-item">
          <span>{{ item.title }} ({{ item.status }})</span>
          <button
            type="button"
            class="danger-btn"
            :disabled="item.id === null"
            @click="deleteDependency(currentTaskId, item.id)"
          >
            删除
          </button>
        </div>
        <p v-if="preDependencies.length === 0">暂无前置依赖</p>
      </div>

      <div class="dep-section">
        <h4>后置依赖（依赖我的任务）</h4>
        <div v-for="(item, index) in postDependencies" :key="item.id ?? `post-${index}`" class="dep-item">
          <span>{{ item.title }} ({{ item.status }})</span>
        </div>
        <p v-if="postDependencies.length === 0">暂无后置依赖</p>
      </div>

      <div class="add-dep-form">
        <select
          v-model="newDepTaskId"
          :disabled="dependencyOptionsLoading || availableDependencyOptions.length === 0"
        >
          <option value="" disabled>
            {{ dependencyOptionsLoading ? '候选任务加载中...' : '请选择前置依赖任务' }}
          </option>
          <option v-for="option in availableDependencyOptions" :key="option.id" :value="String(option.id)">
            {{ option.title }}
          </option>
        </select>
        <button type="button" class="primary-btn" :disabled="!newDepTaskId" @click="addDependency">
          添加依赖
        </button>
      </div>
      <p v-if="!dependencyOptionsLoading && availableDependencyOptions.length === 0" class="dep-empty">
        暂无可添加的候选任务
      </p>
      <template #footer>
        <button type="button" class="btn btn--ghost" @click="closeDependencyDialog">关闭</button>
      </template>
    </AppDialog>
  </div>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'
import AppPageHeader from '@/components/common/AppPageHeader.vue'
import ListPager from '@/components/common/ListPager.vue'
import LoadState from '@/components/common/LoadState.vue'
import AppDialog from '@/components/common/AppDialog.vue'
import TaskList from '@/components/TaskList.vue'
import TaskForm from '@/components/TaskForm.vue'
import { ref } from 'vue'
import { computed } from 'vue'
import {
  getPersonalTaskOptions,
  getTaskDependencies,
  addTaskDependency,
  deleteTaskDependency
} from '@/api/task'
import { getApiErrorMessage } from '@/api/http'
import { useTaskHubPage } from '@/composables/useTaskHubPage'
import { useNoticeStore } from '@/stores/notice'
import type { TaskDependencyItem, TaskOption } from '@/types/task'

// 原有页面逻辑
const {
  taskList,
  showForm,
  teamRoleMap,
  scope,
  filterStatus,
  filterPriority,
  filterDueStart,
  filterDueEnd,
  keyword,
  page,
  pageResult,
  loadError,
  loading,
  totalPages,
  fetchTaskList,
  selectScope,
  onFilterChange,
  clearKeyword,
  changePage,
} = useTaskHubPage()

// 任务依赖相关逻辑
const depVisible = ref(false)
const currentTaskId = ref(0)
const preDependencies = ref<TaskDependencyItem[]>([])
const postDependencies = ref<TaskDependencyItem[]>([])
const dependencyOptions = ref<TaskOption[]>([])
const dependencyOptionsLoading = ref(false)
const newDepTaskId = ref('')
const noticeStore = useNoticeStore()

const availableDependencyOptions = computed(() => {
  const prerequisiteIds = new Set(preDependencies.value.map((item) => item.id))
  return dependencyOptions.value.filter((option) =>
    option.id !== currentTaskId.value && !prerequisiteIds.has(option.id),
  )
})

// 打开依赖管理面板
const openDependency = async (taskId: number) => {
  currentTaskId.value = taskId
  depVisible.value = true
  await Promise.all([loadDependencies(taskId), loadDependencyOptions()])
}

const loadDependencies = async (taskId: number) => {
  const res = await getTaskDependencies(taskId)
  preDependencies.value = res.data.prerequisites || []
  postDependencies.value = res.data.successors || []
}

const loadDependencyOptions = async () => {
  dependencyOptionsLoading.value = true
  try {
    const res = await getPersonalTaskOptions()
    dependencyOptions.value = res.data || []
  } finally {
    dependencyOptionsLoading.value = false
  }
}

// 添加依赖
const addDependency = async () => {
  const depId = Number(newDepTaskId.value)
  if (!depId) return
  try {
    await addTaskDependency(currentTaskId.value, depId)
    await loadDependencies(currentTaskId.value)
    newDepTaskId.value = ''
    noticeStore.show('依赖关系已添加', 'success')
  } catch (error) {
    noticeStore.show(getApiErrorMessage(error))
  }
}

// 删除依赖
const deleteDependency = async (taskId: number, depId: number | null) => {
  if (depId === null) return
  try {
    await deleteTaskDependency(taskId, depId)
    await loadDependencies(taskId)
    noticeStore.show('依赖关系已删除', 'success')
  } catch (error) {
    noticeStore.show(getApiErrorMessage(error))
  }
}

const closeDependencyDialog = () => {
  depVisible.value = false
  resetDependencyData()
}

// 重置数据
const resetDependencyData = () => {
  preDependencies.value = []
  postDependencies.value = []
  dependencyOptions.value = []
  newDepTaskId.value = ''
}
</script>

<style scoped>
.task-page {
  max-width: 960px;
  margin: 0 auto;
  padding: 24px 18px 48px;
}
.filter-bar {
  margin: 20px 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.filter-bar__row {
  display: flex;
  gap: 10px;
  flex-wrap: nowrap;
  align-items: center;
  min-width: 0;
}
.filter-bar button {
  padding: 6px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: #fff;
  cursor: pointer;
}
.filter-bar button.active {
  background: var(--color-primary);
  color: #fff;
  border-color: var(--color-primary);
}
.filter-bar select,
.filter-bar__keyword {
  padding: 6px 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: inherit;
  box-sizing: content-box;
}
.filter-bar__keyword {
  width: 180px;
  flex: 0 0 180px;
}
.filter-bar__date {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-left: 0;
  min-width: 0;
}
.filter-bar__date span {
  color: #566463;
  font-weight: 600;
  font-size: 14px;
}
.filter-bar__date input {
  width: 148px;
  padding: 6px 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: inherit;
  box-sizing: content-box;
}
.filter-bar__sep {
  color: #8a9896;
}
@media (max-width: 860px) {
  .filter-bar__row {
    flex-wrap: wrap;
  }
}
.scope-hint {
  padding: 10px;
  background: #f5f5f5;
  border-radius: 4px;
  color: #666;
}
.add-btn {
  background: var(--color-primary);
  color: #fff;
  border: none;
}
.task-container {
  margin-top: 20px;
}
.tool-bar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}
.add-btn,
.graph-btn,
.query-btn,
.clear-btn {
  min-height: 38px;
  padding: 8px 16px;
  border: 1px solid var(--color-primary-border);
  border-radius: 4px;
  font-weight: 700;
  text-decoration: none;
  cursor: pointer;
  font-size: inherit;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  line-height: 1;
}
.graph-btn,
.clear-btn {
  background: var(--color-primary-soft);
  color: var(--color-primary);
}
.add-btn {
  border-color: var(--color-primary);
}
.query-btn {
  margin-left: auto;
  background: var(--color-primary);
  color: #fff;
}
.clear-btn {
  border-color: #d6e0df;
}
.dep-section {
  margin-bottom: 16px;
}
.dep-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  padding: 8px 12px;
  background: #f5fafe;
  border-radius: 4px;
  margin-bottom: 8px;
}
.add-dep-form {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
  margin-top: 16px;
}
.add-dep-form select {
  flex: 1 1 240px;
  min-height: 38px;
  padding: 0 10px;
  border: 1px solid #cfd8dc;
  border-radius: 8px;
  background: #fff;
}
.primary-btn,
.danger-btn {
  min-height: 34px;
  padding: 0 12px;
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
.dep-empty {
  margin: 8px 0 0;
  color: #999;
}
</style>
