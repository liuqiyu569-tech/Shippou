<template>
  <AppNotice />
  <RouterView />
</template>

<script setup lang="ts">
import AppNotice from './components/AppNotice.vue'
import { RouterView } from 'vue-router'
</script>
