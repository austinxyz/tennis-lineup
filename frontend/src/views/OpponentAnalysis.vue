<template>
  <div class="p-6">
    <h2 class="text-2xl font-bold text-gray-900 mb-6">对手策略分析</h2>

    <!-- Mode toggle tabs -->
    <div class="flex border-b border-gray-200 mb-6">
      <button
        class="px-5 py-2 text-sm font-medium border-b-2 transition"
        :class="mode === 'generate' ? 'border-green-600 text-green-700' : 'border-transparent text-gray-500 hover:text-gray-700'"
        @click="mode = 'generate'"
      >
        排阵生成
      </button>
      <button
        class="px-5 py-2 text-sm font-medium border-b-2 transition"
        :class="mode === 'saved' ? 'border-green-600 text-green-700' : 'border-transparent text-gray-500 hover:text-gray-700'"
        @click="mode = 'saved'"
      >
        已保存对比
      </button>
    </div>

    <!-- Controls (shared) -->
    <div class="bg-white rounded-xl border border-gray-200 shadow-sm p-5 mb-6">
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <!-- Own team selector -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">己方队伍</label>
          <select
            v-model="ownTeamId"
            class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-green-500"
            @change="onOwnTeamChange"
          >
            <option value="" disabled>请选择队伍</option>
            <option v-for="team in teams" :key="team.id" :value="team.id">
              {{ team.name }}
            </option>
          </select>
        </div>

        <!-- Opponent team selector -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">对手队伍</label>
          <select
            v-model="opponentTeamId"
            class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-green-500"
            @change="onOpponentTeamChange"
          >
            <option value="" disabled>请选择对手</option>
            <option v-for="team in teams" :key="team.id" :value="team.id">
              {{ team.name }}
            </option>
          </select>
        </div>

        <!-- Opponent lineup selector -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">对手排阵</label>
          <select
            v-if="opponentLineups.length > 0"
            v-model="opponentLineupId"
            class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-green-500"
          >
            <option value="" disabled>请选择排阵</option>
            <option v-for="lineup in opponentLineups" :key="lineup.id" :value="lineup.id">
              {{ formatLineupLabel(lineup) }}
            </option>
          </select>
          <p v-else-if="opponentTeamId" class="text-sm text-gray-500 mt-2">对手队伍暂无保存排阵</p>
          <p v-else class="text-sm text-gray-400 mt-2">请先选择对手队伍</p>
        </div>
      </div>

      <!-- Action button -->
      <div class="mt-4 flex items-center gap-3">
        <!-- 排阵生成 mode -->
        <button
          v-if="mode === 'generate'"
          :disabled="!canAnalyze || loading"
          class="px-5 py-2 bg-green-600 text-white text-sm font-medium rounded-lg hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed transition"
          @click="onAnalyze"
        >
          {{ loading ? '分析中…' : '分析' }}
        </button>

        <!-- 已保存对比 mode -->
        <button
          v-else
          :disabled="!canCompare || matchupLoading"
          class="px-5 py-2 bg-green-600 text-white text-sm font-medium rounded-lg hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed transition"
          @click="onRunMatchup"
        >
          {{ matchupLoading ? '对比中…' : '对比' }}
        </button>

        <span v-if="actionError" class="text-sm text-red-500">{{ actionError }}</span>
      </div>
    </div>

    <!-- 排阵生成 Results -->
    <div v-if="mode === 'generate' && result" class="grid grid-cols-1 xl:grid-cols-2 gap-6">
      <!-- UTR Recommendation -->
      <div>
        <h3 class="text-lg font-semibold text-gray-800 mb-3">UTR 比较推荐</h3>

        <div class="bg-white rounded-xl border border-gray-200 shadow-sm mb-4 overflow-hidden">
          <table class="w-full text-sm">
            <thead class="bg-gray-50 text-gray-500 text-xs">
              <tr>
                <th class="px-4 py-2 text-left">线</th>
                <th class="px-4 py-2 text-right">己方 UTR</th>
                <th class="px-4 py-2 text-right">对手 UTR</th>
                <th class="px-4 py-2 text-right">差值</th>
                <th class="px-4 py-2 text-center">胜率</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-100">
              <tr v-for="line in result.utrRecommendation.lineAnalysis" :key="line.position">
                <td class="px-4 py-2 font-bold text-green-600">{{ line.position }}</td>
                <td class="px-4 py-2 text-right text-gray-700">{{ line.ownCombinedUtr.toFixed(1) }}</td>
                <td class="px-4 py-2 text-right text-gray-700">{{ line.opponentCombinedUtr.toFixed(1) }}</td>
                <td class="px-4 py-2 text-right" :class="line.delta > 0 ? 'text-green-600' : line.delta < 0 ? 'text-red-500' : 'text-gray-500'">
                  {{ line.delta > 0 ? '+' : '' }}{{ line.delta.toFixed(2) }}
                </td>
                <td class="px-4 py-2 text-center">
                  <span :class="winLabelClass(line.winProbability)" class="px-2 py-0.5 rounded-full text-xs font-medium">
                    {{ line.label }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
          <div class="px-4 py-2 bg-gray-50 border-t border-gray-100 text-sm text-gray-700">
            预期得分: <span class="font-semibold">{{ result.utrRecommendation.expectedScore }}</span> / 10
            <span class="text-gray-400 ml-2">(对手: {{ result.utrRecommendation.opponentExpectedScore }})</span>
          </div>
        </div>

        <LineupCard :lineup="result.utrRecommendation.lineup" :show-player-utr="true" />
      </div>

      <!-- AI Recommendation -->
      <div>
        <h3 class="text-lg font-semibold text-gray-800 mb-3">AI 排阵建议</h3>

        <!-- Not yet triggered -->
        <div v-if="!result.aiRecommendation" class="bg-gray-50 border border-gray-200 rounded-xl p-5 flex flex-col items-center gap-3">
          <p class="text-sm text-gray-500">点击下方按钮，让 AI 分析对手排阵并给出最优建议</p>
          <button
            :disabled="aiLoading"
            class="px-5 py-2 bg-purple-600 text-white text-sm font-medium rounded-lg hover:bg-purple-700 disabled:opacity-50 disabled:cursor-not-allowed transition"
            @click="onAiAnalyze"
          >
            {{ aiLoading ? 'AI 分析中…' : 'AI 排阵分析' }}
          </button>
          <span v-if="aiError" class="text-xs text-red-500">{{ aiError }}</span>
        </div>

        <!-- AI result shown -->
        <template v-else>
          <div v-if="!result.aiRecommendation.aiUsed" class="bg-yellow-50 border border-yellow-200 rounded-lg px-4 py-3 text-sm text-yellow-700 mb-4">
            {{ result.aiRecommendation.explanation }}
          </div>
          <div v-else class="bg-purple-50 border border-purple-200 rounded-lg px-4 py-3 text-sm text-purple-700 mb-4">
            {{ result.aiRecommendation.explanation }}
          </div>
          <LineupCard :lineup="result.aiRecommendation.lineup" :show-player-utr="true" />
          <button
            :disabled="aiLoading"
            class="mt-3 px-4 py-1.5 text-xs text-purple-600 border border-purple-300 rounded-lg hover:bg-purple-50 disabled:opacity-50 transition"
            @click="onAiAnalyze"
          >
            {{ aiLoading ? 'AI 分析中…' : '重新分析' }}
          </button>
        </template>
      </div>
    </div>

    <!-- 已保存对比 Results -->
    <div v-if="mode === 'saved'">
      <!-- Own team no lineups warning -->
      <div v-if="ownTeamId && ownLineups.length === 0 && !matchupLoading" class="bg-yellow-50 border border-yellow-200 rounded-lg px-4 py-3 text-sm text-yellow-700 mb-4">
        己方队伍暂无保存排阵，请先保存排阵
      </div>

      <!-- Matchup results list -->
      <div v-if="matchupResults.length > 0" class="space-y-6 max-h-[70vh] overflow-y-auto pr-1">
        <div
          v-for="(res, idx) in matchupResults"
          :key="res.lineup.id"
          class="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden"
        >
          <!-- Result header -->
          <div class="flex items-center justify-between px-5 py-3 border-b border-gray-100 bg-gray-50">
            <span class="text-sm font-medium text-gray-600">#{{ idx + 1 }} {{ formatLineupLabel(res.lineup) }}</span>
            <span
              class="px-3 py-1 rounded-full text-xs font-semibold"
              :class="verdictClass(res.verdict)"
            >
              {{ res.verdict }}
            </span>
          </div>

          <!-- Per-line comparison: own | delta+verdict | opponent -->
          <div class="divide-y divide-gray-50">
            <div
              v-for="line in res.lineAnalysis"
              :key="line.position"
              class="grid grid-cols-[auto_1fr_auto_1fr] items-center gap-x-3 px-5 py-3"
            >
              <!-- Position label -->
              <span class="w-8 text-xs font-bold text-green-600">{{ line.position }}</span>

              <!-- Own pair -->
              <div class="text-sm text-gray-800 min-w-0">
                {{ pairText(res.lineup, line.position) }}
              </div>

              <!-- Delta + win label -->
              <div class="flex flex-col items-center gap-1 px-2">
                <span
                  class="text-xs font-medium"
                  :class="line.delta > 0 ? 'text-green-600' : line.delta < 0 ? 'text-red-500' : 'text-gray-400'"
                >
                  {{ line.delta > 0 ? '+' : '' }}{{ line.delta.toFixed(1) }}
                </span>
                <span :class="winLabelClass(line.winProbability)" class="px-2 py-0.5 rounded-full text-xs font-medium whitespace-nowrap">
                  {{ line.label }}
                </span>
              </div>

              <!-- Opponent pair -->
              <div class="text-sm text-gray-500 min-w-0 text-right">
                {{ pairText(res.opponentLineup, line.position) }}
              </div>
            </div>

            <!-- Score footer -->
            <div class="px-5 py-2 bg-gray-50 flex justify-between text-xs text-gray-500">
              <span class="font-medium text-gray-700">己方总 UTR: {{ res.lineup.totalUtr?.toFixed(1) }}</span>
              <span>预期得分 <span class="font-semibold text-gray-800">{{ res.expectedScore }}</span> / 10</span>
              <span class="font-medium text-gray-700">对手总 UTR: {{ res.opponentLineup?.totalUtr?.toFixed(1) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import LineupCard from '../components/LineupCard.vue'
import { useTeams } from '../composables/useTeams'
import { useOpponentAnalysis } from '../composables/useOpponentAnalysis'
import { useSavedLineupMatchup } from '../composables/useSavedLineupMatchup'
import { useApi } from '../composables/useApi'

const { teams, fetchTeams } = useTeams()
const { loading, result, analyzeOpponent } = useOpponentAnalysis()
const { loading: matchupLoading, matchupResults, runMatchup } = useSavedLineupMatchup()
const { get } = useApi()

const mode = ref('generate')
const ownTeamId = ref('')
const opponentTeamId = ref('')
const opponentLineupId = ref('')
const opponentLineups = ref([])
const ownLineups = ref([])
const actionError = ref('')
const aiLoading = ref(false)
const aiError = ref('')

const canAnalyze = computed(() => ownTeamId.value && opponentLineupId.value)
const canCompare = computed(() => ownTeamId.value && opponentLineupId.value)

onMounted(() => {
  fetchTeams()
})

async function onOwnTeamChange() {
  ownLineups.value = []
  if (!ownTeamId.value) return
  try {
    ownLineups.value = await get(`/api/teams/${ownTeamId.value}/lineups`)
  } catch (err) {
    console.error('Failed to fetch own lineups:', err)
  }
}

async function onOpponentTeamChange() {
  opponentLineupId.value = ''
  opponentLineups.value = []
  if (!opponentTeamId.value) return
  try {
    opponentLineups.value = await get(`/api/teams/${opponentTeamId.value}/lineups`)
  } catch (err) {
    console.error('Failed to fetch opponent lineups:', err)
  }
}

async function onAnalyze() {
  actionError.value = ''
  try {
    await analyzeOpponent(ownTeamId.value, opponentTeamId.value, opponentLineupId.value, {}, false)
  } catch (err) {
    actionError.value = err.message || '分析失败，请重试'
  }
}

async function onAiAnalyze() {
  aiError.value = ''
  aiLoading.value = true
  try {
    await analyzeOpponent(ownTeamId.value, opponentTeamId.value, opponentLineupId.value, {}, true)
  } catch (err) {
    aiError.value = err.message || 'AI 分析失败，请重试'
  } finally {
    aiLoading.value = false
  }
}

async function onRunMatchup() {
  actionError.value = ''
  try {
    await runMatchup(ownTeamId.value, opponentTeamId.value, opponentLineupId.value)
  } catch (err) {
    actionError.value = err.message || '对比失败，请重试'
  }
}

function formatLineupLabel(lineup) {
  const date = lineup.createdAt ? new Date(lineup.createdAt).toLocaleDateString('zh-CN') : ''
  const utr = lineup.totalUtr != null ? ` (UTR ${lineup.totalUtr.toFixed(1)})` : ''
  return `${date}${utr}`
}

function winLabelClass(winProbability) {
  if (winProbability >= 0.7) return 'bg-green-100 text-green-700'
  if (winProbability >= 0.55) return 'bg-green-50 text-green-600'
  if (winProbability === 0.5) return 'bg-gray-100 text-gray-600'
  if (winProbability >= 0.35) return 'bg-red-50 text-red-500'
  return 'bg-red-100 text-red-600'
}

function pairText(lineup, position) {
  if (!lineup?.pairs) return '—'
  const pair = lineup.pairs.find(p => p.position === position)
  if (!pair) return '—'
  const g1 = pair.player1Gender === 'female' ? '女' : pair.player1Gender === 'male' ? '男' : ''
  const g2 = pair.player2Gender === 'female' ? '女' : pair.player2Gender === 'male' ? '男' : ''
  const u1 = pair.player1Utr != null ? pair.player1Utr : '—'
  const u2 = pair.player2Utr != null ? pair.player2Utr : '—'
  return `${g1}${pair.player1Name}(${u1}) / ${g2}${pair.player2Name}(${u2})`
}

function verdictClass(verdict) {
  if (verdict === '能赢') return 'bg-green-100 text-green-700'
  if (verdict === '势均力敌') return 'bg-yellow-100 text-yellow-700'
  return 'bg-red-100 text-red-600'
}
</script>
