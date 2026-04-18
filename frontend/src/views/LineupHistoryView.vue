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
      <div v-for="lineup in lineups" :key="lineup.id" class="relative">
        <LineupCard :lineup="lineup" :show-player-utr="true" />

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

        <div class="mt-2 flex items-center justify-between px-1">
          <span class="text-xs text-gray-400">{{ formatDate(lineup.createdAt) }}</span>
          <button
            @click="handleDelete(lineup.id)"
            class="text-xs text-red-500 hover:text-red-700 px-2 py-1 rounded hover:bg-red-50 transition-colors"
          >
            删除
          </button>
        </div>
      </div>
    </div>

    <div v-if="deleteError" class="mt-4 bg-red-50 border border-red-200 rounded-md p-3 text-sm text-red-700">
      {{ deleteError }}
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import LineupCard from '../components/LineupCard.vue'
import { useLineupHistory } from '../composables/useLineupHistory'
import { useTeams } from '../composables/useTeams'

const route = useRoute()
const teamId = route.params.id

const { loading, lineups, fetchLineups, deleteLineup, exportLineups, importLineups } = useLineupHistory()
const { teams, fetchTeams } = useTeams()
const deleteError = ref(null)
const importing = ref(false)
const importResult = ref(null)

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
</script>
