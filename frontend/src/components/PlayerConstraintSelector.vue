<template>
  <div>
    <!-- Empty state -->
    <div v-if="!players || players.length === 0" class="text-sm text-gray-400 italic py-2">
      请先选择队伍
    </div>

    <template v-else>
      <!-- Summary row -->
      <div class="text-xs text-gray-500 mb-2 space-x-2">
        <span>固定位置: <span class="font-semibold text-blue-600">{{ pinCount }} 人</span></span>
        <span>/</span>
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
          <span :class="{ 'line-through text-gray-400': states[player.id] === 'exclude' }">
            {{ player.name }}
            <span class="text-xs text-gray-400 ml-1">UTR {{ player.utr }}</span>
            <span
              v-if="player.verified"
              class="ml-1 px-1 py-0.5 bg-green-100 text-green-700 text-xs rounded"
            >认证</span>
          </span>
          <button
            type="button"
            class="ml-2 px-2 py-0.5 rounded text-xs font-medium transition-colors min-w-[52px] text-center"
            :class="btnClass(player.id)"
            @click="toggle(player.id)"
          >
            {{ btnLabel(player.id) }}
          </button>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const CYCLE = ['neutral', 'D1', 'D2', 'D3', 'D4', 'exclude']

const props = defineProps({
  players: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:constraints'])

// state per player: 'neutral' | 'D1' | 'D2' | 'D3' | 'D4' | 'exclude'
const states = ref({})

// Reset states when players list changes
watch(
  () => props.players,
  () => {
    states.value = {}
  }
)

// Sorted: females first, then UTR descending, ties broken by name
const sortedPlayers = computed(() => {
  if (!props.players) return []
  return [...props.players].sort((a, b) => {
    if (a.gender !== b.gender) {
      return a.gender === 'female' ? -1 : 1
    }
    if (b.utr !== a.utr) return b.utr - a.utr
    return a.name.localeCompare(b.name)
  })
})

function toggle(playerId) {
  const current = states.value[playerId] || 'neutral'
  const idx = CYCLE.indexOf(current)
  states.value[playerId] = CYCLE[(idx + 1) % CYCLE.length]

  emit('update:constraints', {
    pinPlayers: pinPlayersMap.value,
    excludePlayers: excludePlayers.value,
  })
}

const pinPlayersMap = computed(() => {
  const result = {}
  Object.entries(states.value).forEach(([id, s]) => {
    if (s === 'D1' || s === 'D2' || s === 'D3' || s === 'D4') {
      result[id] = s
    }
  })
  return result
})

const excludePlayers = computed(() =>
  Object.entries(states.value)
    .filter(([, s]) => s === 'exclude')
    .map(([id]) => id)
)

const pinCount = computed(() => Object.keys(pinPlayersMap.value).length)
const excludeCount = computed(() => excludePlayers.value.length)

function rowClass(playerId) {
  const s = states.value[playerId] || 'neutral'
  if (s === 'D1' || s === 'D2' || s === 'D3' || s === 'D4') return 'border-blue-300 bg-blue-50'
  if (s === 'exclude') return 'border-red-200 bg-red-50'
  return 'border-gray-200 bg-white'
}

function btnClass(playerId) {
  const s = states.value[playerId] || 'neutral'
  if (s === 'D1' || s === 'D2' || s === 'D3' || s === 'D4') return 'bg-blue-100 text-blue-700 hover:bg-blue-200'
  if (s === 'exclude') return 'bg-red-100 text-red-600 hover:bg-red-200'
  return 'bg-gray-100 text-gray-500 hover:bg-gray-200'
}

function btnLabel(playerId) {
  const s = states.value[playerId] || 'neutral'
  if (s === 'neutral') return '中立'
  if (s === 'exclude') return '排除'
  return s
}
</script>
