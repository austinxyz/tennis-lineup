<template>
  <div class="w-64 bg-white border-r border-gray-200 flex flex-col h-full">

    <!-- Header -->
    <div class="px-4 py-4 border-b border-gray-200">
      <div class="flex items-center justify-between mb-3">
        <h2 class="text-sm font-semibold text-gray-700">队伍列表</h2>
      </div>

      <!-- Actions -->
      <div class="flex gap-2">
        <button @click="showCreateModal = true"
          class="flex-1 flex items-center justify-center gap-1.5 px-3 py-1.5 bg-green-600 text-white text-sm rounded-lg hover:bg-green-700 transition-colors">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
          </svg>
          创建队伍
        </button>
        <button @click="showImportModal = true"
          class="flex items-center justify-center gap-1.5 px-3 py-1.5 bg-gray-100 text-gray-700 text-sm rounded-lg hover:bg-gray-200 transition-colors">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"/>
          </svg>
          导入
        </button>
      </div>
    </div>

    <!-- Team list -->
    <nav class="flex-1 overflow-y-auto py-2">
      <div v-if="loading" class="flex justify-center py-8">
        <div class="animate-spin rounded-full h-6 w-6 border-b-2 border-green-600"/>
      </div>

      <div v-else-if="teams.length === 0" class="px-5 py-8 text-center text-sm text-gray-400">
        暂无队伍
      </div>

      <router-link
        v-for="team in teams"
        :key="team.id"
        :to="`/teams/${team.id}`"
        @click="$emit('navigate')"
        class="flex items-center justify-between px-4 py-3 mx-2 rounded-lg transition-colors group"
        :class="activeTeamId === team.id
          ? 'bg-green-50 text-green-700'
          : 'text-gray-700 hover:bg-gray-100'">
        <div class="flex items-center gap-3 min-w-0">
          <div class="w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold flex-shrink-0"
            :class="activeTeamId === team.id ? 'bg-green-600 text-white' : 'bg-gray-200 text-gray-600'">
            {{ team.name?.charAt(0) }}
          </div>
          <div class="min-w-0">
            <div class="font-medium text-sm truncate">{{ team.name }}</div>
            <div class="text-xs text-gray-400">{{ team.players?.length || 0 }} 名球员</div>
          </div>
        </div>
        <button @click.prevent="confirmDelete(team)"
          class="opacity-0 group-hover:opacity-100 p-1 text-gray-400 hover:text-red-500 transition-all rounded flex-shrink-0">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
          </svg>
        </button>
      </router-link>
    </nav>

    <!-- Create Team Modal -->
    <div v-if="showCreateModal" class="fixed inset-0 bg-black/50 z-50 flex items-center justify-center" @click.self="showCreateModal = false">
      <div class="bg-white rounded-xl shadow-xl w-80 p-6">
        <h3 class="text-lg font-semibold text-gray-900 mb-4">创建新队伍</h3>
        <form @submit.prevent="handleCreate">
          <input
            v-model="newTeamName"
            type="text"
            placeholder="请输入队名"
            class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500 mb-4"
            autofocus
            required/>
          <div class="flex justify-end gap-2">
            <button type="button" @click="showCreateModal = false"
              class="px-4 py-2 text-sm bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200">取消</button>
            <button type="submit" :disabled="loading"
              class="px-4 py-2 text-sm bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50">创建</button>
          </div>
        </form>
      </div>
    </div>

    <!-- Import Modal -->
    <div v-if="showImportModal" class="fixed inset-0 bg-black/50 z-50 flex items-center justify-center" @click.self="showImportModal = false">
      <div class="bg-white rounded-xl shadow-xl w-96 p-6">
        <h3 class="text-lg font-semibold text-gray-900 mb-1">批量导入</h3>
        <p class="text-sm text-gray-500 mb-4">上传 CSV 或 JSON 文件批量导入球员数据</p>

        <div class="border-2 border-dashed border-gray-300 rounded-lg p-6 text-center hover:border-gray-400 transition-colors"
          @dragover.prevent @drop.prevent="handleDrop">
          <svg class="w-10 h-10 mx-auto text-gray-400 mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"/>
          </svg>
          <p class="text-sm text-gray-500 mb-2">拖拽文件至此，或</p>
          <label class="cursor-pointer">
            <span class="px-3 py-1.5 bg-gray-100 text-gray-700 text-sm rounded-lg hover:bg-gray-200">选择文件</span>
            <input type="file" accept=".csv,.json" class="hidden" @change="handleFileSelect"/>
          </label>
          <p class="text-xs text-gray-400 mt-2">支持 .csv 和 .json</p>
        </div>

        <div v-if="importFile" class="mt-3 px-3 py-2 bg-gray-50 rounded-lg flex items-center justify-between">
          <span class="text-sm text-gray-700 truncate">{{ importFile.name }}</span>
          <button @click="importFile = null" class="ml-2 text-gray-400 hover:text-gray-600 flex-shrink-0">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>

        <div v-if="importError" class="mt-3 p-3 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm">{{ importError }}</div>
        <div v-if="importResult" class="mt-3 p-3 bg-green-50 border border-green-200 rounded-lg text-green-700 text-sm">
          导入成功：{{ importResult.successCount }} 条，失败：{{ importResult.failureCount }} 条
        </div>

        <div class="flex justify-end gap-2 mt-4">
          <button @click="closeImport" class="px-4 py-2 text-sm bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200">关闭</button>
          <button v-if="importFile" @click="handleImport" :disabled="importLoading"
            class="px-4 py-2 text-sm bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50">
            {{ importLoading ? '导入中…' : '开始导入' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useTeams } from '../composables/useTeams'
import { useBatchImport } from '../composables/useBatchImport'

defineEmits(['navigate'])

const route = useRoute()
const router = useRouter()
const { teams, loading, fetchTeams, createTeam, deleteTeam } = useTeams()
const { loading: importLoading, error: importError, importResult, importFromCSV, importFromJSON } = useBatchImport()

const activeTeamId = computed(() => route.params.id)

const showCreateModal = ref(false)
const newTeamName = ref('')

const showImportModal = ref(false)
const importFile = ref(null)

const handleCreate = async () => {
  if (!newTeamName.value.trim()) return
  try {
    const team = await createTeam(newTeamName.value.trim())
    newTeamName.value = ''
    showCreateModal.value = false
    router.push(`/teams/${team.id}`)
  } catch (err) {
    alert(`创建失败: ${err.message}`)
  }
}

const confirmDelete = async (team) => {
  if (!confirm(`确定要删除队伍 "${team.name}" 吗？`)) return
  await deleteTeam(team.id)
  if (activeTeamId.value === team.id) {
    router.push('/')
  }
}

const handleFileSelect = (e) => { importFile.value = e.target.files[0] || null }
const handleDrop = (e) => { importFile.value = e.dataTransfer.files[0] || null }

const handleImport = async () => {
  if (!importFile.value) return
  const ext = importFile.value.name.split('.').pop().toLowerCase()
  if (ext === 'json') await importFromJSON(importFile.value)
  else await importFromCSV(importFile.value)
  await fetchTeams()
}

const closeImport = () => {
  showImportModal.value = false
  importFile.value = null
}

onMounted(fetchTeams)
</script>
