<template>
  <header class="app-page-header">
    <div class="app-page-header__start">
      <slot name="title">
        <component :is="headingTag" v-if="title" class="app-page-header__title">{{ title }}</component>
      </slot>
    </div>
    <AppTopNav v-if="showNav" />
  </header>
</template>

<script setup lang="ts">
import AppTopNav from './AppTopNav.vue'

withDefaults(
  defineProps<{
    title?: string
    headingTag?: 'h1' | 'h2'
    showNav?: boolean
  }>(),
  {
    headingTag: 'h2',
    showNav: true,
  },
)
</script>

<style scoped>
.app-page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.app-page-header__start {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.app-page-header__title {
  margin: 0;
  font-size: clamp(1.5rem, 3vw, 2rem);
  color: #203032;
}

@media (max-width: 640px) {
  .app-page-header {
    align-items: stretch;
  }
}
</style>
