<template>
  <div class="flex min-h-screen bg-gray-100 overflow-x-hidden">

    <!-- Mobile overlay (click anywhere to close the drawer) -->
    <div v-if="sidebarOpen" @click="sidebarOpen = false" class="lg:hidden fixed inset-0 bg-black/40 z-40"/>

    <!-- Nav Sidebar: drawer on mobile, static on desktop -->
    <aside :class="[
      'fixed top-0 bottom-0 z-50 transition-transform duration-300 lg:static lg:translate-x-0 lg:z-auto',
      sidebarOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'
    ]">
      <NavSidebar @navigate="sidebarOpen = false"/>
    </aside>

    <!-- Main content: pt-14 accounts for the fixed-position AppHeader on mobile -->
    <div class="flex-1 flex flex-col min-w-0 pt-14 lg:pt-0">
      <router-view/>
    </div>
  </div>
</template>

<script setup>
import { ref, provide, onMounted, onUnmounted } from 'vue'
import NavSidebar from '../components/NavSidebar.vue'

const sidebarOpen = ref(false)
provide('sidebarOpen', sidebarOpen)

// Escape key closes the drawer (WCAG 2.1.2 keyboard-trap compliance)
function handleKeydown(e) {
  if (e.key === 'Escape' && sidebarOpen.value) {
    sidebarOpen.value = false
  }
}
onMounted(() => window.addEventListener('keydown', handleKeydown))
onUnmounted(() => window.removeEventListener('keydown', handleKeydown))
</script>

<style>
.fade-enter-active, .fade-leave-active { transition: opacity 0.15s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
