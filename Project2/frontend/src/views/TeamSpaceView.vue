<template>
  <div class="team-space">
    <TeamSpaceHeader
      :title="teamInfo.name || `团队 #${teamId}`"
      :role="userRole"
      :show-role="!loadError"
      show-back
      back-label="返回我的团队"
      @back="goBack"
    />

    <LoadState
      :loading="loading && !teamInfo.name"
      :error="loadError"
      show-back
      back-label="返回我的团队"
      @back="goBack"
    >
      <TeamInfoCard :team-id="teamId" :created-at="teamInfo.createdAt" />

      <section class="task-stats-section" aria-label="团队任务统计">
        <h3>任务面板</h3>
        <div class="task-stats">
          <p v-if="statsError" class="task-stats__error">{{ statsError }}</p>
          <template v-else>
            <div class="task-stats__item task-stats__item--total">
              <span class="task-stats__label">任务总数</span>
              <strong>{{ taskStats?.total ?? 0 }}</strong>
            </div>
            <div class="task-stats__item task-stats__item--risk">
              <span class="task-stats__label">已逾期</span>
              <strong>{{ taskStats?.overdueCount ?? 0 }}</strong>
            </div>
            <div class="task-stats__item task-stats__item--warn">
              <span class="task-stats__label">即将到期</span>
              <strong>{{ taskStats?.upcomingDueCount ?? 0 }}</strong>
            </div>
            <div class="task-stats__item">
              <span class="task-stats__label">TODO</span>
              <strong>{{ taskStats?.byStatus.TODO ?? 0 }}</strong>
            </div>
            <div class="task-stats__item">
              <span class="task-stats__label">IN_PROGRESS</span>
              <strong>{{ taskStats?.byStatus.IN_PROGRESS ?? 0 }}</strong>
            </div>
            <div class="task-stats__item">
              <span class="task-stats__label">DONE</span>
              <strong>{{ taskStats?.byStatus.DONE ?? 0 }}</strong>
            </div>
            <div class="task-stats__item">
              <span class="task-stats__label">LOW</span>
              <strong>{{ taskStats?.byPriority.LOW ?? 0 }}</strong>
            </div>
            <div class="task-stats__item">
              <span class="task-stats__label">MEDIUM</span>
              <strong>{{ taskStats?.byPriority.MEDIUM ?? 0 }}</strong>
            </div>
            <div class="task-stats__item">
              <span class="task-stats__label">HIGH</span>
              <strong>{{ taskStats?.byPriority.HIGH ?? 0 }}</strong>
            </div>
          </template>
        </div>
      </section>

      <section class="member-section">
        <TeamMemberPanel
          :team-id="teamId"
          :my-role="userRole"
          :members="memberList"
          :locked="loading"
          :on-refresh="reloadAll"
          @refresh="reloadAll"
          @role-downgraded="taskModals.closeAllTaskModals"
          @busy-change="memberPanelBusy = $event"
        />
      </section>

      <div class="team-task-filters">
        <input
          v-model="keyword"
          class="team-task-filters__keyword"
          type="search"
          placeholder="搜索标题或描述"
          :disabled="loading"
          @keyup.enter="onFilterChange"
        />
        <select v-model="filterStatus" :disabled="loading" @change="onFilterChange">
          <option value="">所有状态</option>
          <option value="TODO">待处理</option>
          <option value="IN_PROGRESS">进行中</option>
          <option value="DONE">已完成</option>
        </select>
        <select v-model="filterPriority" :disabled="loading" @change="onFilterChange">
          <option value="">所有优先级</option>
          <option value="HIGH">高</option>
          <option value="MEDIUM">中</option>
          <option value="LOW">低</option>
        </select>
        <select v-model="filterAssignee" :disabled="loading" @change="onFilterChange">
          <option value="">全部成员</option>
          <option v-for="member in memberList" :key="member.userId" :value="String(member.userId)">
            {{ member.username }}
          </option>
        </select>
        <div class="team-task-filters__date">
          <div class="team-task-filters__date-range">
            <span>截止时间</span>
            <input
              v-model="filterDueStart"
              type="datetime-local"
              step="1"
              :disabled="loading"
            />
            <span class="team-task-filters__sep">-</span>
            <input
              v-model="filterDueEnd"
              type="datetime-local"
              step="1"
              :disabled="loading"
            />
          </div>
          <div class="team-task-filters__actions">
            <button
              type="button"
              class="team-task-filters__search"
              :disabled="loading"
              @click="onFilterChange"
            >
              查询
            </button>
            <button
              type="button"
              class="team-task-filters__clear"
              :disabled="loading"
              @click="clearKeyword"
            >
              清空
            </button>
          </div>
        </div>
      </div>

      <TeamTaskList
        :tasks="teamTasks"
        :loading="loading"
        :can-manage-tasks="canManageTasks"
        :actions-disabled="loading || taskModals.busy || memberPanelBusy"
        :is-assigned-to-me="isAssignedToMe"
        :member-editing-task-id="taskModals.memberEditingTaskId"
        :member-status-draft="taskModals.memberStatusDraft"
        @create="taskModals.openCreate"
        @view-detail="taskModals.openTaskDetail"
        @edit="taskModals.openEdit"
        @remove="taskModals.removeTask"
        @assign="taskModals.openAssign"
        @manage-dependencies="openTaskDependencies"
        @view-graph="openDependencyGraph"
        @view-logs="openTaskLogs"
        @start-status="taskModals.startAssignedTaskStatus"
        @save-status="taskModals.saveAssignedTaskStatus"
        @cancel-status="taskModals.cancelAssignedTaskStatus"
        @update-status-draft="taskModals.setMemberStatusDraft"
      />
      <ListPager
        v-if="pageResult"
        variant="teams"
        :page="page"
        :total-pages="totalPages"
        :total="pageResult.total"
        :loading="loading"
        @change="changePage"
      />

      <TeamTaskDetailDialog
        :visible="taskModals.showTaskDetail"
        :task="taskModals.detailTask"
        :team-id="teamId"
        @close="taskModals.closeTaskDetail"
      />

      <TeamTaskFormDialog
        :visible="taskModals.showCreate"
        mode="create"
        :form="taskModals.taskForm"
        :busy="taskModals.busy"
        @close="taskModals.closeCreate"
        @submit="taskModals.submitCreate"
      />

      <TeamTaskFormDialog
        :visible="taskModals.showEdit"
        mode="edit"
        :form="taskModals.taskForm"
        :busy="taskModals.busy"
        @close="taskModals.closeEdit"
        @submit="taskModals.submitEdit"
      />

      <TeamTaskAssignDialog
        v-model:selected-ids="taskModals.selectedAssigneeIds"
        :visible="taskModals.showAssign"
        :members="memberList"
        :busy="taskModals.busy"
        @close="taskModals.closeAssign"
        @submit="taskModals.submitAssign"
      />

      <TeamTaskDependencyDialog
        :visible="showTaskDependencies"
        :team-id="teamId"
        :task="dependencyTask"
        @close="closeTaskDependencies"
      />
    </LoadState>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import LoadState from '../components/common/LoadState.vue'
import TeamMemberPanel from '../components/team/TeamMemberPanel.vue'
import TeamSpaceHeader from '../components/team/TeamSpaceHeader.vue'
import TeamInfoCard from '../components/team/TeamInfoCard.vue'
import TeamTaskList from '../components/team/TeamTaskList.vue'
import TeamTaskDetailDialog from '../components/team/TeamTaskDetailDialog.vue'
import TeamTaskFormDialog from '../components/team/TeamTaskFormDialog.vue'
import TeamTaskAssignDialog from '../components/team/TeamTaskAssignDialog.vue'
import TeamTaskDependencyDialog from '../components/team/TeamTaskDependencyDialog.vue'
import ListPager from '../components/common/ListPager.vue'
import { useTeamSpace } from '../composables/useTeamSpace'
import { useTeamTaskModals } from '../composables/useTeamTaskModals'
import type { TeamTask } from '../types/team'

const route = useRoute()
const router = useRouter()

const teamId = computed(() => Number(route.params.teamId))
const showTaskDependencies = ref(false)
const dependencyTask = ref<TeamTask | null>(null)

const closeModalsRef: { fn: () => void } = { fn: () => {} }

const {
  loading,
  loadError,
  memberPanelBusy,
  teamInfo,
  userRole,
  memberList,
  teamTasks,
  taskStats,
  statsError,
  page,
  pageResult,
  filterStatus,
  filterPriority,
  filterAssignee,
  filterDueStart,
  filterDueEnd,
  keyword,
  canManageTasks,
  totalPages,
  onFilterChange,
  clearKeyword,
  changePage,
  reloadAll,
  isAssignedToMe,
  goBack,
} = useTeamSpace(teamId, {
  onRecovering: () => closeModalsRef.fn(),
  onLoadFailed: () => closeModalsRef.fn(),
})

const taskModals = useTeamTaskModals({
  teamId,
  reloadAll,
  loadError,
})

closeModalsRef.fn = () => {
  taskModals.closeAllTaskModals()
  closeTaskDependencies()
}

function openTaskDependencies(task: TeamTask) {
  dependencyTask.value = task
  showTaskDependencies.value = true
}

function closeTaskDependencies() {
  showTaskDependencies.value = false
  dependencyTask.value = null
}

function openDependencyGraph() {
  router.push({ name: 'team-task-graph', params: { teamId: teamId.value } })
}

function openTaskLogs() {
  router.push({ name: 'team-task-logs', params: { teamId: String(teamId.value) } })
}
</script>

<style scoped>
.team-space {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
}

.member-section {
  margin-bottom: 24px;
}

.task-stats-section {
  margin: 0 0 24px;
}

.task-stats-section h3 {
  margin: 0 0 14px;
  font-size: 1.1rem;
  color: #203032;
}

.task-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin: 0;
}

.task-stats__item,
.task-stats__error {
  min-height: 64px;
  padding: 10px 12px;
  border: 1px solid #d9e4e2;
  border-radius: 8px;
  background: #fff;
}

.task-stats__label {
  display: block;
  margin-bottom: 6px;
  color: #667675;
  font-size: 12px;
  font-weight: 700;
}

.task-stats strong {
  font-size: 22px;
  color: #1f2f2d;
}

.task-stats__item--total {
  background: #eef7f5;
  border-color: var(--color-primary-border);
}

.task-stats__item--risk strong {
  color: #a72525;
}

.task-stats__item--warn strong {
  color: #9a5b00;
}

.task-stats__error {
  grid-column: 1 / -1;
  color: #a72525;
}

.team-task-filters {
  display: grid;
  grid-template-columns: 200px repeat(3, 110px) minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  margin-top: 30px;
  margin-bottom: 16px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.team-task-filters select,
.team-task-filters__keyword {
  width: 110px;
  min-height: 38px;
  padding: 0 6px;
  border-radius: 8px;
  border: 1px solid #cfd8dc;
  background: #fff;
  font-size: 14px;
}

.team-task-filters__keyword {
  width: 200px;
  padding: 0 10px;
  box-sizing: border-box;
}

.team-task-filters input:not(.team-task-filters__keyword) {
  width: 148px;
  min-height: 38px;
  padding: 0 8px;
  border-radius: 8px;
  border: 1px solid #cfd8dc;
  background: #fff;
  font-size: 14px;
}

.team-task-filters__date {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  grid-column: 1 / -1;
  margin-left: 0;
}

.team-task-filters__date-range,
.team-task-filters__actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.team-task-filters__date-range span {
  color: #566463;
  font-weight: 600;
}

.team-task-filters__sep {
  color: #8a9896;
}

.team-task-filters__search,
.team-task-filters__clear {
  min-height: 38px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid var(--color-primary-border);
  font-weight: 700;
  cursor: pointer;
}

.team-task-filters__search {
  background: var(--color-primary);
  color: #fff;
}

.team-task-filters__clear {
  background: #fff;
  color: var(--color-primary);
  border-color: var(--color-primary-border);
}

.team-task-filters__search:disabled,
.team-task-filters__clear:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 760px) {
  .task-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .team-task-filters {
    display: flex;
    flex-wrap: wrap;
  }

  .team-task-filters__date {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
