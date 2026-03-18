<template>
  <div class="space-y-4">
    <!-- Mode toggle -->
    <div class="flex gap-2">
      <button
        class="px-4 py-2 rounded-lg text-sm font-medium transition-colors"
        :class="mode === 'preset' ? 'bg-green-600 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'"
        @click="setMode('preset')"
      >
        预设策略
      </button>
      <button
        class="px-4 py-2 rounded-lg text-sm font-medium transition-colors"
        :class="mode === 'custom' ? 'bg-green-600 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'"
        @click="setMode('custom')"
      >
        自定义策略
      </button>
    </div>

    <!-- Preset options -->
    <div v-if="mode === 'preset'" class="flex gap-3">
      <button
        class="flex-1 px-4 py-3 rounded-lg border-2 text-sm font-medium transition-colors"
        :class="preset === 'balanced' ? 'border-green-600 bg-green-50 text-green-700' : 'border-gray-200 text-gray-600 hover:border-gray-300'"
        @click="selectPreset('balanced')"
      >
        <div class="font-semibold">均衡</div>
        <div class="text-xs text-gray-500 mt-1">各线 UTR 均衡分配</div>
      </button>
      <button
        class="flex-1 px-4 py-3 rounded-lg border-2 text-sm font-medium transition-colors"
        :class="preset === 'aggressive' ? 'border-green-600 bg-green-50 text-green-700' : 'border-gray-200 text-gray-600 hover:border-gray-300'"
        @click="selectPreset('aggressive')"
      >
        <div class="font-semibold">集中火力</div>
        <div class="text-xs text-gray-500 mt-1">强化前三线</div>
      </button>
    </div>

    <!-- Custom natural language -->
    <div v-if="mode === 'custom'" class="space-y-2">
      <label class="block text-sm font-medium text-gray-700">策略描述</label>
      <textarea
        v-model="naturalLanguage"
        rows="3"
        class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-green-500"
        placeholder="例如：让前三线尽量强，第四线放弃"
        @input="emitCustom"
      />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const emit = defineEmits(['update:strategy'])

const mode = ref('preset')
const preset = ref('balanced')
const naturalLanguage = ref('')

function setMode(newMode) {
  mode.value = newMode
  if (newMode === 'preset') {
    emitPreset(preset.value)
  } else {
    emitCustom()
  }
}

function selectPreset(value) {
  preset.value = value
  emitPreset(value)
}

function emitPreset(value) {
  emit('update:strategy', { strategyType: 'preset', preset: value, naturalLanguage: null })
}

function emitCustom() {
  emit('update:strategy', { strategyType: 'custom', preset: null, naturalLanguage: naturalLanguage.value })
}

// Emit initial value
emitPreset(preset.value)
</script>
