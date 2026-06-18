<template>
  <AppDialog
    :visible="visible"
    title="分配成员"
    title-id="team-task-assign-title"
    wide
    :disable-close="busy"
    @close="$emit('close')"
  >
    <div class="assign-list">
      <label v-for="m in members" :key="m.userId" class="assign-item">
        <input v-model="selectedIdsModel" type="checkbox" :value="m.userId" />
        <span>{{ m.username }}（{{ roleLabel(m.role) }}）</span>
      </label>
    </div>
    <template #footer>
      <button type="button" class="btn btn--ghost" :disabled="busy" @click="$emit('close')">取消</button>
      <button type="button" class="btn btn--primary" :disabled="busy" @click="$emit('submit')">保存分配</button>
    </template>
  </AppDialog>
</template>

<script setup lang="ts">
import AppDialog from '../common/AppDialog.vue'
import { roleLabel } from '../../utils/team'
import type { MemberInfo } from '../../types/team'

const selectedIdsModel = defineModel<number[]>('selectedIds', { required: true })

defineProps<{
  visible: boolean
  members: MemberInfo[]
  busy: boolean
}>()

defineEmits<{
  (e: 'close'): void
  (e: 'submit'): void
}>()
</script>

<style scoped>
.assign-list {
  display: grid;
  gap: 8px;
}

.assign-item {
  display: flex;
  gap: 8px;
  align-items: center;
}
</style>
