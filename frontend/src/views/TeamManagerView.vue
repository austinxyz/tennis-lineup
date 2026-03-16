<template>
  <div class="flex h-full overflow-hidden">
    <!-- Left: team list panel -->
    <TeamListPanel class="flex-shrink-0"/>

    <!-- Right: team detail or empty state -->
    <div class="flex-1 overflow-y-auto bg-gray-50">
      <router-view v-slot="{ Component, route }">
        <transition name="fade" mode="out-in">
          <component :is="Component" :key="route.params.id"/>
        </transition>
      </router-view>
    </div>
  </div>
</template>

<script setup>
import TeamListPanel from '../components/TeamListPanel.vue'
</script>
