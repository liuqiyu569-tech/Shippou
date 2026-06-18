<template>
  <nav v-if="total > 0" class="list-pager" :class="`list-pager--${variant}`" aria-label="分页">
    <span class="list-pager__info">
      共 {{ total }} 条，第 {{ page }} / {{ totalPages }} 页
    </span>
    <div class="list-pager__actions">
      <button type="button" :disabled="page <= 1 || loading" @click="$emit('change', page - 1)">
        上一页
      </button>
      <button
        type="button"
        :disabled="page >= totalPages || loading"
        @click="$emit('change', page + 1)"
      >
        下一页
      </button>
    </div>
  </nav>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    page: number
    totalPages: number
    total: number
    loading?: boolean
    variant?: 'default' | 'teams'
  }>(),
  {
    loading: false,
    variant: 'default',
  },
)

defineEmits<{
  (e: 'change', page: number): void
}>()
</script>

<style scoped>
.list-pager {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.list-pager--default {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #eee;
}

.list-pager--teams {
  margin-top: 28px;
}

.list-pager__info {
  font-size: 0.9rem;
  color: #666;
}

.list-pager--teams .list-pager__info {
  color: #5c6a69;
}

.list-pager__actions {
  display: flex;
  gap: 8px;
}

.list-pager--teams .list-pager__actions {
  gap: 10px;
}

.list-pager__actions button {
  padding: 6px 14px;
  border: 1px solid var(--color-primary-border);
  border-radius: 4px;
  background: #fff;
  color: var(--color-primary);
  cursor: pointer;
}

.list-pager--teams .list-pager__actions button {
  padding: 8px 16px;
  border-radius: 999px;
  border: 1px solid var(--color-primary-border);
  background: #fff;
  font-weight: 600;
  color: var(--color-primary);
}

.list-pager__actions button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
