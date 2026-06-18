<template>
  <header class="header">
    <div class="header-title">
      <button v-if="showBack" type="button" class="header-back" @click="$emit('back')">
        {{ backLabel }}
      </button>
      <h2>{{ title }}</h2>
      <div v-if="showRole" class="role-tag">{{ roleLabel(role) }}</div>
    </div>
    <AppTopNav />
  </header>
</template>

<script setup lang="ts">
import AppTopNav from '../common/AppTopNav.vue'
import { roleLabel } from '../../utils/team'
import type { TeamRole } from '../../types/team'

withDefaults(
  defineProps<{
    title: string
    role: TeamRole
    showRole?: boolean
    showBack?: boolean
    backLabel?: string
  }>(),
  {
    showBack: false,
    backLabel: '返回',
  },
)

defineEmits<{
  (e: 'back'): void
}>()

</script>

<style scoped>
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.header-title h2 {
  margin: 0;
  font-size: clamp(1.5rem, 3vw, 2rem);
  color: #203032;
}

.header-back {
  min-height: 36px;
  padding: 0 16px;
  border-radius: 999px;
  border: 1px solid var(--color-primary-border);
  background: var(--color-primary-soft);
  color: var(--color-primary);
  font-weight: 700;
  cursor: pointer;
}

.role-tag {
  padding: 4px 10px;
  background: var(--color-primary-soft);
  color: var(--color-primary);
  border-radius: 12px;
  font-size: 12px;
}

@media (max-width: 640px) {
  .header {
    align-items: stretch;
  }
}
</style>
