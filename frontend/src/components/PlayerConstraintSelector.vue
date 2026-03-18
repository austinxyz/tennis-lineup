<template>
  <div>
    <!-- Empty state -->
    <div v-if="!players || players.length === 0" class="text-sm text-gray-400 italic py-2">
      请先选择队伍
    </div>

    <template v-else>
      <!-- Summary row -->
      <div class="text-xs text-gray-500 mb-2">
        必须上场: <span class="font-semibold text-green-600">{{ includeCount }} 人</span>
        &nbsp;/&nbsp;
        排除: <span class="font-semibold text-red-500">{{ excludeCount }} 人</span>
      </div>

      <!-- Player list -->
      <div class="space-y-1">
        <div
          v-for="player in players"
          :key="player.id"
          class="flex items-center justify-between px-3 py-2 rounded-lg border text-sm"
          :class="rowClass(player.id)"
        >
          <span :class="{ 'line-through text-gray-400': states[player.id] === 'exclude' }">
            {{ player.name }}
            <span class="text-xs text-gray-400 ml-1">UTR {{ player.utr }}</span>
          </span>
          <button
            type="button"
            class="ml-2 px-2 py-0.5 rounded text-xs font-medium transition-colors"
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

const props = defineProps({
  players: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:constraints'])

// state per player: 'neutral' | 'include' | 'exclude'
const states = ref({})

// Reset states when players list changes
watch(
  () => props.players,
  () => {
    states.value = {}
  }
)

function toggle(playerId) {
  const current = states.value[playerId] || 'neutral'
  if (current === 'neutral') states.value[playerId] = 'include'
  else if (current === 'include') states.value[playerId] = 'exclude'
  else states.value[playerId] = 'neutral'

  emit('update:constraints', {
    includePlayers: includePlayers.value,
    excludePlayers: excludePlayers.value,
  })
}

const includePlayers = computed(() =>
  Object.entries(states.value)
    .filter(([, s]) => s === 'include')
    .map(([id]) => id)
)

const excludePlayers = computed(() =>
  Object.entries(states.value)
    .filter(([, s]) => s === 'exclude')
    .map(([id]) => id)
)

const includeCount = computed(() => includePlayers.value.length)
const excludeCount = computed(() => excludePlayers.value.length)

function rowClass(playerId) {
  const s = states.value[playerId] || 'neutral'
  if (s === 'include') return 'border-green-300 bg-green-50'
  if (s === 'exclude') return 'border-red-200 bg-red-50'
  return 'border-gray-200 bg-white'
}

function btnClass(playerId) {
  const s = states.value[playerId] || 'neutral'
  if (s === 'include') return 'bg-green-100 text-green-700 hover:bg-green-200'
  if (s === 'exclude') return 'bg-red-100 text-red-600 hover:bg-red-200'
  return 'bg-gray-100 text-gray-500 hover:bg-gray-200'
}

function btnLabel(playerId) {
  const s = states.value[playerId] || 'neutral'
  if (s === 'include') return '必须上场'
  if (s === 'exclude') return '排除'
  return '中立'
}
</script>
