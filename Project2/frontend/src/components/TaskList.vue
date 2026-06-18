<template>
  <TaskHubList 
    :tasks="tasks" 
    :team-role-map="teamRoleMap" 
    @refresh="$emit('refresh')"
    @open-dependency="$emit('open-dependency', $event)"
  />
</template>

<script setup lang="ts">
import TaskHubList from './task/TaskHubList.vue'
import type { Task } from '../types/task'
import type { TeamRole } from '../types/team'

withDefaults(
  defineProps<{
    tasks: Task[]
    teamRoleMap?: Record<number, TeamRole>
  }>(),
  {
    teamRoleMap: () => ({}),
  },
)

defineEmits<{
  (e: 'refresh'): void
  (e: 'open-dependency', taskId: number): void
}>()
</script>