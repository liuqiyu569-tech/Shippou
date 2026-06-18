<template>
  <AppDialog
    :visible="visible"
    title="添加成员"
    title-id="add-member-title"
    wide
    tall
    :disable-close="submitting"
    @close="close"
    @update:visible="emit('update:visible', $event)"
  >
    <UserSearchPanel
      v-model:search-input="searchInput"
      :users="users"
      :picked="picked"
      :list-loading="listLoading"
      :list-error="listError"
      :has-more="hasMore"
      :disabled="submitting"
      :is-picked="isPicked"
      :is-row-disabled="isAlreadyMember"
      :row-tag="memberRowTag"
      @search-input="scheduleSearch"
      @user-check="onUserCheckMember"
      @remove-picked="removePicked"
      @load-next="loadNextPage"
    />

    <template #footer>
      <button type="button" class="btn btn--ghost" :disabled="submitting" @click="close">取消</button>
      <button type="button" class="btn btn--primary" :disabled="submitting || !canSubmit" @click="submit">
        {{ submitting ? '添加中…' : `添加${picked.length ? ` (${picked.length})` : ''}` }}
      </button>
    </template>
  </AppDialog>
</template>

<script setup lang="ts">
import axios from 'axios'
import { computed, ref, watch } from 'vue'
import AppDialog from '../common/AppDialog.vue'
import UserSearchPanel from './UserSearchPanel.vue'
import { addTeamMember } from '../../api/team'
import { getApiErrorMessage } from '../../api/http'
import { useUserSearch } from '../../composables/useUserSearch'
import { useNoticeStore } from '../../stores/notice'
import type { UserBrief } from '../../types/user'

const props = defineProps<{
  visible: boolean
  teamId: number
  memberUserIds: number[]
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'added'): void
  (e: 'refresh'): void
}>()

const noticeStore = useNoticeStore()
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

const canSubmit = computed(() => picked.value.some((p) => !isAlreadyMember(p.id)))

function isAlreadyMember(userId: number) {
  return props.memberUserIds.includes(userId)
}

function memberRowTag(userId: number) {
  return isAlreadyMember(userId) ? '已在团队' : undefined
}

function onUserCheckMember(u: UserBrief, ev: Event) {
  onUserCheck(u, ev, { blocked: isAlreadyMember })
}

function close() {
  emit('update:visible', false)
}

watch(
  () => props.visible,
  (open) => {
    if (open) {
      initOnOpen()
    }
  },
)

async function submit() {
  const validIds = picked.value.filter((p) => !isAlreadyMember(p.id)).map((p) => p.id)
  if (!validIds.length) {
    noticeStore.show('请先选择至少一名未在团队中的用户')
    return
  }

  submitting.value = true
  let successCount = 0
  let failCount = 0

  for (const userId of validIds) {
    try {
      await addTeamMember(props.teamId, userId)
      successCount++
    } catch (e) {
      failCount++
      const status = axios.isAxiosError(e) ? e.response?.status : undefined
      if (status !== 401 && status !== 403) {
        noticeStore.show(getApiErrorMessage(e))
      }
    }
  }

  if (successCount > 0) {
    noticeStore.show(
      `成功添加 ${successCount} 名成员` + (failCount > 0 ? `，${failCount} 名失败` : ''),
      'success',
    )
  }

  emit('added')
  if (failCount > 0) {
    emit('refresh')
  } else {
    close()
  }

  submitting.value = false
}
</script>
