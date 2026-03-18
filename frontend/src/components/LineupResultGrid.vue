<template>
  <div>
    <!-- Empty state -->
    <div
      v-if="!lineups || lineups.length === 0"
      class="flex items-center justify-center h-48 rounded-xl border-2 border-dashed border-gray-200 text-gray-400 text-sm"
    >
      生成排阵后结果将显示在此处
    </div>

    <!-- Grid: 2 columns on desktop, 1 column on mobile -->
    <div v-else class="grid grid-cols-1 lg:grid-cols-2 gap-4">
      <div
        v-for="(lineup, index) in lineups"
        :key="lineup.id || index"
        class="flex flex-col"
      >
        <!-- Plan header -->
        <div
          class="flex items-center gap-2 mb-2"
          :class="index === 0 ? 'text-green-700' : 'text-gray-600'"
        >
          <span class="text-sm font-semibold">方案 {{ index + 1 }}</span>
          <span
            v-if="index === 0"
            class="px-1.5 py-0.5 bg-green-100 text-green-700 text-xs rounded-full"
          >最佳</span>
        </div>
        <!-- Lineup card with best-plan highlight -->
        <div
          class="flex-1 rounded-xl overflow-hidden"
          :class="index === 0 ? 'ring-2 ring-green-400' : ''"
        >
          <LineupCard :lineup="lineup" :show-player-utr="true" />
        </div>
        <!-- Swap panel (collapsible) -->
        <details class="mt-1">
          <summary class="text-xs text-gray-400 cursor-pointer select-none hover:text-gray-600 py-1">
            调整配对
          </summary>
          <LineupSwapPanel :lineup="lineup" @update:lineup="(l) => $emit('update:lineup', index, l)" />
        </details>
      </div>
    </div>
  </div>
</template>

<script setup>
import LineupCard from './LineupCard.vue'
import LineupSwapPanel from './LineupSwapPanel.vue'

defineProps({
  lineups: {
    type: Array,
    default: () => [],
  },
})

defineEmits(['update:lineup'])
</script>
