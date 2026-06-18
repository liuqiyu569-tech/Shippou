<template>
  <Teleport to="body">
    <div
      v-if="visible"
      class="dialog-mask"
      role="presentation"
      @click.self="onMaskClick"
    >
      <div
        class="dialog"
        :class="{ 'dialog--wide': wide, 'dialog--tall': tall }"
        role="dialog"
        aria-modal="true"
        :aria-labelledby="titleId"
      >
        <header class="dialog__header">
          <h2 :id="titleId">{{ title }}</h2>
          <button
            type="button"
            class="dialog__close"
            aria-label="关闭"
            :disabled="disableClose"
            @click="close"
          >
            ×
          </button>
        </header>
        <div class="dialog__body">
          <slot />
        </div>
        <footer v-if="$slots.footer" class="dialog__footer">
          <slot name="footer" />
        </footer>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, watch } from 'vue'

const props = withDefaults(
  defineProps<{
    visible: boolean
    title: string
    titleId?: string
    disableClose?: boolean
    closeOnMask?: boolean
    wide?: boolean
    tall?: boolean
    closeOnEscape?: boolean
  }>(),
  {
    disableClose: false,
    closeOnMask: true,
    wide: false,
    tall: false,
    closeOnEscape: true,
  },
)

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'update:visible', value: boolean): void
}>()

const titleId = computed(() => props.titleId ?? 'app-dialog-title')

let bodyOverflowBeforeLock = ''

function close() {
  if (props.disableClose) {
    return
  }
  emit('update:visible', false)
  emit('close')
}

function onMaskClick() {
  if (props.closeOnMask) {
    close()
  }
}

function onDocKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape' && props.visible && props.closeOnEscape) {
    close()
  }
}

watch(
  () => props.visible,
  (open) => {
    if (open) {
      bodyOverflowBeforeLock = document.body.style.overflow
      document.body.style.overflow = 'hidden'
      window.addEventListener('keydown', onDocKeydown)
    } else {
      document.body.style.overflow = bodyOverflowBeforeLock
      window.removeEventListener('keydown', onDocKeydown)
    }
  },
)

onBeforeUnmount(() => {
  document.body.style.overflow = bodyOverflowBeforeLock
  window.removeEventListener('keydown', onDocKeydown)
})
</script>

<style scoped>
.dialog-mask {
  position: fixed;
  inset: 0;
  z-index: 1100;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 18px;
}

.dialog {
  width: min(480px, 100%);
  max-height: min(82vh, 720px);
  display: flex;
  flex-direction: column;
  border-radius: 18px;
  background: rgba(255, 251, 245, 0.98);
  border: 1px solid rgba(112, 130, 114, 0.22);
  box-shadow: 0 24px 60px rgba(84, 76, 56, 0.2);
}

.dialog--wide {
  width: min(520px, 100%);
}

.dialog--tall {
  max-height: min(92vh, 720px);
}

.dialog__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 18px 0;
  flex-shrink: 0;
}

.dialog__header h2 {
  margin: 0;
  font-size: 1.2rem;
  color: #203032;
}

.dialog__close {
  border: none;
  background: transparent;
  font-size: 1.6rem;
  line-height: 1;
  cursor: pointer;
  color: #5c6a69;
}

.dialog__close:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.dialog__body {
  padding: 16px 18px;
  overflow: auto;
  flex: 1;
  min-height: 0;
}

.dialog__footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 12px 18px 18px;
  flex-shrink: 0;
}

.dialog__footer :deep(.btn) {
  min-height: 40px;
  padding: 0 18px;
  border-radius: 999px;
  font-weight: 700;
  border: none;
  cursor: pointer;
}

.dialog__footer :deep(.btn:disabled) {
  opacity: 0.6;
  cursor: not-allowed;
}

.dialog__footer :deep(.btn--ghost) {
  background: var(--color-primary-soft);
  color: var(--color-primary);
}

.dialog__footer :deep(.btn--primary) {
  background: var(--color-primary);
  color: #fffaf2;
}
</style>
