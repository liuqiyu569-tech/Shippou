<template>
  <main class="auth-page">
    <section class="auth-card">
      <p class="eyebrow">Protected Route</p>
      <h1>任务页占位</h1>
      <p class="subtitle">
        这个页面用于验证登录守卫是否生效。后续成员 E 会在这里承接正式的任务管理界面。
      </p>

      <p v-if="authStore.user" class="session-tag">
        当前登录用户：<strong>{{ authStore.user.username }}</strong>
      </p>

      <div class="card-actions">
        <button class="ghost-link action-button" type="button" @click="handleLogout">
          退出登录
        </button>
        <RouterLink class="ghost-link" to="/login">返回登录页</RouterLink>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { RouterLink, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useNoticeStore } from '../stores/notice'

const router = useRouter()
const authStore = useAuthStore()
const noticeStore = useNoticeStore()

async function handleLogout() {
  authStore.clearSession()
  noticeStore.show('已退出登录', 'success')
  await router.push('/login')
}
</script>
