<template>
  <div class="logs-page">
    <AppPageHeader :show-nav="false">
      <template #title>
        <button type="button" class="page-back" @click="goBack">返回</button>
        <h2 class="page-title">团队任务日志</h2>
      </template>
    </AppPageHeader>

    <p class="page-hint">可查看已删除团队任务的历史操作记录。</p>

    <LoadState :loading="false" :error="invalidTeam ? '团队不存在' : ''" show-back @back="goBack">
      <TaskOperationLogPanel
        v-if="!invalidTeam"
        :mode="logMode"
        variant="page"
        :active="true"
        show-filters
        show-task-column
        show-team-column
        allow-assignee-filter
      />
    </LoadState>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppPageHeader from '@/components/common/AppPageHeader.vue'
import LoadState from '@/components/common/LoadState.vue'
import TaskOperationLogPanel from '@/components/task/TaskOperationLogPanel.vue'
import type { LogQueryMode } from '@/types/taskLog'

const route = useRoute()
const router = useRouter()

const teamId = computed(() => Number(route.params.teamId))
const invalidTeam = computed(() => !Number.isFinite(teamId.value) || teamId.value <= 0)
const logMode = computed<LogQueryMode>(() => ({ kind: 'team-all', teamId: teamId.value }))

function goBack() {
  if (invalidTeam.value) {
    void router.push({ name: 'my-teams' })
    return
  }
  void router.push({ name: 'team-space', params: { teamId: String(teamId.value) } })
}
</script>

<style scoped>
.logs-page {
  max-width: 960px;
  margin: 0 auto;
  padding: 24px;
}

.page-back {
  min-height: 36px;
  padding: 0 16px;
  border-radius: 999px;
  border: 1px solid var(--color-primary-border);
  background: var(--color-primary-soft);
  color: var(--color-primary);
  font-weight: 700;
  cursor: pointer;
}

.page-title {
  margin: 0;
  font-size: clamp(1.5rem, 3vw, 2rem);
  color: #203032;
}

.page-hint {
  margin: 0 0 16px;
  color: #666;
  font-size: 14px;
}
</style>
