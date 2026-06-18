<template>
  <main class="auth-page">
    <section class="auth-card auth-card--form">
      <p class="eyebrow">WELCOME BACK</p>
      <h1>欢迎回来</h1>
      <p class="subtitle">请输入用户名和密码。登录成功后会自动恢复到你想访问的受保护页面。</p>

      <form class="auth-form" @submit.prevent="handleSubmit">
        <label class="field">
          <span class="field__label">用户名</span>
          <input
            v-model.trim="form.username"
            class="field__control"
            type="text"
            name="username"
            autocomplete="username"
            placeholder="例如 student_01"
          />
        </label>

        <label class="field">
          <span class="field__label">密码</span>
          <input
            v-model="form.password"
            class="field__control"
            type="password"
            name="password"
            autocomplete="current-password"
            placeholder="请输入登录密码"
          />
        </label>

        <p v-if="errorMessage" class="form-message form-message--error">{{ errorMessage }}</p>

        <button class="primary-link action-button submit-button" type="submit" :disabled="isSubmitting">
          {{ isSubmitting ? '登录中...' : '登录' }}
        </button>
      </form>

      <div class="card-actions card-actions--compact">
        <RouterLink class="ghost-link ghost-link--primary" to="/register">还没有账号？去注册</RouterLink>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { login } from '../api/auth'
import { getApiErrorMessage } from '../api/http'
import { useAuthStore } from '../stores/auth'
import { useNoticeStore } from '../stores/notice'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const noticeStore = useNoticeStore()

const form = reactive({
  username: '',
  password: '',
})

const isSubmitting = ref(false)
const errorMessage = ref('')

function validateForm() {
  if (!form.username) {
    return '用户名不能为空'
  }

  if (!form.password) {
    return '密码不能为空'
  }

  return ''
}

/**
 * 提交登录表单，并在成功后写入 Pinia 与 localStorage。
 */
async function handleSubmit() {
  errorMessage.value = validateForm()

  if (errorMessage.value) {
    return
  }

  isSubmitting.value = true

  try {
    const payload = await login(form)

    authStore.setSession(payload.data.token, payload.data.user)
    noticeStore.show('登录成功', 'success')

    const redirectTarget = typeof route.query.redirect === 'string' ? route.query.redirect : '/tasks'
    await router.push(redirectTarget)
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error)
    noticeStore.show(errorMessage.value)
  } finally {
    isSubmitting.value = false
  }
}
</script>
