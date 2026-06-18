<template>
  <section class="member-panel">
    <div class="member-panel__toolbar">
      <h3>成员管理</h3>
      <div class="member-panel__toolbar-end">
        <div v-if="isOwner" class="member-panel__toolbar-actions">
          <button
            type="button"
            class="btn-add"
            :disabled="busy || locked"
            @click="openAddDialog"
          >
            添加成员
          </button>
          <button
            type="button"
            class="btn-add"
            :disabled="busy || locked"
            @click="openTransferDialog"
          >
            转让所有权
          </button>
          <button
            type="button"
            class="btn-add btn-add--danger"
            :disabled="busy || locked"
            @click="confirmDissolve"
          >
            解散团队
          </button>
        </div>
        <button
          v-if="canLeaveTeam"
          type="button"
          class="btn-leave"
          :disabled="busy || locked"
          @click="confirmLeave"
        >
          离开团队
        </button>
      </div>
    </div>

    <div class="table-wrap">
      <table class="member-table">
        <thead>
          <tr>
            <th>用户名</th>
            <th>角色</th>
            <th>加入时间</th>
            <th v-if="isOwner" class="col-actions">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="m in members" :key="m.userId">
            <td>{{ m.username }}</td>
            <td>{{ roleLabel(m.role) }}</td>
            <td>{{ formatJoinedAt(m.joinedAt) }}</td>
            <td v-if="isOwner" class="col-actions">
              <template v-if="canManageMember(m)">
                <button
                  type="button"
                  class="btn-row"
                  :disabled="busy || locked"
                  @click="confirmToggleRole(m)"
                >
                  {{ roleLabel(m.role) }} → {{ m.role === 'ADMIN' ? '成员' : '管理员' }}
                </button>
                <button
                  type="button"
                  class="btn-row btn-row--danger"
                  :disabled="busy || locked"
                  @click="confirmRemove(m)"
                >
                  移除
                </button>
              </template>
              <span v-else class="muted">—</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <AddMemberDialog
      v-model:visible="showAdd"
      :team-id="teamId"
      :member-user-ids="memberUserIds"
      @added="emit('refresh')"
      @refresh="emit('refresh')"
    />

    <TransferOwnershipDialog
      v-model:visible="showTransfer"
      :candidates="transferCandidates"
      :submitting="busy"
      @confirm="onTransferConfirm"
    />
  </section>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import axios from 'axios'
import { useRouter } from 'vue-router'
import {
  dissolveTeam,
  leaveTeam,
  removeTeamMember,
  transferOwnership,
  updateTeamMemberRole,
} from '../../api/team'
import { getApiErrorMessage } from '../../api/http'
import { useAuthStore } from '../../stores/auth'
import { useNoticeStore } from '../../stores/notice'
import type { MemberInfo, TeamRole } from '../../types/team'
import AddMemberDialog from './AddMemberDialog.vue'
import TransferOwnershipDialog from './TransferOwnershipDialog.vue'

const props = withDefaults(
  defineProps<{
    teamId: number
    myRole: TeamRole
    members: MemberInfo[]
    locked?: boolean
    onRefresh?: () => Promise<void>
  }>(),
  { locked: false },
)

const emit = defineEmits<{
  (e: 'refresh'): void
  (e: 'role-downgraded'): void
  (e: 'busy-change', value: boolean): void
}>()

const authStore = useAuthStore()
authStore.restoreSession()
const noticeStore = useNoticeStore()
const router = useRouter()

const showAdd = ref(false)
const showTransfer = ref(false)
const busy = ref(false)

watch(busy, (v) => emit('busy-change', v))

async function refreshAfterMemberChange() {
  if (props.onRefresh) {
    await props.onRefresh()
  } else {
    emit('refresh')
  }
}

const isOwner = computed(() => props.myRole === 'OWNER')
const locked = computed(() => props.locked ?? false)
const selfId = computed(() => authStore.user?.id)

const memberUserIds = computed(() => props.members.map((m) => m.userId))

const transferCandidates = computed(() =>
  props.members.filter((m) => m.userId !== selfId.value),
)

const canLeaveTeam = computed(() => props.myRole !== 'OWNER')

function roleLabel(role: TeamRole) {
  const map: Record<TeamRole, string> = {
    OWNER: '负责人',
    ADMIN: '管理员',
    MEMBER: '成员',
  }
  return map[role] ?? role
}

function formatJoinedAt(iso: string) {
  if (!iso) {
    return '—'
  }
  return iso.replace('T', ' ').substring(0, 16)
}

function openAddDialog() {
  if (locked.value) {
    return
  }
  showAdd.value = true
}

function openTransferDialog() {
  if (busy.value || locked.value || !isOwner.value) {
    return
  }
  if (selfId.value == null) {
    noticeStore.show('请重新登录')
    return
  }
  if (!transferCandidates.value.length) {
    noticeStore.show('请先添加其他成员')
    return
  }
  showTransfer.value = true
}

async function onTransferConfirm(newOwnerId: number) {
  if (busy.value || locked.value || !isOwner.value) {
    return
  }
  busy.value = true
  try {
    await transferOwnership(props.teamId, newOwnerId)
    if (props.onRefresh) {
      await props.onRefresh()
    } else {
      emit('refresh')
    }
    showTransfer.value = false
    noticeStore.show('转让成功，你现在是普通成员', 'success')
    emit('role-downgraded')
  } catch (e) {
    const status = axios.isAxiosError(e) ? e.response?.status : undefined
    if (status !== 401 && status !== 403) {
      noticeStore.show(getApiErrorMessage(e))
    }
  } finally {
    busy.value = false
  }
}

async function confirmDissolve() {
  if (busy.value || locked.value) {
    return
  }
  if (!window.confirm('确定解散该团队？此操作不可撤销，团队内的所有任务也会被删除。')) {
    return
  }
  busy.value = true
  try {
    await dissolveTeam(props.teamId)
    noticeStore.show('团队已解散', 'success')
    await router.push({ name: 'my-teams' })
  } catch (e) {
    const status = axios.isAxiosError(e) ? e.response?.status : undefined
    if (status !== 401 && status !== 403) {
      noticeStore.show(getApiErrorMessage(e))
    }
  } finally {
    busy.value = false
  }
}

async function confirmLeave() {
  if (busy.value || locked.value) {
    return
  }
  if (props.myRole === 'OWNER') {
    return
  }
  if (selfId.value == null) {
    noticeStore.show('请重新登录')
    return
  }
  if (
    !window.confirm(
      '确定离开该团队？离开后将无法访问团队任务，你在本团队的任务指派也会被取消。',
    )
  ) {
    return
  }
  busy.value = true
  try {
    await leaveTeam(props.teamId)
    noticeStore.show('已成功离开团队', 'success')
    await router.push({ name: 'my-teams' })
  } catch (e) {
    const status = axios.isAxiosError(e) ? e.response?.status : undefined
    if (status !== 401 && status !== 403) {
      noticeStore.show(getApiErrorMessage(e))
    }
  } finally {
    busy.value = false
  }
}

function canManageMember(m: MemberInfo) {
  if (!isOwner.value) {
    return false
  }
  if (m.role === 'OWNER') {
    return false
  }
  if (selfId.value != null && m.userId === selfId.value) {
    return false
  }
  return true
}

function nextWritableRole(m: MemberInfo): 'ADMIN' | 'MEMBER' | null {
  if (m.role === 'ADMIN') {
    return 'MEMBER'
  }
  if (m.role === 'MEMBER') {
    return 'ADMIN'
  }
  return null
}

async function confirmToggleRole(m: MemberInfo) {
  if (busy.value || locked.value) {
    return
  }
  const next = nextWritableRole(m)
  if (!next) {
    noticeStore.show('负责人角色不可在此修改或降级')
    return
  }
  const label = next === 'ADMIN' ? '管理员' : '成员'
  busy.value = true
  if (!window.confirm(`确定将「${m.username}」的角色改为 ${label}？`)) {
    busy.value = false
    return
  }

  try {
    await updateTeamMemberRole(props.teamId, m.userId, next)
    noticeStore.show('角色已更新', 'success')
    await refreshAfterMemberChange()
  } catch (e) {
    const status = axios.isAxiosError(e) ? e.response?.status : undefined
    if (status !== 401 && status !== 403) {
      noticeStore.show(getApiErrorMessage(e))
    }
    await refreshAfterMemberChange()
  } finally {
    busy.value = false
  }
}

async function confirmRemove(m: MemberInfo) {
  if (busy.value || locked.value) {
    return
  }
  busy.value = true
  if (!window.confirm(`确定将「${m.username}」移出团队？`)) {
    busy.value = false
    return
  }

  try {
    await removeTeamMember(props.teamId, m.userId)
    noticeStore.show('已移除成员', 'success')
    await refreshAfterMemberChange()
  } catch (e) {
    const status = axios.isAxiosError(e) ? e.response?.status : undefined
    if (status !== 401 && status !== 403) {
      noticeStore.show(getApiErrorMessage(e))
    }
    await refreshAfterMemberChange()
  } finally {
    busy.value = false
  }
}
</script>

<style scoped>
.member-panel {
  padding: 4px 0 8px;
}

.member-panel__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.member-panel__toolbar h3 {
  margin: 0;
  font-size: 1.1rem;
  color: #203032;
}

.member-panel__toolbar-end {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.member-panel__toolbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.btn-add {
  padding: 8px 16px;
  border-radius: 999px;
  border: none;
  cursor: pointer;
  font-weight: 700;
  color: #fffaf2;
  background: var(--color-primary);
}

.btn-add:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-add--danger {
  background: var(--color-danger);
}

.btn-leave {
  padding: 8px 16px;
  border-radius: 999px;
  border: 1px solid rgba(104, 132, 118, 0.4);
  cursor: pointer;
  font-weight: 700;
  color: var(--color-primary);
  background: rgba(255, 255, 255, 0.9);
}

.btn-leave:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.table-wrap {
  overflow-x: auto;
  border: 1px solid rgba(104, 132, 118, 0.2);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.65);
}

.member-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.92rem;
}

.member-table th,
.member-table td {
  padding: 10px 12px;
  text-align: left;
  border-bottom: 1px solid rgba(104, 132, 118, 0.12);
}

.member-table th {
  background: rgba(232, 243, 255, 0.9);
  color: #000;
  font-weight: 700;
}

.col-actions {
  white-space: nowrap;
  width: 1%;
}

.btn-row {
  margin-right: 8px;
  padding: 4px 10px;
  border-radius: 8px;
  border: 1px solid rgba(104, 132, 118, 0.35);
  background: rgba(255, 255, 255, 0.95);
  cursor: pointer;
  font-size: 0.85rem;
}

.btn-row:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.btn-row--danger {
  border-color: rgba(245, 108, 108, 0.45);
  color: #c45656;
}

.muted {
  color: #9aa8a6;
}
</style>
