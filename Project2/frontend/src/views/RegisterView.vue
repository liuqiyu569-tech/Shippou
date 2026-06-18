<template>
  <main class="auth-page">
    <section class="auth-card auth-card--form">
      <p class="eyebrow">Create Account</p>
      <h1>注册新账号</h1>
      <p class="subtitle">用户名仅支持字母、数字和下划线。注册成功后会自动登录并进入任务页。</p>

      <form class="auth-form" @submit.prevent="handleSubmit">
        <label class="field">
          <span class="field__label">用户名</span>
          <input
            v-model.trim="form.username"
            class="field__control"
            type="text"
            name="username"
            autocomplete="username"
            placeholder="4 到 20 位，例如 student_01"
          />
        </label>

        <label class="field">
          <span class="field__label">密码</span>
          <input
            v-model="form.password"
            class="field__control"
            type="password"
            name="password"
            autocomplete="new-password"
            placeholder="至少 6 位，且同时包含字母和数字"
          />
        </label>

        <p v-if="errorMessage" class="form-message form-message--error">{{ errorMessage }}</p>

        <button class="primary-link action-button submit-button" type="submit" :disabled="isSubmitting">
          {{ isSubmitting ? '注册中...' : '注册并登录' }}
        </button>
      </form>

      <div class="card-actions card-actions--compact">
        <RouterLink class="ghost-link ghost-link--primary" to="/login">已有账号？返回登录</RouterLink>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { login, register } from '../api/auth'
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

const usernamePattern = /^[A-Za-z0-9_]{4,20}$/
const passwordPattern = /^(?=.*[A-Za-z])(?=.*\d).{6,}$/

function validateForm() {
  if (!form.username) {
    return '用户名不能为空'
  }

  if (!usernamePattern.test(form.username)) {
    return '用户名只能包含字母、数字和下划线，长度为 4 到 20 位'
  }

  if (!form.password) {
    return '密码不能为空'
  }

  if (!passwordPattern.test(form.password)) {
    return '密码不少于 6 位，且必须同时包含字母和数字'
  }

  return ''
}

/**
 * 注册成功后立即调用登录接口，确保前端和当前后端契约保持一致。
 */
async function handleSubmit() {
  errorMessage.value = validateForm()

  if (errorMessage.value) {
    return
  }

  isSubmitting.value = true

  try {
    await register(form)
    const loginPayload = await login(form)

    authStore.setSession(loginPayload.data.token, loginPayload.data.user)
    noticeStore.show('注册成功，已自动登录', 'success')

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
