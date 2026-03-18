<template>
  <div class="p-6 max-w-2xl mx-auto">
    <h2 class="text-2xl font-bold text-gray-900 mb-6">排阵生成</h2>

    <!-- Team selector -->
    <div class="mb-6">
      <label class="block text-sm font-medium text-gray-700 mb-1">选择队伍</label>
      <select
        v-model="selectedTeamId"
        class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-green-500"
      >
        <option value="" disabled>请选择队伍</option>
        <option v-for="team in teams" :key="team.id" :value="team.id">
          {{ team.name }}
        </option>
      </select>
    </div>

    <!-- Strategy selector -->
    <div class="mb-6">
      <label class="block text-sm font-medium text-gray-700 mb-2">选择策略</label>
      <StrategySelector @update:strategy="onStrategyChange" />
    </div>

    <!-- Generate button -->
    <button
      class="w-full py-3 bg-green-600 text-white rounded-lg font-medium hover:bg-green-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
      :disabled="!selectedTeamId || loading"
      @click="generate"
    >
      <span v-if="loading">生成中...</span>
      <span v-else>生成排阵</span>
    </button>

    <!-- Error -->
    <div v-if="errorMessage" class="mt-4 p-4 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm">
      {{ errorMessage }}
    </div>

    <!-- Result -->
    <div v-if="lineup" class="mt-6">
      <LineupCard :lineup="lineup" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useTeams } from '../composables/useTeams'
import { useLineup } from '../composables/useLineup'
import StrategySelector from '../components/StrategySelector.vue'
import LineupCard from '../components/LineupCard.vue'

const { teams, fetchTeams } = useTeams()
const { lineup, loading, error, generateLineup } = useLineup()

const selectedTeamId = ref('')
const strategy = ref({ strategyType: 'preset', preset: 'balanced', naturalLanguage: null })
const errorMessage = ref('')

fetchTeams()

function onStrategyChange(newStrategy) {
  strategy.value = newStrategy
}

async function generate() {
  errorMessage.value = ''
  try {
    await generateLineup({
      teamId: selectedTeamId.value,
      strategyType: strategy.value.strategyType,
      preset: strategy.value.preset,
      naturalLanguage: strategy.value.naturalLanguage,
    })
  } catch (err) {
    errorMessage.value = err.message || '排阵生成失败，请稍后重试'
  }
}
</script>
