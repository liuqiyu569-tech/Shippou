<template>
  <div class="user-chip">
    <span class="user-chip__avatar">{{ initial }}</span>
    <span class="user-chip__name">{{ username }}</span>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  username: string
  initial: string
}>()
</script>

<style scoped>
.user-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.user-chip__avatar {
  width: 32px;
  height: 32px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 700;
  background: linear-gradient(135deg, #2f6b5b, #4d8a79);
}

.user-chip__name {
  font-weight: 600;
  color: #1f2d2f;
}
</style>
