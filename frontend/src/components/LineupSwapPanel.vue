<template>
  <div class="p-3 bg-gray-50 rounded-lg text-sm">
    <div class="text-xs text-gray-500 mb-2">点选两名球员（需来自不同位置）进行互换</div>

    <!-- Player selection grid -->
    <div class="space-y-1 mb-3">
      <div
        v-for="pair in currentLineup.pairs"
        :key="pair.position"
        class="flex items-center gap-2"
      >
        <span class="w-7 text-xs font-bold text-green-600">{{ pair.position }}</span>
        <button
          type="button"
          class="px-2 py-0.5 rounded text-xs transition-colors"
          :class="isSelected(pair.position, 1) ? 'bg-blue-500 text-white' : 'bg-white border border-gray-200 hover:bg-gray-100'"
          @click="selectPlayer(pair.position, 1, pair.player1Id, pair.player1Name)"
        >{{ pair.player1Name }} ({{ pair.player1Utr ?? '—' }})</button>
        <button
          type="button"
          class="px-2 py-0.5 rounded text-xs transition-colors"
          :class="isSelected(pair.position, 2) ? 'bg-blue-500 text-white' : 'bg-white border border-gray-200 hover:bg-gray-100'"
          @click="selectPlayer(pair.position, 2, pair.player2Id, pair.player2Name)"
        >{{ pair.player2Name }} ({{ pair.player2Utr ?? '—' }})</button>
      </div>
    </div>

    <!-- Controls -->
    <div class="flex items-center gap-2">
      <button
        type="button"
        class="px-3 py-1 bg-green-600 text-white rounded text-xs font-medium disabled:opacity-40 disabled:cursor-not-allowed"
        :disabled="!canSwap"
        :title="canSwap ? '' : '请选择不同位置的球员'"
        @click="doSwap"
      >互换</button>
      <button
        type="button"
        class="px-3 py-1 bg-gray-200 text-gray-600 rounded text-xs font-medium hover:bg-gray-300"
        @click="reset"
      >重置</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  lineup: {
    type: Object,
    required: true,
  },
})

const emit = defineEmits(['update:lineup'])

// Deep clone to allow mutation
const currentLineup = ref(JSON.parse(JSON.stringify(props.lineup)))
const originalLineup = JSON.parse(JSON.stringify(props.lineup))

const selected = ref([]) // [{position, slot, playerId, playerName}]

function isSelected(position, slot) {
  return selected.value.some(s => s.position === position && s.slot === slot)
}

function selectPlayer(position, slot, playerId, playerName) {
  // If already selected, deselect
  const idx = selected.value.findIndex(s => s.position === position && s.slot === slot)
  if (idx !== -1) {
    selected.value.splice(idx, 1)
    return
  }
  // Replace if same position/slot already in selection with different
  if (selected.value.length >= 2) {
    selected.value = []
  }
  selected.value.push({ position, slot, playerId, playerName })
}

const canSwap = computed(() => {
  if (selected.value.length !== 2) return false
  return selected.value[0].position !== selected.value[1].position
})

function doSwap() {
  if (!canSwap.value) return
  const [a, b] = selected.value

  // Clone pairs
  const pairs = currentLineup.value.pairs.map(p => ({ ...p }))
  const pairA = pairs.find(p => p.position === a.position)
  const pairB = pairs.find(p => p.position === b.position)

  // Swap the specific slots
  const aId = a.slot === 1 ? pairA.player1Id : pairA.player2Id
  const aName = a.slot === 1 ? pairA.player1Name : pairA.player2Name
  const aUtr = a.slot === 1 ? pairA.player1Utr : pairA.player2Utr
  const aActualUtr = a.slot === 1 ? pairA.player1ActualUtr : pairA.player2ActualUtr
  const aGender = a.slot === 1 ? pairA.player1Gender : pairA.player2Gender
  const bId = b.slot === 1 ? pairB.player1Id : pairB.player2Id
  const bName = b.slot === 1 ? pairB.player1Name : pairB.player2Name
  const bUtr = b.slot === 1 ? pairB.player1Utr : pairB.player2Utr
  const bActualUtr = b.slot === 1 ? pairB.player1ActualUtr : pairB.player2ActualUtr
  const bGender = b.slot === 1 ? pairB.player1Gender : pairB.player2Gender

  if (a.slot === 1) { pairA.player1Id = bId; pairA.player1Name = bName; pairA.player1Utr = bUtr; pairA.player1ActualUtr = bActualUtr; pairA.player1Gender = bGender }
  else { pairA.player2Id = bId; pairA.player2Name = bName; pairA.player2Utr = bUtr; pairA.player2ActualUtr = bActualUtr; pairA.player2Gender = bGender }
  if (b.slot === 1) { pairB.player1Id = aId; pairB.player1Name = aName; pairB.player1Utr = aUtr; pairB.player1ActualUtr = aActualUtr; pairB.player1Gender = aGender }
  else { pairB.player2Id = aId; pairB.player2Name = aName; pairB.player2Utr = aUtr; pairB.player2ActualUtr = aActualUtr; pairB.player2Gender = aGender }

  // Recalculate combinedUtr for affected pairs
  pairA.combinedUtr = (pairA.player1Utr ?? 0) + (pairA.player2Utr ?? 0)
  pairB.combinedUtr = (pairB.player1Utr ?? 0) + (pairB.player2Utr ?? 0)

  // Auto-sort all 4 pairs by combinedUtr descending and reassign positions D1–D4
  const positions = ['D1', 'D2', 'D3', 'D4']
  pairs.sort((x, y) => y.combinedUtr - x.combinedUtr)
  pairs.forEach((p, i) => { p.position = positions[i] })

  currentLineup.value = { ...currentLineup.value, pairs }
  selected.value = []
  emit('update:lineup', currentLineup.value)
}

function reset() {
  currentLineup.value = JSON.parse(JSON.stringify(originalLineup))
  selected.value = []
  emit('update:lineup', currentLineup.value)
}
</script>
