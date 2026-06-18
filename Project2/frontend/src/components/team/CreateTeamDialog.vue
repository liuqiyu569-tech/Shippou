<template>
  <AppDialog
    :visible="visible"
    title="创建团队"
    title-id="create-team-title"
    wide
    tall
    :disable-close="submitting"
    @close="close"
    @update:visible="emit('update:visible', $event)"
  >
    <label class="field">
      <span class="field__label">团队名称</span>
      <input
        v-model="teamName"
        class="field__control"
        type="text"
        maxlength="100"
        placeholder="请输入团队名称"
        autocomplete="organization"
      />
    </label>

    <p class="hint hint--muted">
      你将以<strong>负责人</strong>身份加入；可在下方搜索并勾选初始成员（可选，可多选）。
    </p>

    <UserSearchPanel
      v-model:search-input="searchInput"
      :users="users"
      :picked="picked"
      :list-loading="listLoading"
      :list-error="listError"
      :has-more="hasMore"
      :disabled="submitting"
      picked-label="已选初始成员："
      :is-picked="isPicked"
      :is-row-disabled="isSelf"
      :row-tag="selfRowTag"
      @search-input="scheduleSearch"
      @user-check="onUserCheckSelf"
      @remove-picked="removePicked"
      @load-next="loadNextPage"
    />

    <template #footer>
      <button type="button" class="btn btn--ghost" :disabled="submitting" @click="close">取消</button>
      <button type="button" class="btn btn--primary" :disabled="submitting || !canSubmit" @click="submit">
        {{ submitting ? '创建中…' : '创建' }}
      </button>
    </template>
  </AppDialog>
</template>

<script setup lang="ts">
import axios from 'axios'
import { computed, ref, watch } from 'vue'
import AppDialog from '../common/AppDialog.vue'
import UserSearchPanel from './UserSearchPanel.vue'
import { createTeam } from '../../api/team'
import { getApiErrorMessage } from '../../api/http'
import { useUserSearch } from '../../composables/useUserSearch'
import { useAuthStore } from '../../stores/auth'
import { useNoticeStore } from '../../stores/notice'
import type { UserBrief } from '../../types/user'

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'created'): void
}>()

const authStore = useAuthStore()
authStore.restoreSession()
const noticeStore = useNoticeStore()

const teamName = ref('')
const submitting = ref(false)

const {
  searchInput,
  users,
  picked,
  listLoading,
  listError,
  hasMore,
  isPicked,
  onUserCheck,
  removePicked,
  scheduleSearch,
  loadNextPage,
  initOnOpen,
} = useUserSearch()

const selfId = computed(() => authStore.user?.id ?? null)
const canSubmit = computed(() => teamName.value.trim().length > 0)

function isSelf(userId: number) {
  return selfId.value != null && userId === selfId.value
}

function selfRowTag(userId: number) {
  return isSelf(userId) ? '将自动成为负责人' : undefined
}

function onUserCheckSelf(u: UserBrief, ev: Event) {
  onUserCheck(u, ev, { blocked: isSelf })
}

function close() {
  emit('update:visible', false)
}

function resetTeamForm() {
  teamName.value = ''
}

watch(
  () => props.visible,
  (open) => {
    if (open) {
      resetTeamForm()
      initOnOpen()
    }
  },
)

async function submit() {
  const name = teamName.value.trim()
  if (!name) {
    noticeStore.show('团队名称不能为空')
    return
  }

  submitting.value = true
  try {
    await createTeam({
      name,
      memberIds: picked.value.map((p) => p.id),
    })
    noticeStore.show('创建成功', 'success')
    emit('created')
    close()
  } catch (e) {
    const status = axios.isAxiosError(e) ? e.response?.status : undefined
    if (status !== 401 && status !== 403) {
      noticeStore.show(getApiErrorMessage(e))
    }
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 14px;
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

.hint {
  margin: 0 0 12px;
  color: #5c6a69;
  font-size: 0.9rem;
}

.hint--muted {
  color: #6a7a78;
  line-height: 1.5;
}
</style>
