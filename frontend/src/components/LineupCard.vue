<template>
  <div class="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
    <!-- Header -->
    <div class="px-5 py-4 border-b border-gray-100 flex items-center justify-between">
      <div>
        <span class="text-sm font-semibold text-gray-900">排阵结果</span>
        <span v-if="lineup.aiUsed" class="ml-2 px-2 py-0.5 bg-purple-100 text-purple-700 text-xs rounded-full">AI 优选</span>
        <span v-else class="ml-2 px-2 py-0.5 bg-gray-100 text-gray-500 text-xs rounded-full">启发式</span>
      </div>
      <span class="text-sm text-gray-500">总 UTR: <span class="font-semibold text-gray-800">{{ totalUtr }}</span></span>
    </div>

    <!-- Pairs -->
    <div class="divide-y divide-gray-50">
      <div
        v-for="pair in sortedPairs"
        :key="pair.position"
        class="px-5 py-3 flex items-center gap-4"
      >
        <span class="w-8 text-xs font-bold text-green-600">{{ pair.position }}</span>
        <div class="flex-1 text-sm text-gray-800">
          <template v-if="showPlayerUtr">
            {{ pair.player1Name }} ({{ pair.player1Utr ?? '—' }}) / {{ pair.player2Name }} ({{ pair.player2Utr ?? '—' }})
          </template>
          <template v-else>
            {{ pair.player1Name }} / {{ pair.player2Name }}
          </template>
        </div>
        <span class="text-xs text-gray-500">{{ pair.combinedUtr.toFixed(2) }}</span>
      </div>
    </div>

    <!-- Strategy -->
    <div class="px-5 py-3 border-t border-gray-100 bg-gray-50 text-xs text-gray-500">
      策略: {{ lineup.strategy }}
      <span v-if="!lineup.aiUsed" class="ml-2 text-amber-600">（AI 不可用，已降级）</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  lineup: {
    type: Object,
    required: true,
  },
  showPlayerUtr: {
    type: Boolean,
    default: true,
  },
})

const positionOrder = ['D1', 'D2', 'D3', 'D4']

const sortedPairs = computed(() =>
  [...props.lineup.pairs].sort(
    (a, b) => positionOrder.indexOf(a.position) - positionOrder.indexOf(b.position)
  )
)

const totalUtr = computed(() => props.lineup.totalUtr?.toFixed(2) ?? '—')
</script>
