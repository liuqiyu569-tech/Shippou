<template>
  <p v-if="loading" class="load-state">{{ loadingText }}</p>
  <p v-else-if="error" class="load-state load-state--error">
    {{ error }}
    <button v-if="showBack" type="button" class="load-state__back" @click="$emit('back')">
      {{ backLabel }}
    </button>
  </p>
  <slot v-else />
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    loading?: boolean
    error?: string
    loadingText?: string
    showBack?: boolean
    backLabel?: string
  }>(),
  {
    loading: false,
    error: '',
    loadingText: '加载中…',
    showBack: false,
    backLabel: '返回',
  },
)

defineEmits<{
  (e: 'back'): void
}>()
</script>

<style scoped>
.load-state {
  color: #666;
  margin: 10px 0;
  text-align: center;
}

.load-state--error {
  color: #c45656;
}

.load-state__back {
  display: block;
  margin: 12px auto 0;
  padding: 8px 16px;
  border-radius: 999px;
  border: none;
  cursor: pointer;
  font-weight: 700;
  color: #fffaf2;
  background: var(--color-primary);
}
</style>
