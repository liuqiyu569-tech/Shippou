<template>
  <div class="user-search-panel">
    <div v-if="picked.length" class="picked">
      <span class="picked__label">{{ pickedLabel }}</span>
      <ul class="picked__list">
        <li v-for="p in picked" :key="p.id" class="picked__item">
          {{ p.username }}
          <button type="button" class="picked__remove" :disabled="disabled" @click="$emit('remove-picked', p.id)">
            移除
          </button>
        </li>
      </ul>
    </div>

    <label class="field">
      <span class="field__label">搜索用户并勾选</span>
      <input
        v-model="searchInput"
        class="field__control"
        type="search"
        placeholder="输入用户名关键字"
        autocomplete="off"
        @input="$emit('search-input')"
      />
    </label>

    <p v-if="listLoading && !users.length" class="hint">加载中…</p>
    <p v-else-if="listError" class="hint hint--error">{{ listError }}</p>
    <ul v-else-if="!users.length" class="user-list user-list--empty">
      <li>无匹配用户，可尝试其他关键字。</li>
    </ul>
    <ul v-else class="user-list">
      <li v-for="u in users" :key="u.id">
        <label class="user-row" :class="{ 'user-row--disabled': isRowDisabled(u.id) }">
          <input
            type="checkbox"
            :checked="isPicked(u.id)"
            :disabled="isRowDisabled(u.id) || disabled"
            @change="$emit('user-check', u, $event)"
          />
          <span class="user-row__name">{{ u.username }}</span>
          <span v-if="rowTag(u.id)" class="user-row__tag">{{ rowTag(u.id) }}</span>
        </label>
      </li>
    </ul>

    <div v-if="users.length && hasMore" class="user-search-panel__more">
      <button type="button" :disabled="listLoading || disabled" @click="$emit('load-next')">下一页</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { PickedUser } from '../../composables/useUserSearch'
import type { UserBrief } from '../../types/user'

const searchInput = defineModel<string>('searchInput', { required: true })

withDefaults(
  defineProps<{
    users: UserBrief[]
    picked: PickedUser[]
    listLoading: boolean
    listError: string
    hasMore: boolean
    disabled?: boolean
    pickedLabel?: string
    isPicked: (userId: number) => boolean
    isRowDisabled: (userId: number) => boolean
    rowTag?: (userId: number) => string | undefined
  }>(),
  {
    disabled: false,
    pickedLabel: '已选用户：',
    rowTag: () => undefined,
  },
)

defineEmits<{
  (e: 'search-input'): void
  (e: 'user-check', user: UserBrief, event: Event): void
  (e: 'remove-picked', id: number): void
  (e: 'load-next'): void
}>()
</script>

<style scoped>
.user-search-panel {
  display: block;
}

.picked {
  margin-bottom: 14px;
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(199, 225, 213, 0.28);
  border: 1px solid rgba(104, 132, 118, 0.2);
}

.picked__label {
  font-size: 0.85rem;
  font-weight: 700;
  color: #29473d;
}

.picked__list {
  list-style: none;
  margin: 8px 0 0;
  padding: 0;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.picked__item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  font-size: 0.88rem;
}

.picked__remove {
  border: none;
  background: transparent;
  color: #c45656;
  cursor: pointer;
  font-size: 0.8rem;
  font-weight: 700;
  padding: 0;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 14px;
}

.field__label {
  font-weight: 700;
  color: #29473d;
  font-size: 0.9rem;
}

.field__control {
  width: 100%;
  min-height: 44px;
  padding: 0 14px;
  border: 1px solid rgba(104, 132, 118, 0.32);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.92);
  box-sizing: border-box;
}

.hint {
  margin: 0 0 12px;
  color: #5c6a69;
  font-size: 0.9rem;
}

.hint--error {
  color: #8a2d20;
}

.user-list {
  list-style: none;
  margin: 0;
  padding: 0;
  border: 1px solid rgba(104, 132, 118, 0.2);
  border-radius: 12px;
  overflow: hidden;
}

.user-list--empty li {
  padding: 16px;
  color: #6a7a78;
}

.user-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-bottom: 1px solid rgba(104, 132, 118, 0.12);
  cursor: pointer;
}

.user-row--disabled {
  cursor: default;
  opacity: 0.85;
}

.user-list li:last-child .user-row {
  border-bottom: none;
}

.user-row__name {
  flex: 1;
  font-weight: 600;
  color: #243234;
}

.user-row__tag {
  font-size: 0.75rem;
  color: #6a7a78;
}

.user-search-panel__more {
  margin-top: 12px;
  display: flex;
  justify-content: center;
}

.user-search-panel__more button {
  padding: 8px 18px;
  border-radius: 999px;
  border: 1px solid rgba(104, 132, 118, 0.35);
  background: rgba(255, 255, 255, 0.9);
  cursor: pointer;
  font-weight: 600;
}
</style>
