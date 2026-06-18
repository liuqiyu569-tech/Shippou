<template>
  <div class="my-teams">
    <AppPageHeader title="我的团队" heading-tag="h1" />

    <div class="my-teams__toolbar">
      <button
        type="button"
        class="my-teams__action my-teams__action--primary"
        :disabled="loading"
        @click="showCreate = true"
      >
        <span class="my-teams__action-icon">+</span>
        创建团队
      </button>
    </div>

    <LoadState :loading="loading" :error="loadError">
      <p v-if="!teams.length" class="my-teams__state">
        暂无团队。可点击「创建团队」新建，或等待其他负责人将你加入团队。
      </p>

      <ul v-else class="my-teams__grid">
        <li v-for="team in teams" :key="team.id">
          <TeamCard
            :name="team.name"
            :role="team.role"
            :member-count="team.memberCount"
            @click="goTeam(team.id)"
          />
        </li>
      </ul>

      <ListPager
        v-if="pageResult"
        variant="teams"
        :page="page"
        :total-pages="totalPages"
        :total="pageResult.total"
        :loading="loading"
        @change="changePage"
      />
    </LoadState>

    <CreateTeamDialog v-model:visible="showCreate" @created="onTeamCreated" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getMyTeams } from '../api/team'
import { getApiErrorMessage } from '../api/http'
import AppPageHeader from '../components/common/AppPageHeader.vue'
import ListPager from '../components/common/ListPager.vue'
import LoadState from '../components/common/LoadState.vue'
import CreateTeamDialog from '../components/team/CreateTeamDialog.vue'
import TeamCard from '../components/team/TeamCard.vue'
import type { PageResult, TeamListItem } from '../types/team'

const router = useRouter()

const PAGE_SIZE = 9

const loading = ref(false)
const loadError = ref('')
const teams = ref<TeamListItem[]>([])
const page = ref(1)
const pageResult = ref<PageResult<TeamListItem> | null>(null)
const showCreate = ref(false)

const totalPages = computed(() => {
  if (!pageResult.value) {
    return 1
  }
  return Math.max(1, Math.ceil(pageResult.value.total / pageResult.value.pageSize))
})

async function fetchTeams(targetPage = page.value) {
  loading.value = true
  loadError.value = ''
  try {
    const res = await getMyTeams(targetPage, PAGE_SIZE)
    const { items, total, page: p } = res.data
    if (items.length === 0 && total > 0 && targetPage > 1) {
      const resFirst = await getMyTeams(1, PAGE_SIZE)
      pageResult.value = resFirst.data
      teams.value = resFirst.data.items
      page.value = resFirst.data.page
    } else {
      pageResult.value = res.data
      teams.value = items
      page.value = p
    }
  } catch (e) {
    loadError.value = getApiErrorMessage(e)
  } finally {
    loading.value = false
  }
}

function changePage(next: number) {
  if (next < 1) {
    return
  }
  void fetchTeams(next)
}

function goTeam(id: number) {
  void router.push({ name: 'team-space', params: { teamId: String(id) } })
}

function onTeamCreated() {
  void fetchTeams(1)
}

onMounted(() => {
  void fetchTeams(1)
})
</script>

<style scoped>
.my-teams {
  max-width: 960px;
  margin: 0 auto;
  padding: 24px 18px 48px;
}

.my-teams__action {
  min-height: 38px;
  padding: 0 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border-radius: 999px;
  text-decoration: none;
  cursor: pointer;
  font-weight: 700;
  transition: all 0.2s ease;
}

.my-teams__action-icon {
  font-size: 18px;
  line-height: 1;
  display: inline-flex;
  align-items: center;
}

.my-teams__action:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.my-teams__action--primary {
  border: 1px solid var(--color-primary-border);
  color: #fffaf2;
  background: var(--color-primary);
}

.my-teams__toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 18px;
}

.my-teams__state {
  margin: 32px 0;
  text-align: center;
  color: #5c6a69;
}

.my-teams__grid {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
}
</style>
