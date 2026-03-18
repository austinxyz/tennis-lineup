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
        v-for="(lineup, index) in localLineups"
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
        <!-- Save button -->
        <div class="mt-2 flex items-center gap-2">
          <button
            v-if="!savedStates[index]"
            class="px-3 py-1.5 text-xs rounded-lg border border-blue-300 text-blue-600 hover:bg-blue-50 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            :disabled="savingStates[index]"
            @click="handleSave(index, lineup)"
          >
            {{ savingStates[index] ? '保留中...' : '保留此排阵' }}
          </button>
          <span v-else class="text-xs text-green-600 font-medium">已保留 ✓</span>
          <span v-if="saveErrors[index]" class="text-xs text-red-500">{{ saveErrors[index] }}</span>
        </div>
        <!-- Swap panel (collapsible) -->
        <details class="mt-1">
          <summary class="text-xs text-gray-400 cursor-pointer select-none hover:text-gray-600 py-1">
            调整配对
          </summary>
          <LineupSwapPanel :lineup="lineup" @update:lineup="(l) => { localLineups[index] = l }" />
        </details>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import LineupCard from './LineupCard.vue'
import LineupSwapPanel from './LineupSwapPanel.vue'
import { useLineup } from '../composables/useLineup'

const props = defineProps({
  lineups: {
    type: Array,
    default: () => [],
  },
  teamId: {
    type: String,
    default: '',
  },
})

const { saveLineup } = useLineup()

// Local mutable copy so swap changes are reflected in card and saved correctly
const localLineups = ref([])
const savedStates = ref([])
const savingStates = ref([])
const saveErrors = ref([])

watch(() => props.lineups, (newLineups) => {
  localLineups.value = newLineups.map(l => ({ ...l, pairs: l.pairs ? l.pairs.map(p => ({ ...p })) : [] }))
  savedStates.value = new Array(newLineups.length).fill(false)
  savingStates.value = new Array(newLineups.length).fill(false)
  saveErrors.value = new Array(newLineups.length).fill('')
}, { immediate: true })

const handleSave = async (index, lineup) => {
  savingStates.value[index] = true
  saveErrors.value[index] = ''
  try {
    await saveLineup(props.teamId, lineup)
    savedStates.value[index] = true
  } catch (err) {
    saveErrors.value[index] = err.message || '保留失败，请重试'
  } finally {
    savingStates.value[index] = false
  }
}
</script>
