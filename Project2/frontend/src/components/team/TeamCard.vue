<template>
  <article
    class="team-card"
    role="button"
    tabindex="0"
    @click="emit('click')"
    @keydown.enter.prevent="emit('click')"
    @keydown.space.prevent="emit('click')"
  >
    <h3 class="team-card__title">{{ name }}</h3>
    <p class="team-card__meta">
      <span class="team-card__role">{{ roleLabel }}</span>
      <span class="team-card__count">{{ memberCount }} 名成员</span>
    </p>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { TeamRole } from '../../types/team'

const props = defineProps<{
  name: string
  role: TeamRole
  memberCount: number
}>()

const emit = defineEmits<{
  (e: 'click'): void
}>()

const roleLabel = computed(() => {
  const map: Record<TeamRole, string> = {
    OWNER: '负责人',
    ADMIN: '管理员',
    MEMBER: '成员',
  }
  return map[props.role] ?? props.role
})
</script>

<style scoped>
.team-card {
  padding: 20px 22px;
  border-radius: 16px;
  background: rgba(255, 251, 245, 0.92);
  border: 1px solid rgba(112, 130, 114, 0.22);
  box-shadow: 0 12px 28px rgba(84, 76, 56, 0.1);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.team-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 18px 36px rgba(84, 76, 56, 0.14);
}

.team-card__title {
  margin: 0 0 10px;
  font-size: 1.15rem;
  color: #203032;
}

.team-card__meta {
  margin: 0;
  display: flex;
  flex-wrap: wrap;
  gap: 10px 16px;
  font-size: 0.9rem;
  color: #5c6a69;
}

.team-card__role {
  font-weight: 700;
  color: var(--color-primary);
}

.team-card__count {
  color: #6a7a78;
}
</style>
