<template>
  <div class="flex flex-col min-h-full">
    <AppHeader title="对手分析" />

    <div class="pt-14 lg:pt-0 p-4 lg:p-6 max-w-3xl mx-auto w-full">

      <!-- ── Our side ── -->
      <div class="text-center text-xs text-gray-500 font-semibold tracking-widest my-3">· 我 方 ·</div>

      <label class="block text-xs text-gray-500 font-semibold mb-1 uppercase">队伍</label>
      <select
        data-testid="select-my-team"
        v-model="myTeamId"
        class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white focus:outline-none focus:ring-2 focus:ring-green-500"
      >
        <option value="" disabled>选择我方队伍</option>
        <option v-for="t in teams" :key="t.id" :value="t.id" :disabled="t.id === oppTeamId">{{ t.name }}</option>
      </select>

      <label class="block text-xs text-gray-500 font-semibold mb-1 mt-3 uppercase">排阵</label>
      <select
        data-testid="select-my-lineup"
        v-model="myLineupId"
        :disabled="!myTeamId || myLineups.length === 0"
        class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white disabled:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-green-500"
      >
        <option value="" disabled>
          {{ !myTeamId ? '请先选队伍' : (myLineups.length ? '选择排阵' : '该队伍暂无排阵，请先添加') }}
        </option>
        <option v-for="l in myLineups" :key="l.id" :value="l.id">
          {{ l.label || l.strategy }} (总{{ (l.totalUtr || 0).toFixed(2) }})
        </option>
      </select>

      <!-- My lineup preview -->
      <div
        v-if="myLineup"
        data-testid="my-preview"
        class="mt-2 bg-white border border-gray-200 rounded-lg p-3 text-xs"
      >
        <div class="text-gray-500 font-semibold uppercase mb-1">我方预览</div>
        <div v-for="pair in sortedPairs(myLineup)" :key="pair.position" class="flex items-center gap-2 py-1">
          <span class="w-7 text-xs font-bold text-green-600">{{ pair.position }}</span>
          <div class="flex-1 flex flex-wrap gap-1 items-center">
            <span
              v-if="pair.player1Gender"
              class="text-xs font-bold px-1 rounded"
              :class="pair.player1Gender === 'female' ? 'bg-pink-100 text-pink-700' : 'bg-blue-100 text-blue-700'"
            >{{ pair.player1Gender === 'female' ? '女' : '男' }}</span>
            <span class="font-medium">{{ pair.player1Name }}</span>
            <span class="text-gray-400">/</span>
            <span
              v-if="pair.player2Gender"
              class="text-xs font-bold px-1 rounded"
              :class="pair.player2Gender === 'female' ? 'bg-pink-100 text-pink-700' : 'bg-blue-100 text-blue-700'"
            >{{ pair.player2Gender === 'female' ? '女' : '男' }}</span>
            <span class="font-medium">{{ pair.player2Name }}</span>
          </div>
        </div>
      </div>

      <!-- Lineup load error (if any fetch failed) -->
      <div v-if="lineupLoadError" data-testid="lineup-load-error" class="mt-2 px-3 py-2 bg-red-50 border border-red-200 rounded text-xs text-red-700">
        {{ lineupLoadError }}
      </div>

      <!-- ── Opponent side ── -->
      <div class="text-center text-xs text-gray-500 font-semibold tracking-widest my-4">· 对 手 ·</div>

      <label class="block text-xs text-gray-500 font-semibold mb-1 uppercase">队伍</label>
      <select
        data-testid="select-opp-team"
        v-model="oppTeamId"
        class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white focus:outline-none focus:ring-2 focus:ring-green-500"
      >
        <option value="" disabled>选择对手队伍</option>
        <option v-for="t in teams" :key="t.id" :value="t.id" :disabled="t.id === myTeamId">
          {{ t.name }}
        </option>
      </select>

      <label class="block text-xs text-gray-500 font-semibold mb-1 mt-3 uppercase">对手排阵</label>
      <select
        data-testid="select-opp-lineup"
        v-model="oppLineupId"
        :disabled="!oppTeamId || oppLineups.length === 0"
        class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white disabled:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-green-500"
      >
        <option value="" disabled>
          {{ !oppTeamId ? '请先选对手' : (oppLineups.length ? '选择排阵' : '该队伍暂无排阵，请先添加') }}
        </option>
        <option v-for="l in oppLineups" :key="l.id" :value="l.id">
          {{ l.label || l.strategy }} (总{{ (l.totalUtr || 0).toFixed(2) }})
        </option>
      </select>

      <!-- Opponent lineup preview (red-tinted) -->
      <div
        v-if="oppLineup"
        data-testid="opp-preview"
        class="mt-2 bg-red-50 border border-red-200 rounded-lg p-3 text-xs"
      >
        <div class="text-red-700 font-semibold uppercase mb-1">对手预览</div>
        <div v-for="pair in sortedPairs(oppLineup)" :key="pair.position" class="flex items-center gap-2 py-1">
          <span class="w-7 text-xs font-bold text-red-600">{{ pair.position }}</span>
          <div class="flex-1 flex flex-wrap gap-1 items-center">
            <span
              v-if="pair.player1Gender"
              class="text-xs font-bold px-1 rounded"
              :class="pair.player1Gender === 'female' ? 'bg-pink-100 text-pink-700' : 'bg-blue-100 text-blue-700'"
            >{{ pair.player1Gender === 'female' ? '女' : '男' }}</span>
            <span class="font-medium">{{ pair.player1Name }}</span>
            <span class="text-gray-400">/</span>
            <span
              v-if="pair.player2Gender"
              class="text-xs font-bold px-1 rounded"
              :class="pair.player2Gender === 'female' ? 'bg-pink-100 text-pink-700' : 'bg-blue-100 text-blue-700'"
            >{{ pair.player2Gender === 'female' ? '女' : '男' }}</span>
            <span class="font-medium">{{ pair.player2Name }}</span>
          </div>
        </div>
      </div>

      <!-- ── Analyze button ── -->
      <button
        data-testid="analyze-btn"
        type="button"
        @click="analyze"
        :disabled="!canAnalyze || analyzing"
        class="w-full mt-4 py-3 bg-blue-600 text-white rounded-lg font-semibold disabled:bg-gray-300 disabled:cursor-not-allowed hover:bg-blue-700 transition"
      >{{ analyzing ? '分析中...' : '开始分析' }}</button>

      <span v-if="analyzeError" class="block mt-2 text-sm text-red-500 text-center">{{ analyzeError }}</span>

      <!-- ── Results section ── -->
      <div v-if="result" data-testid="analysis-result" class="mt-6 space-y-6">

        <!-- Per-lineup result cards -->
        <template v-if="result.results && result.results.length > 0">
          <div
            v-for="(res, idx) in result.results"
            :key="res.lineup ? res.lineup.id : idx"
            class="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden"
          >
            <div class="flex items-center justify-between px-5 py-3 border-b border-gray-100 bg-gray-50">
              <span class="text-sm font-medium text-gray-600">#{{ idx + 1 }}</span>
              <div class="flex items-center gap-3">
                <span class="text-xs text-gray-500">预期得分 <span class="font-semibold text-gray-800">{{ res.expectedScore }}</span> / 10</span>
                <span class="px-3 py-1 rounded-full text-xs font-semibold" :class="verdictClass(res.verdict)">
                  {{ res.verdict }}
                </span>
              </div>
            </div>

            <div class="divide-y divide-gray-50">
              <div
                v-for="line in res.lineAnalysis"
                :key="line.position"
                class="grid grid-cols-[auto_1fr_auto_1fr] items-center gap-x-3 px-5 py-3"
              >
                <span class="w-8 text-xs font-bold text-green-600">{{ line.position }}</span>
                <div class="text-sm text-gray-800 min-w-0">
                  {{ pairText(res.lineup, line.position) }}
                  <span class="text-xs text-gray-400 ml-1">{{ dualUtrLabel(line.ownCombinedRegularUtr, line.ownCombinedUtr) }}</span>
                </div>
                <div class="flex flex-col items-center gap-1 px-2">
                  <span class="text-xs font-medium" :class="line.delta > 0 ? 'text-green-600' : line.delta < 0 ? 'text-red-500' : 'text-gray-400'">
                    {{ line.delta > 0 ? '+' : '' }}{{ line.delta.toFixed(1) }}
                  </span>
                  <span :class="winLabelClass(line.winProbability)" class="px-2 py-0.5 rounded-full text-xs font-medium whitespace-nowrap">
                    {{ line.label }}
                  </span>
                </div>
                <div class="text-sm text-gray-500 min-w-0 text-right">
                  {{ pairText(oppLineup, line.position) }}
                  <span class="text-xs text-gray-400 ml-1">{{ dualUtrLabel(line.opponentCombinedUtr, line.opponentCombinedActualUtr) }}</span>
                </div>
              </div>
            </div>
          </div>
        </template>

        <!-- AI Recommendation card -->
        <div
          v-if="result.aiRecommendation"
          class="bg-white rounded-xl border border-purple-200 shadow-sm overflow-hidden"
        >
          <div class="flex items-center justify-between px-5 py-3 border-b border-purple-100 bg-purple-50">
            <div class="flex items-center gap-2">
              <span class="text-sm font-semibold text-purple-800">AI 推荐排阵</span>
              <span v-if="!result.aiRecommendation.aiUsed" class="px-2 py-0.5 bg-yellow-100 text-yellow-700 text-xs rounded-full">AI 不可用</span>
            </div>
            <span class="text-xs text-gray-500">预期得分 <span class="font-semibold text-gray-800">{{ result.aiRecommendation.expectedScore }}</span> / 10</span>
          </div>

          <div v-if="result.aiRecommendation.explanation" class="px-5 py-2 text-sm text-purple-700 border-b border-purple-100 bg-purple-50/50">
            {{ result.aiRecommendation.explanation }}
          </div>

          <div class="divide-y divide-gray-50">
            <div
              v-for="line in result.aiRecommendation.lineAnalysis"
              :key="line.position"
              class="grid grid-cols-[auto_1fr_auto_1fr] items-center gap-x-3 px-5 py-3"
            >
              <span class="w-8 text-xs font-bold text-green-600">{{ line.position }}</span>
              <div class="text-sm text-gray-800 min-w-0">
                {{ pairText(result.aiRecommendation.lineup, line.position) }}
                <span class="text-xs text-gray-400 ml-1">{{ dualUtrLabel(line.ownCombinedRegularUtr, line.ownCombinedUtr) }}</span>
              </div>
              <div class="flex flex-col items-center gap-1 px-2">
                <span class="text-xs font-medium" :class="line.delta > 0 ? 'text-green-600' : line.delta < 0 ? 'text-red-500' : 'text-gray-400'">
                  {{ line.delta > 0 ? '+' : '' }}{{ line.delta.toFixed(1) }}
                </span>
                <span :class="winLabelClass(line.winProbability)" class="px-2 py-0.5 rounded-full text-xs font-medium whitespace-nowrap">
                  {{ line.label }}
                </span>
              </div>
              <div class="text-sm text-gray-500 min-w-0 text-right">
                {{ pairText(result.aiRecommendation.opponentLineup, line.position) }}
                <span class="text-xs text-gray-400 ml-1">{{ dualUtrLabel(line.opponentCombinedUtr, line.opponentCombinedActualUtr) }}</span>
              </div>
            </div>
          </div>
        </div>

      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import AppHeader from '../components/AppHeader.vue'
import { useTeams } from '../composables/useTeams'
import { useApi } from '../composables/useApi'

const { teams, fetchTeams } = useTeams()
const { get, post } = useApi()

// ── State ──
const myTeamId = ref('')
const myLineupId = ref('')
const oppTeamId = ref('')
const oppLineupId = ref('')
const myLineups = ref([])
const oppLineups = ref([])
const analyzing = ref(false)
const analyzeError = ref('')
const result = ref(null)

onMounted(() => fetchTeams())

// ── Watches: load lineups on team change ──
const lineupLoadError = ref('')

watch(myTeamId, async (id) => {
  myLineupId.value = ''
  myLineups.value = []
  lineupLoadError.value = ''
  if (!id) return
  try {
    myLineups.value = await get(`/api/teams/${id}/lineups`)
  } catch (err) {
    lineupLoadError.value = err.message || '加载我方排阵失败，请重试'
  }
})

watch(oppTeamId, async (id) => {
  oppLineupId.value = ''
  oppLineups.value = []
  lineupLoadError.value = ''
  if (!id) return
  try {
    oppLineups.value = await get(`/api/teams/${id}/lineups`)
  } catch (err) {
    lineupLoadError.value = err.message || '加载对手排阵失败，请重试'
  }
})

// ── Computed ──
const myLineup = computed(() => myLineups.value.find(l => l.id === myLineupId.value) || null)
const oppLineup = computed(() => oppLineups.value.find(l => l.id === oppLineupId.value) || null)
const canAnalyze = computed(() => Boolean(myLineup.value) && Boolean(oppLineup.value))

// ── Analyze ──
async function analyze() {
  if (!canAnalyze.value) return
  analyzing.value = true
  analyzeError.value = ''
  result.value = null
  try {
    result.value = await post('/api/lineups/matchup', {
      teamId: myTeamId.value,
      opponentTeamId: oppTeamId.value,
      ownLineupId: myLineupId.value,
      opponentLineupId: oppLineupId.value,
      includeAi: true,
      ownPartnerNotes: buildPartnerNotes(myLineup.value),
      opponentPartnerNotes: buildPartnerNotes(oppLineup.value),
    })
  } catch (err) {
    analyzeError.value = err.message || '分析失败，请重试'
  } finally {
    analyzing.value = false
  }
}

// ── Helpers ──
// Pair stores notes per-player (player1Notes/player2Notes), not on the pair itself.
// Combine both into a single DTO note when either is present.
function buildPartnerNotes(lineup) {
  if (!lineup?.pairs) return []
  const result = []
  for (const p of lineup.pairs) {
    const notes = [p.player1Notes, p.player2Notes].filter(Boolean)
    if (notes.length === 0) continue
    result.push({
      player1Name: p.player1Name,
      player2Name: p.player2Name,
      note: notes.join(' | '),
    })
  }
  return result
}

function sortedPairs(lineup) {
  if (!lineup?.pairs) return []
  const order = ['D1', 'D2', 'D3', 'D4', 'S1', 'S2', 'S3']
  return [...lineup.pairs].sort((a, b) => {
    const ai = order.indexOf(a.position)
    const bi = order.indexOf(b.position)
    return (ai === -1 ? 99 : ai) - (bi === -1 ? 99 : bi)
  })
}

function pairText(lineup, position) {
  if (!lineup?.pairs) return '—'
  const pair = lineup.pairs.find(p => p.position === position)
  if (!pair) return '—'
  const g1 = pair.player1Gender === 'female' ? '女' : pair.player1Gender === 'male' ? '男' : ''
  const g2 = pair.player2Gender === 'female' ? '女' : pair.player2Gender === 'male' ? '男' : ''
  const p1 = playerUtrLabel(pair.player1Utr, pair.player1ActualUtr)
  const p2 = playerUtrLabel(pair.player2Utr, pair.player2ActualUtr)
  return `${g1}${pair.player1Name}(${p1}) / ${g2}${pair.player2Name}(${p2})`
}

function playerUtrLabel(utr, actualUtr) {
  if (utr == null) return '—'
  if (actualUtr != null && Math.abs(actualUtr - utr) >= 0.05) {
    return `${utr}/实${actualUtr}`
  }
  return `${utr}`
}

function dualUtrLabel(regularUtr, actualUtr) {
  if (regularUtr == null && actualUtr == null) return ''
  const base = actualUtr != null ? actualUtr : regularUtr
  if (regularUtr != null && actualUtr != null && Math.abs(actualUtr - regularUtr) >= 0.05) {
    return `(${regularUtr.toFixed(1)}/实${actualUtr.toFixed(1)})`
  }
  return `(${base.toFixed(1)})`
}

function winLabelClass(winProbability) {
  if (winProbability >= 0.7) return 'bg-green-100 text-green-700'
  if (winProbability >= 0.55) return 'bg-green-50 text-green-600'
  // Tolerant 50/50 band (floating-point safe: 0.5 ± ~0.05 → neutral)
  if (winProbability >= 0.45 && winProbability <= 0.55) return 'bg-gray-100 text-gray-600'
  if (winProbability >= 0.35) return 'bg-red-50 text-red-500'
  return 'bg-red-100 text-red-600'
}

function verdictClass(verdict) {
  if (verdict === '能赢') return 'bg-green-100 text-green-700'
  if (verdict === '势均力敌') return 'bg-yellow-100 text-yellow-700'
  return 'bg-red-100 text-red-600'
}
</script>
