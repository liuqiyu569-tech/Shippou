<template>
  <div class="info-card">
    <div class="info-item">
      <label>团队ID：</label>
      <span>{{ teamId }}</span>
    </div>
    <div class="info-item">
      <label>创建时间：</label>
      <span>{{ formatDate(createdAt) }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { formatDate } from '../../utils/format'

defineProps<{
  teamId: number
  createdAt: string
}>()
</script>

<style scoped>
.info-card {
  padding: 16px;
  border: 1px solid #eee;
  border-radius: 8px;
  margin-bottom: 24px;
}

.info-item {
  margin: 8px 0;
}
</style>
