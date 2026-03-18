<template>
  <div class="p-6">
    <h2 class="text-2xl font-bold text-gray-900 mb-6">排阵生成</h2>

    <div class="flex flex-col lg:flex-row gap-6">
      <!-- Left column: controls -->
      <div class="w-full lg:w-2/5 flex flex-col max-h-[calc(100vh-10rem)]">
        <!-- Scrollable area: team, strategy, player constraints -->
        <div class="flex-1 overflow-y-auto space-y-6 pr-1">
          <!-- Team selector -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">选择队伍</label>
            <select
              v-model="selectedTeamId"
              class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-green-500"
              @change="onTeamChange"
            >
              <option value="" disabled>请选择队伍</option>
              <option v-for="team in teams" :key="team.id" :value="team.id">
                {{ team.name }}
              </option>
            </select>
          </div>

          <!-- Strategy selector -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">选择策略</label>
            <StrategySelector @update:strategy="onStrategyChange" />
          </div>

          <!-- Player constraint selector -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">球员约束</label>
            <PlayerConstraintSelector
              :players="teamPlayers"
              @update:constraints="onConstraintsChange"
            />
          </div>
        </div>

        <!-- Always-visible bottom: generate button + error -->
        <div class="pt-4 space-y-3 shrink-0">
          <button
            class="w-full py-3 bg-green-600 text-white rounded-lg font-medium hover:bg-green-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            :disabled="!selectedTeamId || loading"
            @click="generate"
          >
            <span v-if="loading">生成中...</span>
            <span v-else>生成排阵</span>
          </button>

          <div v-if="errorMessage" class="p-4 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm">
            {{ errorMessage }}
          </div>
        </div>
      </div>

      <!-- Right column: results -->
      <div class="w-full lg:w-3/5 overflow-y-auto max-h-[calc(100vh-10rem)] pr-1">
        <LineupResultGrid :lineups="lineups" :team-id="selectedTeamId" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useTeams } from '../composables/useTeams'
import { useLineup } from '../composables/useLineup'
import { usePlayers } from '../composables/usePlayers'
import StrategySelector from '../components/StrategySelector.vue'
import PlayerConstraintSelector from '../components/PlayerConstraintSelector.vue'
import LineupResultGrid from '../components/LineupResultGrid.vue'

const { teams, fetchTeams } = useTeams()
const { lineups, loading, generateLineup } = useLineup()

const selectedTeamId = ref('')
const strategy = ref({ strategyType: 'preset', preset: 'balanced', naturalLanguage: null })
const constraints = ref({ pinPlayers: {}, includePlayers: [], excludePlayers: [] })
const errorMessage = ref('')
const teamPlayers = ref([])

fetchTeams()

function onStrategyChange(newStrategy) {
  strategy.value = newStrategy
}

function onConstraintsChange(newConstraints) {
  constraints.value = newConstraints
}

async function onTeamChange() {
  teamPlayers.value = []
  constraints.value = { pinPlayers: {}, includePlayers: [], excludePlayers: [] }
  if (!selectedTeamId.value) return
  try {
    const { players, fetchPlayers } = usePlayers(selectedTeamId.value)
    await fetchPlayers()
    teamPlayers.value = players.value
  } catch {
    // non-critical — constraint selector shows empty state
  }
}

async function generate() {
  errorMessage.value = ''
  try {
    await generateLineup({
      teamId: selectedTeamId.value,
      strategyType: strategy.value.strategyType,
      preset: strategy.value.preset,
      naturalLanguage: strategy.value.naturalLanguage,
      pinPlayers: constraints.value.pinPlayers,
      includePlayers: constraints.value.includePlayers,
      excludePlayers: constraints.value.excludePlayers,
    })
  } catch (err) {
    errorMessage.value = err.message || '排阵生成失败，请稍后重试'
  }
}
</script>
