<template>
  <div class="space-y-3">
    <!-- Preset dropdown -->
    <div class="flex items-center gap-2">
      <select
        v-model="selectedPresetId"
        class="flex-1 border border-gray-300 rounded-lg px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-green-500"
      >
        <option value="">暂无预设</option>
        <option v-for="preset in presets" :key="preset.id" :value="preset.id">
          {{ preset.name }}
        </option>
      </select>
      <button
        class="px-3 py-1.5 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
        :disabled="!selectedPresetId"
        @click="loadSelected"
      >
        加载
      </button>
      <button
        class="px-3 py-1.5 text-sm bg-red-100 text-red-600 rounded-lg hover:bg-red-200 disabled:opacity-50 disabled:cursor-not-allowed"
        :disabled="!selectedPresetId"
        @click="deleteSelected"
      >
        删除
      </button>
    </div>

    <!-- Warning for missing players -->
    <div v-if="missingPlayersWarning" class="text-xs text-amber-600 bg-amber-50 border border-amber-200 rounded px-2 py-1">
      部分球员已不在队伍中，相关约束已跳过
    </div>

    <!-- Save preset row -->
    <div class="flex items-center gap-2">
      <input
        v-model="newPresetName"
        type="text"
        placeholder="预设名称..."
        class="flex-1 border border-gray-300 rounded-lg px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-green-500"
        @keyup.enter="saveCurrentConstraints"
      />
      <button
        class="px-3 py-1.5 text-sm bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed"
        :disabled="!newPresetName.trim() || saving"
        @click="saveCurrentConstraints"
      >
        {{ saving ? '保存中...' : '保存' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  presets: {
    type: Array,
    default: () => [],
  },
  players: {
    type: Array,
    default: () => [],
  },
  currentConstraints: {
    type: Object,
    default: () => ({ excludePlayers: [], includePlayers: [], pinPlayers: {} }),
  },
})

const emit = defineEmits(['load-preset', 'save-preset', 'delete-preset'])

const selectedPresetId = ref('')
const newPresetName = ref('')
const saving = ref(false)
const missingPlayersWarning = ref(false)

function loadSelected() {
  const preset = props.presets.find(p => p.id === selectedPresetId.value)
  if (!preset) return

  const playerIds = new Set(props.players.map(p => p.id))

  // Filter out players no longer on the team
  const filteredExclude = (preset.excludePlayers || []).filter(id => playerIds.has(id))
  const filteredInclude = (preset.includePlayers || []).filter(id => playerIds.has(id))
  const filteredPin = Object.fromEntries(
    Object.entries(preset.pinPlayers || {}).filter(([id]) => playerIds.has(id))
  )

  const hadMissing =
    (preset.excludePlayers || []).length !== filteredExclude.length ||
    (preset.includePlayers || []).length !== filteredInclude.length ||
    Object.keys(preset.pinPlayers || {}).length !== Object.keys(filteredPin).length

  missingPlayersWarning.value = hadMissing

  emit('load-preset', {
    excludePlayers: filteredExclude,
    includePlayers: filteredInclude,
    pinPlayers: filteredPin,
  })
}

async function saveCurrentConstraints() {
  if (!newPresetName.value.trim()) return
  saving.value = true
  try {
    await emit('save-preset', newPresetName.value.trim())
    newPresetName.value = ''
  } finally {
    saving.value = false
  }
}

function deleteSelected() {
  if (!selectedPresetId.value) return
  emit('delete-preset', selectedPresetId.value)
  selectedPresetId.value = ''
  missingPlayersWarning.value = false
}
</script>
