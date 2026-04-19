<template>
  <div class="flex flex-col flex-1 min-h-0">
    <!-- Mobile-only AppHeader: visible when no team is selected
         (TeamDetail renders its own AppHeader when a team is selected) -->
    <AppHeader v-if="!teamSelected" title="队伍列表" />

    <div class="flex flex-1 min-h-0" :class="{ 'pt-14 lg:pt-0': !teamSelected }">
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
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from '../components/AppHeader.vue'
import TeamListPanel from '../components/TeamListPanel.vue'

const route = useRoute()
const teamSelected = computed(() => Boolean(route.params?.id))
</script>
