import { ref } from 'vue'
import { defineStore } from 'pinia'

type NoticeKind = 'error' | 'success'

export const useNoticeStore = defineStore('notice', () => {
  const message = ref('')
  const kind = ref<NoticeKind>('error')
  const visible = ref(false)
  let timer: number | undefined

  function show(nextMessage: string, nextKind: NoticeKind = 'error') {
    message.value = nextMessage
    kind.value = nextKind
    visible.value = true

    if (timer) {
      window.clearTimeout(timer)
    }

    timer = window.setTimeout(() => {
      visible.value = false
    }, 2600)
  }

  function hide() {
    visible.value = false
  }

  return {
    message,
    kind,
    visible,
    show,
    hide,
  }
})
