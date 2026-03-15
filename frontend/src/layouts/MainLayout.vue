<template>
  <div class="flex h-screen bg-gray-100 overflow-hidden">

    <!-- Mobile header -->
    <header class="lg:hidden fixed top-0 left-0 right-0 z-50 bg-white border-b border-gray-200 h-14 flex items-center px-4">
      <button @click="mobileOpen = !mobileOpen" class="p-2 rounded-lg hover:bg-gray-100 mr-3">
        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16"/>
        </svg>
      </button>
      <span class="font-bold text-gray-900">网球排阵管理</span>
    </header>

    <!-- Mobile overlay -->
    <div v-if="mobileOpen" @click="mobileOpen = false" class="lg:hidden fixed inset-0 bg-black/40 z-40"/>

    <!-- Nav Sidebar -->
    <aside :class="[
      'fixed top-0 bottom-0 z-40 transition-transform duration-300 lg:static lg:translate-x-0',
      mobileOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'
    ]">
      <NavSidebar @navigate="mobileOpen = false"/>
    </aside>

    <!-- Main content -->
    <div class="flex-1 flex flex-col overflow-hidden pt-14 lg:pt-0">
      <router-view/>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import NavSidebar from '../components/NavSidebar.vue'

const mobileOpen = ref(false)
</script>

<style>
.fade-enter-active, .fade-leave-active { transition: opacity 0.15s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
