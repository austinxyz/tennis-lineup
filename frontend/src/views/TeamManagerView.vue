<template>
  <div class="flex flex-1 min-h-0">
    <div
      data-testid="team-list-panel-wrapper"
      :class="[
        'lg:block',
        teamSelected ? 'hidden' : 'block w-full lg:w-auto'
      ]"
    >
      <TeamListPanel class="flex-shrink-0"/>
    </div>
    <div
      data-testid="team-detail-wrapper"
      :class="[
        'flex-1 min-w-0',
        teamSelected ? 'block lg:block' : 'hidden lg:block'
      ]"
    >
      <router-view v-slot="{ Component, route }">
        <transition name="fade" mode="out-in">
          <component :is="Component" :key="route.params.id"/>
        </transition>
      </router-view>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import TeamListPanel from '../components/TeamListPanel.vue'

const route = useRoute()
const teamSelected = computed(() => Boolean(route.params?.id))
</script>
