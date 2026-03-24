<template>
  <div>
    <!-- Empty state -->
    <div v-if="!players || players.length === 0" class="text-sm text-gray-400 italic py-2">
      请先选择队伍
    </div>

    <template v-else>
      <!-- Summary row -->
      <div class="text-xs text-gray-500 mb-2 flex flex-wrap gap-x-3">
        <span>固定位置: <span class="font-semibold text-blue-600">{{ pinCount }} 人</span></span>
        <span>一定上场: <span class="font-semibold text-green-600">{{ includeOnlyCount }} 人</span></span>
        <span>排除: <span class="font-semibold text-red-500">{{ excludeCount }} 人</span></span>
      </div>

      <!-- Player list (sorted: females first, then UTR desc) -->
      <div class="space-y-1">
        <div
          v-for="player in sortedPlayers"
          :key="player.id"
          class="flex items-center justify-between px-3 py-2 rounded-lg border text-sm"
          :class="rowClass(player.id)"
        >
          <span class="flex items-center gap-1.5 flex-1 min-w-0" :class="{ 'line-through text-gray-400': states[player.id] === 'exclude' }">
            <!-- Gender badge -->
            <span
              class="text-xs font-bold px-1 rounded shrink-0"
              :class="player.gender === 'female' ? 'bg-pink-100 text-pink-600' : 'bg-blue-100 text-blue-600'"
            >{{ player.gender === 'female' ? '女' : '男' }}</span>
            <span class="truncate">{{ player.name }}</span>
            <span class="text-xs text-gray-400 shrink-0">{{ player.utr }}</span>
            <span
              v-if="player.actualUtr != null && player.actualUtr !== player.utr"
              class="text-xs text-orange-500 shrink-0"
            >实:{{ Number(player.actualUtr).toFixed(2) }}</span>
            <span
              v-if="player.verified"
              class="shrink-0 px-1 py-0.5 bg-green-100 text-green-700 text-xs rounded"
            >认证</span>
          </span>
          <select
            class="ml-2 text-xs rounded border px-1 py-0.5 cursor-pointer shrink-0"
            :class="selectClass(player.id)"
            :value="states[player.id] || 'neutral'"
            @change="onChange(player.id, $event.target.value)"
          >
            <option value="neutral">中立</option>
            <option value="exclude">不上</option>
            <option value="include">一定上</option>
            <option value="D1">D1</option>
            <option value="D2">D2</option>
            <option value="D3">D3</option>
            <option value="D4">D4</option>
          </select>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  players: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:constraints'])

// state per player: 'neutral' | 'exclude' | 'include' | 'D1' | 'D2' | 'D3' | 'D4'
const states = ref({})

// Reset states when players list changes
watch(
  () => props.players,
  () => { states.value = {} }
)

// Sorted: females first, then UTR descending, ties broken by name
const sortedPlayers = computed(() => {
  if (!props.players) return []
  return [...props.players].sort((a, b) => {
    if (a.gender !== b.gender) return a.gender === 'female' ? -1 : 1
    if (b.utr !== a.utr) return b.utr - a.utr
    return a.name.localeCompare(b.name)
  })
})

function onChange(playerId, value) {
  states.value[playerId] = value
  emitConstraints()
}

function emitConstraints() {
  const pinPlayers = {}
  const includePlayers = []
  const excludePlayers = []

  Object.entries(states.value).forEach(([id, s]) => {
    if (s === 'D1' || s === 'D2' || s === 'D3' || s === 'D4') {
      pinPlayers[id] = s
      includePlayers.push(id) // pinned players are implicitly included
    } else if (s === 'include') {
      includePlayers.push(id)
    } else if (s === 'exclude') {
      excludePlayers.push(id)
    }
  })

  emit('update:constraints', { pinPlayers, includePlayers, excludePlayers })
}

const pinCount = computed(() =>
  Object.values(states.value).filter(s => s === 'D1' || s === 'D2' || s === 'D3' || s === 'D4').length
)

const includeOnlyCount = computed(() =>
  Object.values(states.value).filter(s => s === 'include').length
)

const excludeCount = computed(() =>
  Object.values(states.value).filter(s => s === 'exclude').length
)

function rowClass(playerId) {
  const s = states.value[playerId] || 'neutral'
  if (s === 'D1' || s === 'D2' || s === 'D3' || s === 'D4') return 'border-blue-300 bg-blue-50'
  if (s === 'include') return 'border-green-300 bg-green-50'
  if (s === 'exclude') return 'border-red-200 bg-red-50'
  return 'border-gray-200 bg-white'
}

function selectClass(playerId) {
  const s = states.value[playerId] || 'neutral'
  if (s === 'D1' || s === 'D2' || s === 'D3' || s === 'D4') return 'border-blue-300 text-blue-700 bg-blue-50'
  if (s === 'include') return 'border-green-300 text-green-700 bg-green-50'
  if (s === 'exclude') return 'border-red-300 text-red-600 bg-red-50'
  return 'border-gray-300 text-gray-500 bg-white'
}

// Allow parent to load preset states
function loadStates(newStates) {
  states.value = { ...newStates }
  emitConstraints()
}

defineExpose({ loadStates })
</script>
