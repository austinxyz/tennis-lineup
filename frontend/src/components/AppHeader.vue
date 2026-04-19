<template>
  <header class="lg:hidden fixed top-0 left-0 right-0 z-50 bg-white border-b border-gray-200 h-14 flex items-center px-3 gap-2">
    <button
      type="button"
      data-testid="hamburger"
      :aria-label="sidebarOpen ? '关闭导航' : '打开导航'"
      :aria-expanded="sidebarOpen"
      class="p-2 rounded-lg hover:bg-gray-100 flex-shrink-0"
      @click="toggleSidebar"
    >
      <svg class="w-6 h-6" aria-hidden="true" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16"/>
      </svg>
    </button>

    <button
      v-if="backTo"
      type="button"
      data-testid="back-btn"
      :aria-label="backLabel"
      class="text-blue-600 text-sm flex-shrink-0 px-1"
      @click="router.push(backTo)"
    >
      <span aria-hidden="true">←</span> {{ backLabel }}
    </button>

    <span class="flex-1 truncate font-semibold text-gray-900">{{ title }}</span>

    <slot name="actions" />
  </header>
</template>

<script setup>
import { inject, ref } from 'vue'
import { useRouter } from 'vue-router'

defineProps({
  title: {
    type: String,
    required: true,
  },
  backTo: {
    type: String,
    default: null,
  },
  backLabel: {
    type: String,
    default: '返回',
  },
})

// Fallback must be a real ref so the .value assignment in toggleSidebar actually works
// even when AppHeader is used without a MainLayout parent providing sidebarOpen.
const sidebarOpen = inject('sidebarOpen', ref(false))
const router = useRouter()

function toggleSidebar() {
  sidebarOpen.value = !sidebarOpen.value
}
</script>
