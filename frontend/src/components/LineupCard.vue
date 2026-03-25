<template>
  <div class="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
    <!-- Header -->
    <div class="px-5 py-4 border-b border-gray-100 flex items-center justify-between">
      <div>
        <span class="text-sm font-semibold text-gray-900">排阵结果</span>
        <span v-if="lineup.aiUsed" class="ml-2 px-2 py-0.5 bg-purple-100 text-purple-700 text-xs rounded-full">AI 优选</span>
        <span v-else class="ml-2 px-2 py-0.5 bg-gray-100 text-gray-500 text-xs rounded-full">启发式</span>
      </div>
      <div class="text-right">
        <div class="text-sm text-gray-500">总 UTR: <span class="font-semibold text-gray-800">{{ totalUtr }}</span></div>
        <div class="text-sm text-orange-500">实际 UTR: <span class="font-semibold">{{ actualUtrSum }}</span></div>
      </div>
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
            <span v-if="pair.player1Gender" :class="pair.player1Gender === 'female' ? 'text-pink-500' : 'text-blue-500'" class="text-xs font-semibold mr-0.5">{{ pair.player1Gender === 'female' ? '女' : '男' }}</span>{{ pair.player1Name }} (<span class="text-gray-600">{{ pair.player1Utr ?? '—' }}</span> / <span class="text-orange-500 text-xs">实:{{ effectiveActualUtr(pair.player1ActualUtr, pair.player1Utr) }}</span>)
            /
            <span v-if="pair.player2Gender" :class="pair.player2Gender === 'female' ? 'text-pink-500' : 'text-blue-500'" class="text-xs font-semibold mr-0.5">{{ pair.player2Gender === 'female' ? '女' : '男' }}</span>{{ pair.player2Name }} (<span class="text-gray-600">{{ pair.player2Utr ?? '—' }}</span> / <span class="text-orange-500 text-xs">实:{{ effectiveActualUtr(pair.player2ActualUtr, pair.player2Utr) }}</span>)
          </template>
          <template v-else>
            {{ pair.player1Name }} / {{ pair.player2Name }}
          </template>
        </div>
        <div v-if="showPlayerUtr" class="text-right shrink-0">
          <div class="text-xs text-gray-500">{{ pair.combinedUtr.toFixed(2) }}</div>
          <div class="text-xs text-orange-500">实:{{ pairActualUtr(pair) }}</div>
        </div>
        <span v-else class="text-xs text-gray-500">{{ pair.combinedUtr.toFixed(2) }}</span>
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

const actualUtrSum = computed(() => {
  if (props.lineup.actualUtrSum != null) return props.lineup.actualUtrSum.toFixed(2)
  // Fallback: compute from pairs when actualUtrSum not available
  if (!props.lineup.pairs) return props.lineup.totalUtr?.toFixed(2) ?? '—'
  const sum = props.lineup.pairs.reduce((acc, pair) => {
    const a1 = pair.player1ActualUtr ?? pair.player1Utr ?? 0
    const a2 = pair.player2ActualUtr ?? pair.player2Utr ?? 0
    return acc + a1 + a2
  }, 0)
  return sum.toFixed(2)
})

function effectiveActualUtr(actualUtr, utr) {
  const val = actualUtr ?? utr
  return val != null ? Number(val).toFixed(2) : '—'
}

function pairActualUtr(pair) {
  const a1 = pair.player1ActualUtr ?? pair.player1Utr ?? 0
  const a2 = pair.player2ActualUtr ?? pair.player2Utr ?? 0
  return (a1 + a2).toFixed(2)
}
</script>
