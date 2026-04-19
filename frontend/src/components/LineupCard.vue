<template>
  <div class="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
    <!-- Header -->
    <div class="px-5 py-3 border-b border-gray-100 flex items-start justify-between gap-3">
      <div class="min-w-0 flex-1">
        <div class="flex items-center gap-1.5 flex-wrap">
          <span
            v-if="preferred"
            class="px-1.5 py-0.5 text-xs font-medium rounded-full bg-yellow-100 text-yellow-700 flex-shrink-0"
          >⭐ 首选</span>
          <span v-if="label" class="text-sm font-semibold text-gray-900 truncate">{{ label }}</span>
          <span v-else class="text-sm font-semibold text-gray-900">排阵结果</span>
        </div>
        <p v-if="comment" class="mt-1 text-xs text-gray-500 leading-snug line-clamp-2">{{ comment }}</p>
      </div>
      <div class="text-right shrink-0">
        <div class="text-sm text-gray-500">总 UTR: <span class="font-semibold text-gray-800">{{ totalUtr }}</span></div>
        <div class="text-sm text-orange-500">实际 UTR: <span class="font-semibold">{{ actualUtrSum }}</span></div>
      </div>
    </div>

    <!-- Pairs -->
    <div class="divide-y divide-gray-50">
      <template v-if="showPlayerUtr">
        <div
          v-for="pair in sortedPairs"
          :key="pair.position"
          class="px-4 py-2.5 grid grid-cols-[36px_1fr_auto] gap-3 items-start border-t border-gray-50 first:border-t-0"
        >
          <span class="text-xs font-bold text-green-600 pt-1">{{ pair.position }}</span>
          <div class="space-y-1 min-w-0">
            <div
              v-for="slot in [1, 2]"
              :key="slot"
              data-testid="pair-player-row"
              class="flex items-center gap-2 text-sm min-w-0"
            >
              <span
                v-if="pair[`player${slot}Gender`]"
                data-testid="gender-tag"
                class="text-xs font-bold px-1.5 py-0.5 rounded flex-shrink-0"
                :class="pair[`player${slot}Gender`] === 'female' ? 'bg-pink-100 text-pink-700' : 'bg-blue-100 text-blue-700'"
              >{{ pair[`player${slot}Gender`] === 'female' ? '女' : '男' }}</span>
              <span class="font-semibold text-gray-900 flex-1 truncate">{{ pair[`player${slot}Name`] }}</span>
              <span class="text-xs text-gray-500">{{ pair[`player${slot}Utr`] ?? '—' }}</span>
              <span
                v-if="pair[`player${slot}ActualUtr`] != null && pair[`player${slot}ActualUtr`] !== pair[`player${slot}Utr`]"
                class="text-xs text-amber-500 font-semibold"
              >实:{{ pair[`player${slot}ActualUtr`].toFixed(2) }}</span>
            </div>
          </div>
          <div class="text-right text-xs text-gray-500 pt-1 whitespace-nowrap">
            <div>{{ pair.combinedUtr.toFixed(2) }}</div>
            <div v-if="pairActualUtrSum(pair) !== null" class="text-xs text-amber-500">
              实:{{ pairActualUtrSum(pair) }}
            </div>
          </div>
        </div>
      </template>
      <template v-else>
        <div
          v-for="pair in sortedPairs"
          :key="pair.position"
          class="px-5 py-3 flex items-center gap-4"
        >
          <span class="w-8 text-xs font-bold text-green-600">{{ pair.position }}</span>
          <div class="flex-1 text-sm text-gray-800">
            {{ pair.player1Name }} / {{ pair.player2Name }}
          </div>
          <span class="text-xs text-gray-500">{{ pair.combinedUtr.toFixed(2) }}</span>
        </div>
      </template>
    </div>

    <!-- Strategy footer -->
    <div class="px-5 py-2 border-t border-gray-100 bg-gray-50 text-xs text-gray-500 flex items-center gap-2">
      <span>策略: {{ lineup.strategy }}</span>
      <span v-if="lineup.aiUsed" class="px-1.5 py-0.5 bg-purple-100 text-purple-700 rounded-full">AI 优选</span>
      <span v-else class="px-1.5 py-0.5 bg-gray-100 text-gray-500 rounded-full">启发式</span>
      <span v-if="!lineup.aiUsed" class="text-amber-600">（AI 不可用，已降级）</span>
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
  label: {
    type: String,
    default: null,
  },
  comment: {
    type: String,
    default: null,
  },
  preferred: {
    type: Boolean,
    default: false,
  },
})

const positionOrder = ['D1', 'D2', 'D3', 'D4']

// Defensive sort: unknown positions get sorted to the end instead of top.
const sortedPairs = computed(() =>
  [...props.lineup.pairs].sort((a, b) => {
    const ai = positionOrder.indexOf(a.position)
    const bi = positionOrder.indexOf(b.position)
    return (ai === -1 ? 99 : ai) - (bi === -1 ? 99 : bi)
  })
)

const totalUtr = computed(() => props.lineup.totalUtr?.toFixed(2) ?? '—')

const actualUtrSum = computed(() =>
  props.lineup.actualUtrSum?.toFixed(2) ?? props.lineup.totalUtr?.toFixed(2) ?? '—'
)

// Only sum non-null actualUtr values. Returns null when no player has actualUtr
// (so the caller can hide the "实:" row entirely and avoid misleading
// partial-sum values like "实:0.00" or mixing actual with scheduled UTR).
function pairActualUtrSum(pair) {
  const vals = [pair.player1ActualUtr, pair.player2ActualUtr].filter(v => v != null)
  if (vals.length === 0) return null
  return vals.reduce((s, v) => s + v, 0).toFixed(2)
}
</script>
