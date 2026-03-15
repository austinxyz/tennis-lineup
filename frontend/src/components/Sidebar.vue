<template>
  <!-- Desktop sidebar: always visible, static in flex flow -->
  <div class="w-64 flex-shrink-0 bg-white border-r border-gray-200 min-h-screen hidden md:flex flex-col">
    <div class="h-16 flex items-center px-6 border-b border-gray-200">
      <h2 class="text-lg font-semibold text-gray-800">导航</h2>
    </div>
    <nav class="mt-4 flex-1">
      <router-link
        to="/"
        class="flex items-center px-6 py-3 text-gray-700 hover:bg-gray-100 hover:text-gray-900 transition-colors"
        active-class="bg-blue-50 text-blue-700">
        <svg class="w-5 h-5 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
        </svg>
        队伍列表
      </router-link>
      <router-link
        to="/import"
        class="flex items-center px-6 py-3 text-gray-700 hover:bg-gray-100 hover:text-gray-900 transition-colors"
        active-class="bg-blue-50 text-blue-700">
        <svg class="w-5 h-5 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
        </svg>
        批量导入
      </router-link>
    </nav>
  </div>

  <!-- Mobile: top bar with hamburger -->
  <div class="md:hidden fixed top-0 left-0 right-0 z-50 bg-white border-b border-gray-200 h-14 flex items-center px-4">
    <button @click="mobileOpen = !mobileOpen" class="p-2 rounded-md hover:bg-gray-100 mr-3">
      <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
      </svg>
    </button>
    <span class="font-semibold text-gray-800">网球排阵管理系统</span>
  </div>

  <!-- Mobile drawer -->
  <div v-if="mobileOpen" class="md:hidden fixed inset-0 z-40" @click.self="mobileOpen = false">
    <div class="w-64 bg-white h-full shadow-lg">
      <nav class="mt-4">
        <router-link to="/" @click="mobileOpen = false"
          class="flex items-center px-6 py-3 text-gray-700 hover:bg-gray-100">队伍列表</router-link>
        <router-link to="/import" @click="mobileOpen = false"
          class="flex items-center px-6 py-3 text-gray-700 hover:bg-gray-100">批量导入</router-link>
      </nav>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
const mobileOpen = ref(false)
</script>