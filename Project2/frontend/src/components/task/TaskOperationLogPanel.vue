<template>
  <div class="log-panel" :class="`log-panel--${variant}`">
    <div v-if="showFilters" class="log-filters">
      <select v-model="filterOperationType" @change="onFilterChange">
        <option value="">所有操作类型</option>
        <option value="CREATE">创建</option>
        <option value="UPDATE">更新</option>
        <option value="DELETE">删除</option>
      </select>
      <select v-model="filterObjectType" @change="onFilterChange">
        <option value="">所有对象类型</option>
        <option value="TASK_INFO">任务信息</option>
        <option value="TASK_DEPENDENCY">任务依赖</option>
        <option v-if="allowAssigneeFilter" value="TASK_ASSIGNEE">负责人</option>
      </select>
      <input
        v-model="filterKeyword"
        type="text"
        class="log-filters__keyword"
        placeholder="按任务标题搜索"
        @keyup.enter="onFilterChange"
      />
      <div class="log-filters__date">
        <span>操作时间</span>
        <input v-model="filterStartTime" type="datetime-local" />
        <span class="log-filters__sep">-</span>
        <input v-model="filterEndTime" type="datetime-local" />
      </div>
      <button type="button" class="log-filters__search" @click="onFilterChange">查询</button>
      <button type="button" class="log-filters__clear" @click="clearFilters">清空</button>
    </div>

    <p v-if="loading" class="state-text">加载中…</p>
    <p v-else-if="loadError" class="error-text">{{ loadError }}</p>
    <p v-else-if="isEmpty" class="state-text">暂无操作记录</p>

    <ul v-else class="log-list">
      <li v-for="log in items" :key="log.id" class="log-item">
        <div class="log-item__header">
          <span class="log-item__operator">{{ log.operator.username }}</span>
          <span class="log-item__sep">·</span>
          <span class="log-item__time">{{ formatDate(log.operationTime) }}</span>
        </div>
        <div class="log-item__tags">
          <span class="log-tag">{{ formatOperationType(log.operationType) }}</span>
          <span class="log-tag log-tag--muted">{{ formatObjectType(log.objectType) }}</span>
        </div>
        <p class="log-item__summary" :title="log.summary">{{ truncate(log.summary, 200) }}</p>
        <p v-if="showTaskColumn && log.task" class="log-item__meta">
          任务：{{ log.task.title }}
        </p>
        <p v-if="showTeamColumn && log.team" class="log-item__meta">
          团队：{{ log.team.name }}
        </p>
      </li>
    </ul>

    <ListPager
      v-if="pageResult && pageResult.total > 0"
      :page="page"
      :total-pages="totalPages"
      :total="pageResult.total"
      :loading="loading"
      :variant="pagerVariant"
      @change="changePage"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, toRef, watch } from 'vue'
import ListPager from '../common/ListPager.vue'
import { useTaskOperationLogs } from '../../composables/useTaskOperationLogs'
import { formatDate, truncate } from '../../utils/format'
import { formatObjectType, formatOperationType } from '../../utils/taskLogLabels'
import type { LogQueryMode } from '../../types/taskLog'

const props = withDefaults(
  defineProps<{
    mode: LogQueryMode
    variant?: 'embedded' | 'page'
    active?: boolean
    showFilters?: boolean
    showTaskColumn?: boolean
    showTeamColumn?: boolean
    allowAssigneeFilter?: boolean
  }>(),
  {
    variant: 'embedded',
    active: true,
    showFilters: false,
    showTaskColumn: false,
    showTeamColumn: false,
    allowAssigneeFilter: false,
  },
)

const modeRef = toRef(props, 'mode')

const {
  loading,
  loadError,
  page,
  pageResult,
  filterOperationType,
  filterObjectType,
  filterKeyword,
  filterStartTime,
  filterEndTime,
  items,
  totalPages,
  isEmpty,
  fetchLogs,
  onFilterChange,
  changePage,
  reset,
  clearFilters,
} = useTaskOperationLogs(() => modeRef.value)

const pagerVariant = computed(() =>
  props.mode.kind === 'team-all' || props.mode.kind === 'team-task' ? 'teams' : 'default',
)

watch(
  () => props.mode,
  () => {
    if (props.active) {
      reset()
      void fetchLogs()
    }
  },
  { immediate: true, deep: true },
)

watch(
  () => props.active,
  (visible) => {
    if (visible) {
      reset()
      void fetchLogs()
    }
  },
)
</script>

<style scoped>
.log-panel--embedded {
  margin-top: 8px;
}

.log-panel--embedded .log-list {
  max-height: 240px;
  overflow-y: auto;
}

.log-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-bottom: 12px;
}

.log-filters select,
.log-filters input[type='text'],
.log-filters input[type='datetime-local'] {
  padding: 6px 10px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
}

.log-filters__keyword {
  min-width: 160px;
}

.log-filters__date {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.log-filters__sep {
  color: #999;
}

.log-filters__search,
.log-filters__clear {
  padding: 6px 14px;
  border-radius: 6px;
  border: none;
  cursor: pointer;
  font-size: 14px;
}

.log-filters__search {
  background: var(--color-primary);
  color: #fffaf2;
  font-weight: 600;
}

.log-filters__clear {
  background: #f0f0f0;
  color: #555;
}

.state-text {
  color: #888;
  margin: 8px 0;
  text-align: center;
}

.error-text {
  color: #c45656;
  margin: 8px 0;
}

.log-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.log-item {
  padding: 10px 0;
  border-bottom: 1px solid #eee;
}

.log-item:last-child {
  border-bottom: none;
}

.log-item__header {
  font-size: 14px;
  color: #333;
  margin-bottom: 4px;
}

.log-item__operator {
  font-weight: 600;
}

.log-item__sep {
  margin: 0 4px;
  color: #aaa;
}

.log-item__time {
  color: #666;
}

.log-item__tags {
  display: flex;
  gap: 6px;
  margin-bottom: 4px;
}

.log-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  background: var(--color-primary-soft);
  color: var(--color-primary);
}

.log-tag--muted {
  background: #f0f0f0;
  color: #666;
}

.log-item__summary {
  margin: 4px 0 0;
  font-size: 13px;
  color: #444;
  line-height: 1.5;
  word-break: break-word;
}

.log-item__meta {
  margin: 4px 0 0;
  font-size: 12px;
  color: #888;
}
</style>
