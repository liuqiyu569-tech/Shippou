<template>
  <nav class="top-nav" aria-label="主导航">
    <RouterLink class="top-nav__item" :to="{ name: 'tasks' }">首页</RouterLink>
    <RouterLink class="top-nav__item" :to="{ name: 'my-teams' }">我的团队</RouterLink>
    <button type="button" class="top-nav__item top-nav__item--button" @click="logout">
      退出登录
    </button>
    <div class="top-nav__profile" aria-label="当前用户">
      <span class="top-nav__avatar" aria-hidden="true">{{ userInitial }}</span>
      <span class="top-nav__name">{{ username }}</span>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'
import { useAuthHeader } from '../../composables/useAuthHeader'

const { logout, username, userInitial } = useAuthHeader()
</script>

<style scoped>
.top-nav {
  display: inline-flex;
  align-items: stretch;
  border: 1px solid var(--color-primary-border);
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}

.top-nav__item {
  min-height: 38px;
  padding: 0 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 0;
  border-right: 1px solid var(--color-primary-border);
  background: #fff;
  color: var(--color-primary);
  font-weight: 700;
  line-height: 1;
  text-decoration: none;
  cursor: pointer;
}

.top-nav__item:last-child {
  border-right: 0;
}

.top-nav__item.router-link-active,
.top-nav__item:hover {
  background: var(--color-primary);
  color: #fff;
}

.top-nav__item--button {
  font: inherit;
  font-weight: 800;
}

.top-nav__profile {
  min-height: 38px;
  padding: 0 16px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: #fff;
  color: var(--color-primary);
}

.top-nav__avatar {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
}

.top-nav__name {
  font-weight: 700;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 520px) {
  .top-nav {
    width: 100%;
  }

  .top-nav__item {
    flex: 1;
    padding: 0 10px;
  }

  .top-nav__profile {
    flex: 1;
    justify-content: center;
    padding: 0 10px;
  }
}
</style>
