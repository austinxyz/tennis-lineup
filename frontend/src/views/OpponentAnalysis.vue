<template>
  <div class="p-6">
    <h2 class="text-2xl font-bold text-gray-900 mb-6">对手策略分析</h2>

    <!-- Mode radio toggle -->
    <div class="flex gap-1 bg-gray-100 rounded-lg p-1 w-fit mb-6">
      <button
        class="px-5 py-2 text-sm font-medium rounded-md transition"
        :class="analysisMode === 'bestThree' ? 'bg-white text-green-700 shadow-sm' : 'text-gray-500 hover:text-gray-700'"
        @click="onModeChange('bestThree')"
      >
        最佳三阵
      </button>
      <button
        class="px-5 py-2 text-sm font-medium rounded-md transition"
        :class="analysisMode === 'headToHead' ? 'bg-white text-green-700 shadow-sm' : 'text-gray-500 hover:text-gray-700'"
        @click="onModeChange('headToHead')"
      >
        逐线对比
      </button>
    </div>

    <!-- Shared controls -->
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

        <!-- Opponent lineup selector + preview -->
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
          <p v-else-if="opponentTeamId" class="text-sm text-yellow-600 mt-2">对手队伍暂无保存排阵</p>
          <p v-else class="text-sm text-gray-400 mt-2">请先选择对手队伍</p>
          <!-- Opponent lineup preview -->
          <div v-if="opponentLineupPreviewPairs.length > 0" class="mt-2 text-xs text-gray-400 space-y-0.5">
            <div v-for="pair in opponentLineupPreviewPairs" :key="pair.position">
              {{ pair.position }}: {{ pairPreviewText(pair) }}
            </div>
          </div>
        </div>
      </div>

      <!-- Opponent partner notes (scouting notes) -->
      <div v-if="opponentTeamId && opponentPlayers.length >= 2" class="mt-3">
        <button
          @click="showOppNotes = !showOppNotes"
          class="text-xs text-gray-500 hover:text-gray-700 flex items-center gap-1"
        >
          <span>对手搭档笔记</span>
          <span>{{ showOppNotes ? '▲' : '▼' }}</span>
        </button>
        <div v-if="showOppNotes" class="mt-2 bg-gray-50 rounded-lg border border-gray-200 p-3">
          <PartnerNotesEditor :teamId="opponentTeamId" :players="opponentPlayers" />
        </div>
      </div>

      <!-- 逐线对比 mode: own lineup selector + preview -->
      <div v-if="analysisMode === 'headToHead'" class="mt-4">
        <label class="block text-sm font-medium text-gray-700 mb-1">己方排阵</label>
        <select
          v-if="ownLineups.length > 0"
          v-model="ownLineupId"
          class="w-full md:w-1/3 border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-green-500"
        >
          <option value="" disabled>请选择己方排阵</option>
          <option v-for="lineup in ownLineups" :key="lineup.id" :value="lineup.id">
            {{ formatLineupLabel(lineup) }}
          </option>
        </select>
        <p v-else-if="ownTeamId" class="text-sm text-yellow-600 mt-1">己方队伍暂无保存排阵，请先保存排阵</p>
        <p v-else class="text-sm text-gray-400 mt-1">请先选择己方队伍</p>
        <!-- Own lineup preview -->
        <div v-if="ownLineupPreviewPairs.length > 0" class="mt-2 text-xs text-gray-400 space-y-0.5">
          <div v-for="pair in ownLineupPreviewPairs" :key="pair.position">
            {{ pair.position }}: {{ pair.player1Name }} + {{ pair.player2Name }}
          </div>
        </div>
      </div>

      <!-- Action buttons -->
      <div class="mt-4 flex items-center gap-3">
        <button
          v-if="analysisMode === 'bestThree'"
          :disabled="!canRunBestThree || loading"
          class="px-5 py-2 bg-green-600 text-white text-sm font-medium rounded-lg hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed transition"
          @click="onRunBestThree"
        >
          {{ loading ? '查找中…' : '查找最佳三阵' }}
        </button>

        <button
          v-else
          :disabled="!canRunHeadToHead || loading"
          class="px-5 py-2 bg-green-600 text-white text-sm font-medium rounded-lg hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed transition"
          @click="onRunHeadToHead"
        >
          {{ loading ? '对比中…' : '对比分析' }}
        </button>

        <span v-if="error" class="text-sm text-red-500">{{ error }}</span>
      </div>
    </div>

    <!-- 最佳三阵 results: two-column layout -->
    <div v-if="analysisMode === 'bestThree' && bestThreeResults.length > 0" class="grid grid-cols-1 lg:grid-cols-2 gap-6 items-start">

      <!-- Left column: UTR-based top 3 -->
      <div class="space-y-4">
        <div class="flex items-center gap-2 mb-1">
          <span class="text-sm font-semibold text-gray-700">UTR 最佳三阵</span>
          <span class="px-2 py-0.5 bg-green-100 text-green-700 text-xs rounded-full">算法推荐</span>
        </div>
        <div
          v-for="(res, idx) in bestThreeResults"
          :key="res.lineup.id"
          class="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden"
        >
          <div class="flex items-center justify-between px-5 py-3 border-b border-gray-100 bg-gray-50">
            <span class="text-sm font-medium text-gray-600">#{{ idx + 1 }} {{ formatLineupLabel(res.lineup) }}</span>
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
                {{ pairText(opponentLineupObj, line.position) }}
                <span class="text-xs text-gray-400 ml-1">{{ dualUtrLabel(line.opponentCombinedUtr, line.opponentCombinedActualUtr) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Right column: AI recommendation -->
      <div class="space-y-4">
        <div class="flex items-center gap-2 mb-1">
          <span class="text-sm font-semibold text-gray-700">AI 推荐</span>
          <span class="px-2 py-0.5 bg-purple-100 text-purple-700 text-xs rounded-full">智能分析</span>
        </div>

        <!-- Trigger button (before result) -->
        <div v-if="!aiResult" class="flex flex-col items-center justify-center bg-white rounded-xl border border-dashed border-purple-200 px-6 py-12 gap-3">
          <span class="text-sm text-gray-400">基于比赛策略和球员特点，AI 给出最佳出场推荐</span>
          <span v-if="aiError" class="text-xs text-red-500">{{ aiError }}</span>
          <button
            :disabled="aiLoading"
            class="px-5 py-2 bg-purple-600 text-white text-sm font-medium rounded-lg hover:bg-purple-700 disabled:opacity-50 disabled:cursor-not-allowed transition"
            @click="onRunBestThreeAi"
          >
            {{ aiLoading ? 'AI 分析中…' : '获取 AI 推荐' }}
          </button>
        </div>

        <!-- AI result card -->
        <div v-else class="bg-white rounded-xl border border-purple-200 shadow-sm overflow-hidden">
          <div class="flex items-center justify-between px-5 py-3 border-b border-purple-100 bg-purple-50">
            <div class="flex items-center gap-2">
              <span class="text-sm font-semibold text-purple-800">AI 推荐排阵</span>
              <span v-if="!aiResult.aiUsed" class="px-2 py-0.5 bg-yellow-100 text-yellow-700 text-xs rounded-full">AI 不可用</span>
            </div>
            <div class="flex items-center gap-3">
              <span class="text-xs text-gray-500">预期得分 <span class="font-semibold text-gray-800">{{ aiResult.expectedScore }}</span> / 10</span>
              <button
                :disabled="aiLoading"
                class="px-3 py-1 text-xs text-purple-600 border border-purple-300 rounded-lg hover:bg-purple-100 disabled:opacity-50 transition"
                @click="onRunBestThreeAi"
              >
                {{ aiLoading ? '分析中…' : '重新分析' }}
              </button>
            </div>
          </div>
          <div v-if="aiResult.explanation" class="px-5 py-2 text-sm text-purple-700 border-b border-purple-100 bg-purple-50/50">
            {{ aiResult.explanation }}
          </div>
          <div class="divide-y divide-gray-50">
            <div
              v-for="line in aiResult.lineAnalysis"
              :key="line.position"
              class="grid grid-cols-[auto_1fr_auto_1fr] items-center gap-x-3 px-5 py-3"
            >
              <span class="w-8 text-xs font-bold text-green-600">{{ line.position }}</span>
              <div class="text-sm text-gray-800 min-w-0">
                {{ pairText(aiResult.lineup, line.position) }}
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
                {{ pairText(aiResult.opponentLineup, line.position) }}
                <span class="text-xs text-gray-400 ml-1">{{ dualUtrLabel(line.opponentCombinedUtr, line.opponentCombinedActualUtr) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

    </div>

    <!-- 逐线对比 results -->
    <div v-if="analysisMode === 'headToHead' && headToHeadResult" class="space-y-4">

      <!-- UTR comparison card -->
      <div class="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
        <div class="flex items-center justify-between px-5 py-3 border-b border-gray-100 bg-gray-50">
          <span class="text-sm font-semibold text-gray-700">UTR 比较分析</span>
          <div class="flex items-center gap-3">
            <span class="text-xs text-gray-500">预期得分 <span class="font-semibold text-gray-800">{{ headToHeadResult.expectedScore }}</span> / 10
              <span class="text-gray-400 ml-1">(对手 {{ headToHeadResult.opponentExpectedScore }})</span>
            </span>
            <span class="px-3 py-1 rounded-full text-xs font-semibold" :class="verdictClass(headToHeadResult.verdict)">
              {{ headToHeadResult.verdict }}
            </span>
          </div>
        </div>

        <div class="divide-y divide-gray-50">
          <div
            v-for="line in headToHeadResult.lineAnalysis"
            :key="line.position"
            class="grid grid-cols-[auto_1fr_auto_1fr] items-center gap-x-3 px-5 py-3"
          >
            <span class="w-8 text-xs font-bold text-green-600">{{ line.position }}</span>
            <div class="text-sm text-gray-800 min-w-0">
              {{ pairText(headToHeadResult.lineup, line.position) }}
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
              {{ pairText(opponentLineupObj, line.position) }}
              <span class="text-xs text-gray-400 ml-1">{{ dualUtrLabel(line.opponentCombinedUtr, line.opponentCombinedActualUtr) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- AI 逐线评析 card -->
      <div class="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
        <!-- Not triggered yet -->
        <div v-if="!commentaryResult" class="px-5 py-4 flex items-center justify-between">
          <span class="text-sm font-semibold text-gray-700">AI 逐线评析</span>
          <div class="flex items-center gap-3">
            <span v-if="commentaryError" class="text-xs text-red-500">{{ commentaryError }}</span>
            <button
              :disabled="commentaryLoading"
              class="px-4 py-2 bg-purple-600 text-white text-sm font-medium rounded-lg hover:bg-purple-700 disabled:opacity-50 disabled:cursor-not-allowed transition"
              @click="onRunCommentary"
            >
              {{ commentaryLoading ? 'AI 分析中…' : 'AI 逐线评析' }}
            </button>
          </div>
        </div>

        <!-- Commentary result -->
        <template v-else>
          <div class="flex items-center justify-between px-5 py-3 border-b border-purple-100 bg-purple-50">
            <div class="flex items-center gap-2">
              <span class="text-sm font-semibold text-purple-800">AI 逐线评析</span>
              <span v-if="!commentaryResult.aiUsed" class="px-2 py-0.5 bg-yellow-100 text-yellow-700 text-xs rounded-full">AI 不可用</span>
            </div>
            <button
              :disabled="commentaryLoading"
              class="px-3 py-1 text-xs text-purple-600 border border-purple-300 rounded-lg hover:bg-purple-100 disabled:opacity-50 transition"
              @click="onRunCommentary"
            >
              {{ commentaryLoading ? '分析中…' : '重新分析' }}
            </button>
          </div>
          <div class="divide-y divide-gray-50">
            <div
              v-for="line in commentaryResult.lines"
              :key="line.position"
              class="px-5 py-3 flex items-start gap-3"
            >
              <span class="w-8 text-xs font-bold text-green-600 shrink-0">{{ line.position }}</span>
              <span class="text-sm text-gray-700">{{ line.commentary }}</span>
            </div>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useTeams } from '../composables/useTeams'
import { useOpponentMatchup } from '../composables/useOpponentMatchup'
import { useApi } from '../composables/useApi'
import PartnerNotesEditor from '../components/PartnerNotesEditor.vue'

const { teams, fetchTeams } = useTeams()
const { loading, error, runBestThree, runHeadToHead, runAiAnalysis, runCommentary } = useOpponentMatchup()
const { get } = useApi()

const analysisMode = ref('bestThree')
const ownTeamId = ref('')
const opponentTeamId = ref('')
const opponentLineupId = ref('')
const ownLineupId = ref('')
const opponentLineups = ref([])
const ownLineups = ref([])
const opponentPlayers = ref([])
const showOppNotes = ref(false)
const aiLoading = ref(false)
const aiError = ref('')
const commentaryLoading = ref(false)
const commentaryError = ref('')

const bestThreeResults = ref([])
const headToHeadResult = ref(null)
const aiResult = ref(null)
const commentaryResult = ref(null)

const canRunBestThree = computed(() => ownTeamId.value && opponentLineupId.value)
const canRunHeadToHead = computed(() => ownTeamId.value && ownLineupId.value && opponentLineupId.value)
const opponentLineupObj = computed(() => opponentLineups.value.find(l => l.id === opponentLineupId.value) || null)
const ownLineupObj = computed(() => ownLineups.value.find(l => l.id === ownLineupId.value) || null)
const opponentLineupPreviewPairs = computed(() => opponentLineupObj.value?.pairs || [])
const ownLineupPreviewPairs = computed(() => ownLineupObj.value?.pairs || [])

onMounted(() => {
  fetchTeams()
})

function onModeChange(newMode) {
  analysisMode.value = newMode
  bestThreeResults.value = []
  headToHeadResult.value = null
  aiResult.value = null
  aiError.value = ''
  commentaryResult.value = null
  commentaryError.value = ''
}

async function onOwnTeamChange() {
  ownLineups.value = []
  ownLineupId.value = ''
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
  opponentPlayers.value = []
  showOppNotes.value = false
  if (!opponentTeamId.value) return
  try {
    const [lineups, players] = await Promise.all([
      get(`/api/teams/${opponentTeamId.value}/lineups`),
      get(`/api/teams/${opponentTeamId.value}/players`),
    ])
    opponentLineups.value = lineups
    opponentPlayers.value = players
  } catch (err) {
    console.error('Failed to fetch opponent data:', err)
  }
}

async function onRunBestThree() {
  bestThreeResults.value = []
  try {
    bestThreeResults.value = await runBestThree(ownTeamId.value, opponentTeamId.value, opponentLineupId.value)
  } catch (_) {
    // error ref already set in composable
  }
}

async function onRunHeadToHead() {
  headToHeadResult.value = null
  commentaryResult.value = null
  commentaryError.value = ''
  try {
    headToHeadResult.value = await runHeadToHead(ownTeamId.value, ownLineupId.value, opponentTeamId.value, opponentLineupId.value)
  } catch (_) {
    // error ref already set in composable
  }
}

async function onRunBestThreeAi() {
  aiError.value = ''
  aiLoading.value = true
  try {
    aiResult.value = await runAiAnalysis(ownTeamId.value, opponentTeamId.value, opponentLineupId.value)
  } catch (err) {
    aiError.value = err.message || 'AI 分析失败，请重试'
  } finally {
    aiLoading.value = false
  }
}

async function onRunCommentary() {
  commentaryError.value = ''
  commentaryLoading.value = true
  try {
    commentaryResult.value = await runCommentary(ownTeamId.value, ownLineupId.value, opponentTeamId.value, opponentLineupId.value)
  } catch (err) {
    commentaryError.value = err.message || 'AI 评析失败，请重试'
  } finally {
    commentaryLoading.value = false
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

function pairPreviewText(pair) {
  const p1 = playerUtrLabel(pair.player1Utr, pair.player1ActualUtr)
  const p2 = playerUtrLabel(pair.player2Utr, pair.player2ActualUtr)
  return `${pair.player1Name}(${p1}) + ${pair.player2Name}(${p2})`
}

function dualUtrLabel(regularUtr, actualUtr) {
  if (regularUtr == null && actualUtr == null) return ''
  const base = actualUtr != null ? actualUtr : regularUtr
  if (regularUtr != null && actualUtr != null && Math.abs(actualUtr - regularUtr) >= 0.05) {
    return `(${regularUtr.toFixed(1)}/实${actualUtr.toFixed(1)})`
  }
  return `(${base.toFixed(1)})`
}

function verdictClass(verdict) {
  if (verdict === '能赢') return 'bg-green-100 text-green-700'
  if (verdict === '势均力敌') return 'bg-yellow-100 text-yellow-700'
  return 'bg-red-100 text-red-600'
}
</script>
