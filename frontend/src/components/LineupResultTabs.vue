<template>
  <div>
    <!-- Empty state -->
    <div
      v-if="!lineups || lineups.length === 0"
      class="flex items-center justify-center h-48 rounded-xl border-2 border-dashed border-gray-200 text-gray-400 text-sm"
    >
      生成排阵后结果将显示在此处
    </div>

    <template v-else>
      <!-- Tab grid: 3 columns, up to 2 rows -->
      <div class="grid grid-cols-3 gap-2 mb-4">
        <button
          v-for="(lineup, index) in lineups"
          :key="lineup.id || index"
          type="button"
          class="py-2 px-3 rounded-lg text-sm font-medium transition-colors text-center"
          :class="
            activeIndex === index
              ? 'bg-green-600 text-white'
              : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
          "
          @click="activeIndex = index"
        >
          方案 {{ index + 1 }}
        </button>
      </div>

      <!-- Active lineup card -->
      <LineupCard v-if="activeLineup" :lineup="activeLineup" />
    </template>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import LineupCard from './LineupCard.vue'

const props = defineProps({
  lineups: {
    type: Array,
    default: () => [],
  },
})

const activeIndex = ref(0)

// Reset to first tab when lineups change
watch(
  () => props.lineups,
  () => {
    activeIndex.value = 0
  }
)

const activeLineup = computed(() =>
  props.lineups && props.lineups.length > 0 ? props.lineups[activeIndex.value] : null
)
</script>
