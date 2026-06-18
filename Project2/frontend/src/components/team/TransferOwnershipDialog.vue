<template>
  <AppDialog
    :visible="visible"
    title="转让团队所有权"
    title-id="transfer-owner-title"
    :disable-close="submitting"
    @close="close"
    @update:visible="emit('update:visible', $event)"
  >
    <p v-if="!candidates.length" class="hint">
      团队中暂无其他成员，请先添加成员或解散团队。
    </p>
    <template v-else>
      <label class="field">
        <span class="field__label">选择新负责人</span>
        <select
          v-model="selectedUserId"
          class="field__control field__select"
          :disabled="submitting"
        >
          <option value="" disabled>请选择新负责人</option>
          <option v-for="m in candidates" :key="m.userId" :value="m.userId">
            {{ m.username }}（{{ roleLabel(m.role) }}）
          </option>
        </select>
      </label>
    </template>

    <template #footer>
      <button type="button" class="btn btn--ghost" :disabled="submitting" @click="close">取消</button>
      <button
        type="button"
        class="btn btn--primary"
        :disabled="submitting || !canSubmit"
        @click="onConfirm"
      >
        {{ submitting ? '转让中…' : '确认转让' }}
      </button>
    </template>
  </AppDialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import AppDialog from '../common/AppDialog.vue'
import { roleLabel } from '../../utils/team'
import type { MemberInfo } from '../../types/team'

const props = defineProps<{
  visible: boolean
  candidates: MemberInfo[]
  submitting: boolean
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'confirm', newOwnerId: number): void
}>()

const selectedUserId = ref<number | ''>('')

const canSubmit = computed(
  () =>
    props.candidates.length > 0 &&
    selectedUserId.value !== '' &&
    Number.isFinite(Number(selectedUserId.value)),
)

function resetState() {
  selectedUserId.value = ''
}

function close() {
  emit('update:visible', false)
}

function onConfirm() {
  if (!canSubmit.value || selectedUserId.value === '') {
    return
  }
  const id = Number(selectedUserId.value)
  const target = props.candidates.find((m) => m.userId === id)
  const name = target?.username ?? '该成员'
  if (
    !window.confirm(
      `转让后你将变为普通成员，「${name}」将成为团队负责人。确定继续？`,
    )
  ) {
    return
  }
  emit('confirm', id)
}

watch(
  () => props.visible,
  (open) => {
    if (open) {
      resetState()
    }
  },
)
</script>

<style scoped>
.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field__label {
  font-weight: 700;
  color: #29473d;
  font-size: 0.9rem;
}

.field__control {
  width: 100%;
  min-height: 44px;
  padding: 0 14px;
  border: 1px solid rgba(104, 132, 118, 0.32);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.92);
  box-sizing: border-box;
}

.field__select {
  cursor: pointer;
}

.hint {
  margin: 0;
  color: #5c6a69;
  font-size: 0.92rem;
  line-height: 1.5;
}
</style>
