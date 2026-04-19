<template>
  <div class="p-6">
    <div class="flex items-center justify-between mb-6">
      <h2 class="text-2xl font-bold text-gray-900">已保存排阵</h2>
      <div class="flex items-center gap-2">
        <button
          @click="handleExport"
          class="px-3 py-1.5 text-xs rounded-lg border border-green-300 text-green-700 hover:bg-green-50 transition-colors"
        >导出排阵</button>
        <button
          @click="$refs.importInput.click()"
          :disabled="importing"
          class="px-3 py-1.5 text-xs rounded-lg border border-blue-300 text-blue-600 hover:bg-blue-50 transition-colors disabled:opacity-50"
        >{{ importing ? '导入中...' : '导入排阵' }}</button>
        <input ref="importInput" type="file" accept=".json" class="hidden" @change="handleImport" />
      </div>
    </div>

    <div v-if="importResult" data-testid="import-result" class="mb-4 px-3 py-2 rounded-lg text-sm" :class="importResult.error ? 'bg-red-50 text-red-700' : 'bg-green-50 text-green-700'">
      {{ importResult.message }}
    </div>

    <div v-if="loading" class="flex justify-center items-center h-40">
      <div class="animate-spin rounded-full h-10 w-10 border-b-2 border-gray-900"></div>
    </div>

    <div v-else-if="lineups.length === 0" class="text-center py-16 text-gray-400 text-lg">
      暂无保存的排阵
    </div>

    <div v-else class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <div v-for="(lineup, index) in lineups" :key="lineup.id" class="relative">
        <LineupCard :lineup="lineup" :show-player-utr="true" />

        <!-- Label row -->
        <div class="mt-1 px-1 flex items-center gap-1">
          <span
            v-if="index === 0"
            data-testid="preferred-badge"
            class="inline-block px-2 py-0.5 text-xs font-medium rounded-full bg-yellow-100 text-yellow-700 mr-1"
          >⭐ 首选</span>

          <template v-if="editingLabelId === lineup.id">
            <input
              data-testid="label-input"
              :value="labelInputValue"
              @input="labelInputValue = $event.target.value"
              @blur="handleLabelBlur(lineup)"
              @keydown.enter="handleLabelSave(lineup)"
              @keydown.escape="cancelLabelEdit"
              class="text-sm border border-gray-300 rounded px-1 py-0.5 w-40 focus:outline-none focus:border-blue-400"
              autofocus
            />
          </template>
          <template v-else>
            <span
              data-testid="lineup-label"
              @click="startLabelEdit(lineup)"
              class="text-sm font-medium text-gray-700 cursor-pointer hover:text-blue-600"
            >{{ lineup.label || lineup.strategy }}</span>
            <button
              data-testid="label-edit-btn"
              @click="startLabelEdit(lineup)"
              class="text-xs text-gray-400 hover:text-gray-600 px-1"
            >✏️</button>
          </template>
        </div>

        <!-- Validity badge -->
        <div class="mt-1 px-1">
          <span
            v-if="lineup.currentValid !== false"
            class="inline-block px-2 py-0.5 text-xs font-medium rounded-full bg-green-100 text-green-700"
          >
            合法
          </span>
          <span
            v-else
            class="inline-block px-2 py-0.5 text-xs font-medium rounded-full bg-red-100 text-red-600"
          >
            已失效
          </span>
          <ul
            v-if="lineup.currentValid === false && lineup.currentViolations && lineup.currentViolations.length"
            class="mt-1 text-xs text-red-600 list-disc list-inside space-y-0.5"
          >
            <li v-for="(v, i) in lineup.currentViolations" :key="i">{{ v }}</li>
          </ul>
        </div>

        <!-- Comment row -->
        <div class="mt-1 px-1">
          <template v-if="editingCommentId === lineup.id">
            <textarea
              data-testid="comment-input"
              :value="commentInputValue"
              @input="commentInputValue = $event.target.value"
              @blur="handleCommentBlur(lineup)"
              @keydown.escape="cancelCommentEdit"
              rows="2"
              class="w-full text-sm border border-gray-300 rounded px-1 py-0.5 focus:outline-none focus:border-blue-400 resize-none"
            ></textarea>
          </template>
          <template v-else>
            <span
              v-if="lineup.comment"
              data-testid="lineup-comment"
              @click="startCommentEdit(lineup)"
              class="text-sm text-gray-500 cursor-pointer hover:text-blue-600"
            >{{ lineup.comment }}</span>
            <button
              v-else
              data-testid="add-comment-btn"
              @click="startCommentEdit(lineup)"
              class="text-xs text-gray-400 hover:text-blue-500"
            >+ 添加备注</button>
          </template>
        </div>

        <!-- Task 6: Swap panel (collapsible) -->
        <details class="mt-2">
          <summary class="text-xs text-gray-400 cursor-pointer hover:text-gray-600 py-1">调整配对</summary>
          <LineupSwapPanel :key="lineup.id + '-' + swapPanelVersion" :lineup="lineup" @update:lineup="handleSwapUpdate(lineup, $event)" />
        </details>

        <!-- Task 7: Player replacement entry button -->
        <div class="mt-2 px-1">
          <button
            data-testid="start-replace-btn"
            @click="startReplace(lineup)"
            class="text-xs text-blue-500 hover:text-blue-700 border border-blue-300 rounded px-2 py-0.5 hover:bg-blue-50 transition-colors"
          >替换球员</button>
        </div>

        <!-- Task 7: Player replacement UI -->
        <div v-if="replacingLineupId === lineup.id" class="mt-2 px-1 border border-gray-200 rounded-lg p-3 bg-gray-50">
          <div class="text-xs text-gray-600 font-medium mb-2">替换球员配置</div>

          <!-- Pairs editor -->
          <div class="space-y-2">
            <div
              v-for="(pair, pairIdx) in replacingPairs"
              :key="pair.position"
              class="flex items-center gap-2"
            >
              <span class="w-7 text-xs font-bold text-green-600">{{ pair.position }}</span>
              <!-- Player 1 slot -->
              <select
                :value="pair.player1Id"
                @change="onReplacePlayerChange(pairIdx, 'player1Id', $event.target.value)"
                class="text-xs border border-gray-300 rounded px-1 py-0.5 flex-1"
              >
                <option
                  v-for="player in getAvailablePlayers(pairIdx, 'player1Id')"
                  :key="player.id"
                  :value="player.id"
                >{{ player.name }} (UTR: {{ player.utr }})</option>
              </select>
              <!-- Player 2 slot -->
              <select
                :value="pair.player2Id"
                @change="onReplacePlayerChange(pairIdx, 'player2Id', $event.target.value)"
                class="text-xs border border-gray-300 rounded px-1 py-0.5 flex-1"
              >
                <option
                  v-for="player in getAvailablePlayers(pairIdx, 'player2Id')"
                  :key="player.id"
                  :value="player.id"
                >{{ player.name }} (UTR: {{ player.utr }})</option>
              </select>
              <!-- Combined UTR display -->
              <span class="text-xs text-gray-400 w-16">
                UTR: {{ calcCombinedUtr(pair) }}
              </span>
            </div>
          </div>

          <!-- Constraint violations -->
          <ul v-if="replaceViolations.length" class="mt-2 space-y-0.5">
            <li
              v-for="(v, i) in replaceViolations"
              :key="i"
              data-testid="replace-violation"
              class="text-xs text-red-600"
            >{{ v }}</li>
          </ul>

          <!-- Action buttons -->
          <div class="mt-3 flex items-center gap-2">
            <button
              data-testid="save-replace-btn"
              @click="saveReplace(lineup)"
              class="text-xs px-3 py-1 bg-blue-600 text-white rounded hover:bg-blue-700 transition-colors"
            >保存修改</button>
            <button
              data-testid="cancel-replace-btn"
              @click="cancelReplace"
              class="text-xs px-3 py-1 bg-gray-200 text-gray-600 rounded hover:bg-gray-300 transition-colors"
            >取消</button>
          </div>
        </div>

        <!-- Bottom row: date, reorder buttons, delete -->
        <div class="mt-2 flex items-center justify-between px-1">
          <span class="text-xs text-gray-400">{{ formatDate(lineup.createdAt) }}</span>
          <div class="flex items-center gap-1">
            <button
              data-testid="move-up-btn"
              @click="handleMoveUp(index)"
              :disabled="index === 0"
              class="text-xs text-gray-400 hover:text-gray-600 px-1 py-0.5 rounded hover:bg-gray-100 transition-colors disabled:opacity-30 disabled:cursor-not-allowed"
            >↑</button>
            <button
              data-testid="move-down-btn"
              @click="handleMoveDown(index)"
              :disabled="index === lineups.length - 1"
              class="text-xs text-gray-400 hover:text-gray-600 px-1 py-0.5 rounded hover:bg-gray-100 transition-colors disabled:opacity-30 disabled:cursor-not-allowed"
            >↓</button>
            <button
              @click="handleDelete(lineup.id)"
              class="text-xs text-red-500 hover:text-red-700 px-2 py-1 rounded hover:bg-red-50 transition-colors"
            >
              删除
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="deleteError" class="mt-4 bg-red-50 border border-red-200 rounded-md p-3 text-sm text-red-700">
      {{ deleteError }}
    </div>
    <div v-if="updateError" data-testid="update-error" class="mt-4 bg-red-50 border border-red-200 rounded-md p-3 text-sm text-red-700">
      {{ updateError }}
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import LineupCard from '../components/LineupCard.vue'
import LineupSwapPanel from '../components/LineupSwapPanel.vue'
import { useLineupHistory } from '../composables/useLineupHistory'
import { useTeams } from '../composables/useTeams'

const route = useRoute()
const teamId = route.params.id

const { loading, lineups, fetchLineups, deleteLineup, exportLineups, importLineups, updateLineup } = useLineupHistory()
const { teams, fetchTeams } = useTeams()
const deleteError = ref(null)
const updateError = ref(null)
const importing = ref(false)
const importResult = ref(null)

// Label edit state
const editingLabelId = ref(null)
const labelInputValue = ref('')
const labelSaving = ref(false)

// Comment edit state
const editingCommentId = ref(null)
const commentInputValue = ref('')

// Task 7: Player replacement state
const replacingLineupId = ref(null)
const replacingPairs = ref([])
const replaceViolations = ref([])
const swapPanelVersion = ref(0)

onMounted(async () => {
  await Promise.all([fetchLineups(teamId), fetchTeams()])
})

const currentTeam = computed(() => teams.value.find(t => t.id === teamId))

function handleExport() {
  const teamName = currentTeam.value?.name || teamId
  exportLineups(teamId, teamName)
}

async function handleImport(event) {
  const file = event.target.files[0]
  if (!file) return
  importing.value = true
  importResult.value = null
  try {
    const result = await importLineups(teamId, file)
    importResult.value = { message: `导入成功：${result.imported} 条，跳过：${result.skipped} 条`, error: false }
    await fetchLineups(teamId)
  } catch (err) {
    importResult.value = { message: err.message || '导入失败，请重试', error: true }
  } finally {
    importing.value = false
    event.target.value = ''
  }
}

async function handleDelete(lineupId) {
  if (!confirm('确定要删除这个排阵吗？此操作不可撤销。')) return
  deleteError.value = null
  try {
    await deleteLineup(lineupId)
  } catch (err) {
    deleteError.value = err.message || '删除失败，请重试'
  }
}

function formatDate(iso) {
  if (!iso) return ''
  return new Date(iso).toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit',
  })
}

// ── Label editing ─────────────────────────────────────────────────────────────

function startLabelEdit(lineup) {
  editingLabelId.value = lineup.id
  labelInputValue.value = lineup.label || ''
}

async function handleLabelSave(lineup) {
  if (labelSaving.value) return // guard against Enter+blur double-fire
  labelSaving.value = true
  updateError.value = null
  try {
    await updateLineup(teamId, lineup.id, { label: labelInputValue.value })
    editingLabelId.value = null
    await fetchLineups(teamId)
  } catch (err) {
    updateError.value = err.message || '保存标签失败，请重试'
  } finally {
    labelSaving.value = false
  }
}

async function handleLabelBlur(lineup) {
  if (editingLabelId.value !== lineup.id) return
  await handleLabelSave(lineup)
}

function cancelLabelEdit() {
  editingLabelId.value = null
}

// ── Comment editing ───────────────────────────────────────────────────────────

function startCommentEdit(lineup) {
  editingCommentId.value = lineup.id
  commentInputValue.value = lineup.comment || ''
}

async function handleCommentBlur(lineup) {
  if (editingCommentId.value !== lineup.id) return
  updateError.value = null
  try {
    await updateLineup(teamId, lineup.id, { comment: commentInputValue.value })
    editingCommentId.value = null
    await fetchLineups(teamId)
  } catch (err) {
    updateError.value = err.message || '保存备注失败，请重试'
  }
}

function cancelCommentEdit() {
  editingCommentId.value = null
}

// ── Reorder ───────────────────────────────────────────────────────────────────

async function handleMoveUp(index) {
  if (index === 0) return
  const current = lineups.value[index]
  const previous = lineups.value[index - 1]
  updateError.value = null
  try {
    await Promise.all([
      updateLineup(teamId, current.id, { sortOrder: previous.sortOrder }),
      updateLineup(teamId, previous.id, { sortOrder: current.sortOrder }),
    ])
    await fetchLineups(teamId)
  } catch (err) {
    updateError.value = err.message || '排序更新失败，请重试'
    await fetchLineups(teamId)
  }
}

async function handleMoveDown(index) {
  if (index === lineups.value.length - 1) return
  const current = lineups.value[index]
  const next = lineups.value[index + 1]
  updateError.value = null
  try {
    await Promise.all([
      updateLineup(teamId, current.id, { sortOrder: next.sortOrder }),
      updateLineup(teamId, next.id, { sortOrder: current.sortOrder }),
    ])
    await fetchLineups(teamId)
  } catch (err) {
    updateError.value = err.message || '排序更新失败，请重试'
    await fetchLineups(teamId)
  }
}

// ── Task 6: Swap panel ────────────────────────────────────────────────────────

async function handleSwapUpdate(lineup, updatedLineup) {
  updateError.value = null
  try {
    await updateLineup(teamId, lineup.id, { pairs: updatedLineup.pairs })
  } catch (err) {
    updateError.value = err.message || '互换保存失败，请重试'
  } finally {
    swapPanelVersion.value += 1
    await fetchLineups(teamId)
  }
}

// ── Task 7: Player replacement ────────────────────────────────────────────────

function startReplace(lineup) {
  replacingLineupId.value = lineup.id
  replacingPairs.value = JSON.parse(JSON.stringify(lineup.pairs ?? []))
  replaceViolations.value = validateReplacePairs(replacingPairs.value, currentTeam.value?.players)
}

function cancelReplace() {
  replacingLineupId.value = null
  replacingPairs.value = []
  replaceViolations.value = []
}

function getAvailablePlayers(pairIdx, slot) {
  const players = currentTeam.value?.players ?? []
  // Collect all used player IDs except the current slot
  const usedIds = new Set()
  replacingPairs.value.forEach((pair, idx) => {
    if (idx === pairIdx) {
      // exclude only the OTHER slot in same pair
      const otherSlot = slot === 'player1Id' ? 'player2Id' : 'player1Id'
      if (pair[otherSlot]) usedIds.add(pair[otherSlot])
    } else {
      if (pair.player1Id) usedIds.add(pair.player1Id)
      if (pair.player2Id) usedIds.add(pair.player2Id)
    }
  })
  const available = players.filter(p => !usedIds.has(p.id))
  // Preserve a ghost entry for the current slot value if the player is no longer on the team
  const currentPair = replacingPairs.value[pairIdx]
  const currentId = currentPair?.[slot]
  if (currentId && !available.some(p => p.id === currentId)) {
    available.unshift({ id: currentId, name: '(已移除球员)', utr: 0 })
  }
  return available
}

function calcCombinedUtr(pair) {
  const players = currentTeam.value?.players ?? []
  const playerMap = Object.fromEntries(players.map(p => [p.id, p]))
  const utr1 = playerMap[pair.player1Id]?.utr ?? 0
  const utr2 = playerMap[pair.player2Id]?.utr ?? 0
  return (utr1 + utr2).toFixed(1)
}

function onReplacePlayerChange(pairIdx, slot, playerId) {
  // Update the changed slot
  const updated = replacingPairs.value.map((pair, idx) => {
    if (idx !== pairIdx) return pair
    return { ...pair, [slot]: playerId }
  })

  // Auto-reorder D1-D4 by combinedUtr descending (same as lineup generator)
  const players = currentTeam.value?.players ?? []
  const playerMap = Object.fromEntries(players.map(p => [p.id, p]))
  const positions = ['D1', 'D2', 'D3', 'D4']
  const reordered = updated
    .map(pair => ({
      ...pair,
      _combined: (playerMap[pair.player1Id]?.utr ?? 0) + (playerMap[pair.player2Id]?.utr ?? 0),
    }))
    .sort((a, b) => b._combined - a._combined)
    .map((pair, i) => {
      const { _combined, ...rest } = pair
      return { ...rest, position: positions[i] ?? pair.position }
    })

  replacingPairs.value = reordered
  replaceViolations.value = validateReplacePairs(reordered, players)
}

function validateReplacePairs(pairs, players) {
  const violations = []
  const playerMap = Object.fromEntries((players ?? []).map(p => [p.id, p]))

  const enriched = pairs.map(pair => ({
    ...pair,
    p1: playerMap[pair.player1Id],
    p2: playerMap[pair.player2Id],
    combined: (playerMap[pair.player1Id]?.utr ?? 0) + (playerMap[pair.player2Id]?.utr ?? 0),
  }))

  // Hard constraint: total UTR ≤ 40.5
  const totalUtr = enriched.reduce((sum, e) => sum + e.combined, 0)
  if (totalUtr > 40.5) violations.push(`总UTR超出上限（当前: ${totalUtr.toFixed(2)}）`)

  // Hard constraint: partner gap ≤ 3.5
  enriched.forEach(e => {
    const gap = Math.abs((e.p1?.utr ?? 0) - (e.p2?.utr ?? 0))
    if (gap > 3.5) violations.push(`${e.position} 搭档UTR差值超过3.5（${gap.toFixed(2)}）`)
  })

  // Hard constraint: at least 2 female players
  const femaleCount = enriched.reduce((count, e) => {
    if (e.p1?.gender === 'female') count++
    if (e.p2?.gender === 'female') count++
    return count
  }, 0)
  if (femaleCount < 2) violations.push(`至少需要2名女球员上阵（当前: ${femaleCount}名）`)

  // Hard constraint: D4 both players must have verified doubles UTR
  const d4 = enriched.find(e => e.position === 'D4')
  if (d4) {
    if (d4.p1 && !d4.p1.verified) violations.push(`D4 ${d4.p1.name} 缺少Verified Doubles UTR`)
    if (d4.p2 && !d4.p2.verified) violations.push(`D4 ${d4.p2.name} 缺少Verified Doubles UTR`)
  }

  // Position order is auto-fixed by reordering — no violation needed

  return violations
}

async function saveReplace(lineup) {
  if (replaceViolations.value.length > 0) return
  updateError.value = null
  try {
    await updateLineup(teamId, lineup.id, { pairs: replacingPairs.value })
    replacingLineupId.value = null
    replacingPairs.value = []
    replaceViolations.value = []
    await fetchLineups(teamId)
  } catch (err) {
    updateError.value = err.message || '保存替换失败，请重试'
  }
}
</script>
