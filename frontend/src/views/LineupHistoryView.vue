<template>
  <div class="p-6">
    <!-- Page header: title + export/import -->
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
      <div v-for="(lineup, index) in lineups" :key="lineup.id">

        <!-- ── VIEW CARD ──────────────────────────────────────── -->
        <div>
          <!-- Lineup card (includes name/comment in header) -->
          <LineupCard
            :lineup="lineup"
            :show-player-utr="true"
            :label="lineup.label || lineup.strategy"
            :comment="lineup.comment"
            :preferred="index === 0"
          />

          <!-- Validity + violations -->
          <div v-if="lineup.currentValid === false && lineup.currentViolations?.length" class="mt-1 px-1">
            <ul class="text-xs text-red-600 list-disc list-inside space-y-0.5">
              <li v-for="(v, i) in lineup.currentViolations" :key="i">{{ v }}</li>
            </ul>
          </div>

          <!-- Bottom bar: validity badge · date · reorder · edit · delete -->
          <div class="mt-1 px-1 flex items-center justify-between flex-wrap gap-2">
            <div class="flex items-center gap-2">
              <span
                v-if="lineup.currentValid !== false"
                class="px-2 py-0.5 text-xs font-medium rounded-full bg-green-100 text-green-700"
              >合法</span>
              <span
                v-else
                class="px-2 py-0.5 text-xs font-medium rounded-full bg-red-100 text-red-600"
              >已失效</span>
              <span class="text-xs text-gray-400">{{ formatDate(lineup.createdAt) }}</span>
            </div>
            <div class="flex items-center gap-1">
              <button
                data-testid="move-up-btn"
                @click="handleMoveUp(index)"
                :disabled="index === 0"
                class="text-xs text-gray-400 hover:text-gray-600 px-1 py-0.5 rounded hover:bg-gray-100 disabled:opacity-30 disabled:cursor-not-allowed"
              >↑</button>
              <button
                data-testid="move-down-btn"
                @click="handleMoveDown(index)"
                :disabled="index === lineups.length - 1"
                class="text-xs text-gray-400 hover:text-gray-600 px-1 py-0.5 rounded hover:bg-gray-100 disabled:opacity-30 disabled:cursor-not-allowed"
              >↓</button>
              <button
                data-testid="edit-btn"
                @click="startEdit(lineup)"
                class="text-xs text-blue-600 hover:text-blue-800 px-2 py-0.5 rounded border border-blue-200 hover:bg-blue-50 transition-colors ml-1"
              >编辑</button>
              <button
                @click="handleDelete(lineup.id)"
                class="text-xs text-red-500 hover:text-red-700 px-2 py-0.5 rounded hover:bg-red-50 transition-colors"
              >删除</button>
            </div>
          </div>
        </div>

        <!-- ── EDIT PANEL (expands below card when editing) ──── -->
        <div
          v-if="editingLineupId === lineup.id"
          data-testid="edit-panel"
          class="mt-2 bg-gray-50 rounded-xl border border-blue-200 shadow-sm overflow-hidden"
        >
          <!-- Edit panel header -->
          <div class="px-4 py-3 bg-blue-50 border-b border-blue-200 flex items-center justify-between">
            <span class="text-sm font-medium text-blue-800">✏️ 编辑排阵</span>
            <div class="flex items-center gap-2">
              <button
                data-testid="save-edit-btn"
                @click="saveEdit(lineup)"
                :disabled="editSaving"
                class="text-xs px-3 py-1 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50 transition-colors"
              >{{ editSaving ? '保存中...' : '保存' }}</button>
              <button
                data-testid="cancel-edit-btn"
                @click="cancelEdit"
                class="text-xs px-3 py-1 bg-white text-gray-600 rounded border border-gray-300 hover:bg-gray-100 transition-colors"
              >取消</button>
            </div>
          </div>

          <!-- Name + Comment -->
          <div class="px-4 py-3 space-y-3 border-b border-gray-200">
            <div>
              <label class="block text-xs text-gray-500 mb-1">排阵名称</label>
              <input
                data-testid="edit-label-input"
                v-model="editLabel"
                type="text"
                placeholder="默认显示策略名"
                class="w-full text-sm border border-gray-300 rounded px-2 py-1 focus:outline-none focus:border-blue-400"
              />
            </div>
            <div>
              <label class="block text-xs text-gray-500 mb-1">备注</label>
              <textarea
                data-testid="edit-comment-input"
                v-model="editComment"
                rows="2"
                placeholder="添加备注..."
                class="w-full text-sm border border-gray-300 rounded px-2 py-1 focus:outline-none focus:border-blue-400 resize-none"
              ></textarea>
            </div>
          </div>

          <!-- Swap section -->
          <div class="px-4 py-3 border-b border-gray-200">
            <div class="text-xs font-medium text-gray-600 mb-2">调整配对</div>
            <LineupSwapPanel
              :key="lineup.id + '-' + swapPanelVersion"
              :lineup="lineup"
              @update:lineup="handleSwapUpdate(lineup, $event)"
            />
          </div>

          <!-- Replace players section -->
          <div class="px-4 py-3">
            <div class="text-xs font-medium text-gray-600 mb-2">替换球员</div>

            <template v-if="replacingLineupId !== lineup.id">
              <button
                data-testid="start-replace-btn"
                @click="startReplace(lineup)"
                class="text-xs text-blue-500 hover:text-blue-700 border border-blue-300 rounded px-2 py-0.5 hover:bg-blue-50 transition-colors"
              >选择替换</button>
            </template>

            <template v-else>
              <!-- Pairs selects -->
              <div class="space-y-2">
                <div
                  v-for="(pair, pairIdx) in replacingPairs"
                  :key="pair.position"
                  class="flex items-center gap-2"
                >
                  <span class="w-7 text-xs font-bold text-green-600">{{ pair.position }}</span>
                  <select
                    :value="pair.player1Id"
                    @change="onReplacePlayerChange(pairIdx, 'player1Id', $event.target.value)"
                    class="text-xs border border-gray-300 rounded px-1 py-0.5 flex-1"
                  >
                    <option v-for="p in getAvailablePlayers(pairIdx, 'player1Id')" :key="p.id" :value="p.id">
                      {{ p.name }} ({{ p.utr }})
                    </option>
                  </select>
                  <select
                    :value="pair.player2Id"
                    @change="onReplacePlayerChange(pairIdx, 'player2Id', $event.target.value)"
                    class="text-xs border border-gray-300 rounded px-1 py-0.5 flex-1"
                  >
                    <option v-for="p in getAvailablePlayers(pairIdx, 'player2Id')" :key="p.id" :value="p.id">
                      {{ p.name }} ({{ p.utr }})
                    </option>
                  </select>
                  <span class="text-xs text-gray-400 w-12 text-right">{{ calcCombinedUtr(pair) }}</span>
                </div>
              </div>

              <!-- Violations -->
              <ul v-if="replaceViolations.length" class="mt-2 space-y-0.5">
                <li
                  v-for="(v, i) in replaceViolations"
                  :key="i"
                  data-testid="replace-violation"
                  class="text-xs text-red-600"
                >⚠️ {{ v }}</li>
              </ul>

              <!-- Replace action buttons -->
              <div class="mt-3 flex items-center gap-2">
                <button
                  data-testid="save-replace-btn"
                  @click="saveReplace(lineup)"
                  :disabled="replaceViolations.length > 0"
                  class="text-xs px-3 py-1 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
                >保存替换</button>
                <button
                  data-testid="cancel-replace-btn"
                  @click="cancelReplace"
                  class="text-xs px-3 py-1 bg-white text-gray-600 rounded border border-gray-300 hover:bg-gray-100 transition-colors"
                >取消</button>
              </div>
            </template>
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

// Edit mode state
const editingLineupId = ref(null)
const editLabel = ref('')
const editComment = ref('')
const editSaving = ref(false)

// Swap state
const swapPanelVersion = ref(0)

// Replace state
const replacingLineupId = ref(null)
const replacingPairs = ref([])
const replaceViolations = ref([])

onMounted(async () => {
  await Promise.all([fetchLineups(teamId), fetchTeams()])
})

const currentTeam = computed(() => teams.value.find(t => t.id === teamId))

// ── Export / Import ───────────────────────────────────────────────────────────

function handleExport() {
  exportLineups(teamId, currentTeam.value?.name || teamId)
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

// ── Delete ────────────────────────────────────────────────────────────────────

async function handleDelete(lineupId) {
  if (!confirm('确定要删除这个排阵吗？此操作不可撤销。')) return
  deleteError.value = null
  try {
    await deleteLineup(lineupId)
  } catch (err) {
    deleteError.value = err.message || '删除失败，请重试'
  }
}

// ── Reorder ───────────────────────────────────────────────────────────────────

/**
 * Reassign sortOrder of every lineup to match its new display position.
 * Robust against existing duplicate/unset sortOrder values (all 0 by default).
 * Only PATCHes lineups whose sortOrder actually needs to change.
 */
async function applyNewOrder(newOrder) {
  updateError.value = null
  try {
    await Promise.all(
      newOrder.map((lineup, newIndex) =>
        lineup.sortOrder === newIndex
          ? Promise.resolve()
          : updateLineup(teamId, lineup.id, { sortOrder: newIndex })
      )
    )
  } catch (err) {
    updateError.value = err.message || '排序更新失败，请重试'
  }
  await fetchLineups(teamId)
}

async function handleMoveUp(index) {
  if (index === 0) return
  const newOrder = [...lineups.value]
  ;[newOrder[index - 1], newOrder[index]] = [newOrder[index], newOrder[index - 1]]
  await applyNewOrder(newOrder)
}

async function handleMoveDown(index) {
  if (index === lineups.value.length - 1) return
  const newOrder = [...lineups.value]
  ;[newOrder[index], newOrder[index + 1]] = [newOrder[index + 1], newOrder[index]]
  await applyNewOrder(newOrder)
}

// ── Edit mode ─────────────────────────────────────────────────────────────────

function startEdit(lineup) {
  editingLineupId.value = lineup.id
  editLabel.value = lineup.label || ''
  editComment.value = lineup.comment || ''
  // Close any open replace UI for other cards
  cancelReplace()
  updateError.value = null
}

async function saveEdit(lineup) {
  if (editSaving.value) return
  editSaving.value = true
  updateError.value = null
  try {
    await updateLineup(teamId, lineup.id, { label: editLabel.value, comment: editComment.value })
    editingLineupId.value = null
    await fetchLineups(teamId)
  } catch (err) {
    updateError.value = err.message || '保存失败，请重试'
  } finally {
    editSaving.value = false
  }
}

function cancelEdit() {
  editingLineupId.value = null
  cancelReplace()
}

// ── Swap ──────────────────────────────────────────────────────────────────────

async function handleSwapUpdate(lineup, updatedLineup) {
  updateError.value = null
  try {
    await updateLineup(teamId, lineup.id, { pairs: updatedLineup.pairs })
  } catch (err) {
    updateError.value = err.message || '互换保存失败，请重试'
  }
  // Refresh FIRST, then bump key so SwapPanel remounts with the fresh lineup
  await fetchLineups(teamId)
  swapPanelVersion.value += 1
}

// ── Replace players ───────────────────────────────────────────────────────────

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
  const usedIds = new Set()
  replacingPairs.value.forEach((pair, idx) => {
    if (idx === pairIdx) {
      const other = slot === 'player1Id' ? 'player2Id' : 'player1Id'
      if (pair[other]) usedIds.add(pair[other])
    } else {
      if (pair.player1Id) usedIds.add(pair.player1Id)
      if (pair.player2Id) usedIds.add(pair.player2Id)
    }
  })
  const available = players.filter(p => !usedIds.has(p.id))
  const currentId = replacingPairs.value[pairIdx]?.[slot]
  if (currentId && !available.some(p => p.id === currentId)) {
    available.unshift({ id: currentId, name: '(已移除球员)', utr: 0 })
  }
  return available
}

function calcCombinedUtr(pair) {
  const players = currentTeam.value?.players ?? []
  const pm = Object.fromEntries(players.map(p => [p.id, p]))
  return ((pm[pair.player1Id]?.utr ?? 0) + (pm[pair.player2Id]?.utr ?? 0)).toFixed(1)
}

function onReplacePlayerChange(pairIdx, slot, playerId) {
  const players = currentTeam.value?.players ?? []
  const pm = Object.fromEntries(players.map(p => [p.id, p]))
  const player = pm[playerId]

  // Determine which pair fields to update based on slot (player1 or player2)
  const prefix = slot === 'player1Id' ? 'player1' : 'player2'
  const playerFields = player ? {
    [slot]: playerId,
    [`${prefix}Name`]: player.name,
    [`${prefix}Utr`]: player.utr,
    [`${prefix}Gender`]: player.gender ?? null,
    [`${prefix}ActualUtr`]: player.actualUtr ?? null,
  } : { [slot]: playerId }

  const updated = replacingPairs.value.map((pair, idx) =>
    idx !== pairIdx ? pair : { ...pair, ...playerFields }
  )
  const positions = ['D1', 'D2', 'D3', 'D4']
  const reordered = updated
    .map(pair => ({ ...pair, _c: (pm[pair.player1Id]?.utr ?? 0) + (pm[pair.player2Id]?.utr ?? 0) }))
    .sort((a, b) => b._c - a._c)
    .map((pair, i) => { const { _c, ...rest } = pair; return { ...rest, position: positions[i] ?? pair.position } })
  replacingPairs.value = reordered
  replaceViolations.value = validateReplacePairs(reordered, players)
}

function validateReplacePairs(pairs, players) {
  const violations = []
  const pm = Object.fromEntries((players ?? []).map(p => [p.id, p]))
  const enriched = pairs.map(pair => ({
    ...pair,
    p1: pm[pair.player1Id],
    p2: pm[pair.player2Id],
    combined: (pm[pair.player1Id]?.utr ?? 0) + (pm[pair.player2Id]?.utr ?? 0),
  }))
  const totalUtr = enriched.reduce((s, e) => s + e.combined, 0)
  if (totalUtr > 40.5) violations.push(`总UTR超出上限（当前: ${totalUtr.toFixed(2)}）`)
  enriched.forEach(e => {
    const gap = Math.abs((e.p1?.utr ?? 0) - (e.p2?.utr ?? 0))
    if (gap > 3.5) violations.push(`${e.position} 搭档UTR差值超过3.5（${gap.toFixed(2)}）`)
  })
  const females = enriched.reduce((n, e) => n + (e.p1?.gender === 'female' ? 1 : 0) + (e.p2?.gender === 'female' ? 1 : 0), 0)
  if (females < 2) violations.push(`至少需要2名女球员上阵（当前: ${females}名）`)
  const d4 = enriched.find(e => e.position === 'D4')
  if (d4) {
    if (d4.p1 && !d4.p1.verified) violations.push(`D4 ${d4.p1.name} 缺少Verified Doubles UTR`)
    if (d4.p2 && !d4.p2.verified) violations.push(`D4 ${d4.p2.name} 缺少Verified Doubles UTR`)
  }
  return violations
}

async function saveReplace(lineup) {
  if (replaceViolations.value.length > 0) return
  updateError.value = null
  try {
    await updateLineup(teamId, lineup.id, { pairs: replacingPairs.value })
    cancelReplace()
    await fetchLineups(teamId)
    swapPanelVersion.value += 1  // remount SwapPanel AFTER fetch so it sees new pairs
  } catch (err) {
    updateError.value = err.message || '保存替换失败，请重试'
  }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

function formatDate(iso) {
  if (!iso) return ''
  return new Date(iso).toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit',
  })
}
</script>
