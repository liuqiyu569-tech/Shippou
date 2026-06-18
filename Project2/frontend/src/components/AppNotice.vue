<template>
  <Transition name="notice-fade">
    <aside v-if="noticeStore.visible" class="notice" :class="`notice--${noticeStore.kind}`">
      <p>{{ noticeStore.message }}</p>
      <button class="notice__close" type="button" @click="noticeStore.hide">知道了</button>
    </aside>
  </Transition>
</template>

<script setup lang="ts">
import { useNoticeStore } from '../stores/notice'

const noticeStore = useNoticeStore()
</script>
