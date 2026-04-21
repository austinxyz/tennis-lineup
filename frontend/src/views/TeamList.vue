<template>
  <div>
    <div class="flex justify-between items-center mb-6">
      <h2 class="text-2xl font-bold text-gray-900">队伍列表</h2>
      <button
        @click="showCreateModal = true"
        class="bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700 transition-colors">
        创建队伍
      </button>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="flex justify-center items-center h-64">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-gray-900"></div>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="bg-red-50 border border-red-200 rounded-md p-4">
      <p class="text-red-800">{{ error }}</p>
    </div>

    <!-- Empty State -->
    <div v-else-if="teams.length === 0" class="text-center py-12">
      <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
      </svg>
      <h3 class="mt-2 text-sm font-medium text-gray-900">暂无队伍</h3>
      <p class="mt-1 text-sm text-gray-500">开始创建您的第一个队伍</p>
      <button
        @click="showCreateModal = true"
        class="mt-4 bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700">
        创建队伍
      </button>
    </div>

    <!-- Teams Grid -->
    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <div
        v-for="team in teams"
        :key="team.id"
        class="bg-white rounded-lg shadow-md hover:shadow-lg transition-shadow">
        <div class="p-6">
          <div class="flex justify-between items-start">
            <div>
              <h3 class="text-lg font-semibold text-gray-900">{{ team.name }}</h3>
              <p class="text-sm text-gray-500 mt-1">
                创建于: {{ formatDate(team.createdAt) }}
              </p>
            </div>
            <button
              @click="confirmDelete(team)"
              :disabled="!isDeletable(team)"
              :title="deleteDisabledReason(team)"
              class="text-red-600 hover:text-red-800 p-1 disabled:cursor-not-allowed disabled:text-gray-300 disabled:hover:text-gray-300">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
            </button>
          </div>

          <div class="mt-4">
            <p class="text-sm text-gray-600">
              球员数量: {{ team.players?.length || 0 }}
            </p>
            <router-link
              :to="`/teams/${team.id}`"
              class="mt-2 inline-flex items-center text-blue-600 hover:text-blue-800 text-sm">
              查看详情
              <svg class="w-4 h-4 ml-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
              </svg>
            </router-link>
          </div>
        </div>
      </div>
    </div>

    <!-- Create Team Modal -->
    <div v-if="showCreateModal" class="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
      <div class="relative top-20 mx-auto p-5 border w-96 shadow-lg rounded-md bg-white">
        <div class="mt-3">
          <h3 class="text-lg font-medium text-gray-900 mb-4">创建新队伍</h3>
          <form @submit.prevent="createTeam">
            <div class="mb-4">
              <label for="teamName" class="block text-sm font-medium text-gray-700 mb-2">
                队名
              </label>
              <input
                type="text"
                id="teamName"
                v-model="newTeamName"
                class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="请输入队名"
                required />
            </div>
            <div class="flex justify-end space-x-3">
              <button
                type="button"
                @click="showCreateModal = false"
                class="px-4 py-2 bg-gray-300 text-gray-700 rounded-md hover:bg-gray-400">
                取消
              </button>
              <button
                type="submit"
                :disabled="loading"
                class="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50">
                创建
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useTeams } from '../composables/useTeams'

const router = useRouter()
const { teams, loading, error, fetchTeams, createTeam, deleteTeam } = useTeams()
const showCreateModal = ref(false)
const newTeamName = ref('')

const formatDate = (dateString) => {
  return new Date(dateString).toLocaleDateString('zh-CN')
}

const isDeletable = (team) =>
  (team.players?.length ?? 0) === 0 && (team.lineups?.length ?? 0) === 0

const deleteDisabledReason = (team) => {
  const hasPlayers = (team.players?.length ?? 0) > 0
  const hasLineups = (team.lineups?.length ?? 0) > 0
  if (hasPlayers && hasLineups) return '请先移除球员和已保存的排阵后再删除队伍'
  if (hasPlayers) return '请先移除球员后再删除队伍'
  if (hasLineups) return '请先移除已保存的排阵后再删除队伍'
  return ''
}

const confirmDelete = async (team) => {
  if (!isDeletable(team)) return
  if (!confirm(`确定要删除队伍 "${team.name}" 吗？此操作不可撤销。`)) return
  try {
    await deleteTeam(team.id)
  } catch (err) {
    alert(`删除失败: ${err.message}`)
  }
}

const createNewTeam = async () => {
  if (!newTeamName.value.trim()) return

  try {
    await createTeam(newTeamName.value.trim())
    newTeamName.value = ''
    showCreateModal.value = false
  } catch (err) {
    alert(`创建失败: ${err.message}`)
  }
}

onMounted(() => {
  fetchTeams()
})
</script>