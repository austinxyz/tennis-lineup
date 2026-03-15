<template>
  <div v-if="team" class="p-6">
    <!-- Header -->
    <div class="flex justify-between items-center mb-6">
      <div>
        <h2 class="text-2xl font-bold text-gray-900">{{ team.name }}</h2>
        <p class="text-gray-500 text-sm">创建于: {{ formatDate(team.createdAt) }}</p>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="playersLoading" class="flex justify-center items-center h-64">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-gray-900"></div>
    </div>

    <!-- Players Section -->
    <div v-else>
      <div class="flex justify-between items-center mb-4">
        <h3 class="text-xl font-semibold">球员列表 ({{ players.length }})</h3>
        <button
          @click="showAddPlayerModal = true"
          class="bg-green-600 text-white px-4 py-2 rounded-md hover:bg-green-700 transition-colors">
          添加球员
        </button>
      </div>

      <!-- Players Table -->
      <div class="bg-white rounded-lg shadow overflow-hidden">
        <table class="min-w-full divide-y divide-gray-200">
          <thead class="bg-gray-50">
            <tr>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                姓名
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                性别
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                UTR
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                已验证
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                操作
              </th>
            </tr>
          </thead>
          <tbody class="bg-white divide-y divide-gray-200">
            <tr v-if="players.length === 0">
              <td colspan="5" class="px-6 py-4 text-center text-gray-500">
                暂无球员，点击上方按钮添加
              </td>
            </tr>
            <tr v-for="player in players" :key="player.id">
              <td class="px-6 py-4 whitespace-nowrap">
                <div class="text-sm font-medium text-gray-900">{{ player.name }}</div>
              </td>
              <td class="px-6 py-4 whitespace-nowrap">
                <span class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full"
                      :class="player.gender === 'male' ? 'bg-blue-100 text-blue-800' : 'bg-pink-100 text-pink-800'">
                  {{ player.gender === 'male' ? '男' : '女' }}
                </span>
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                {{ player.utr }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap">
                <span class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full"
                      :class="player.verified ? 'bg-green-100 text-green-800' : 'bg-yellow-100 text-yellow-800'">
                  {{ player.verified ? '已验证' : '未验证' }}
                </span>
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm font-medium">
                <button
                  @click="editPlayer(player)"
                  class="text-indigo-600 hover:text-indigo-900 mr-3">
                  编辑
                </button>
                <button
                  @click="confirmDeletePlayer(player)"
                  class="text-red-600 hover:text-red-800">
                  删除
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Add/Edit Player Modal -->
    <div v-if="showAddPlayerModal || editingPlayer" class="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
      <div class="relative top-20 mx-auto p-5 border w-96 shadow-lg rounded-md bg-white">
        <div class="mt-3">
          <h3 class="text-lg font-medium text-gray-900 mb-4">
            {{ editingPlayer ? '编辑球员' : '添加球员' }}
          </h3>
          <form @submit.prevent="savePlayer">
            <div class="mb-4">
              <label for="playerName" class="block text-sm font-medium text-gray-700 mb-2">
                姓名
              </label>
              <input
                type="text"
                id="playerName"
                v-model="playerForm.name"
                class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                required />
            </div>
            <div class="mb-4">
              <label for="playerGender" class="block text-sm font-medium text-gray-700 mb-2">
                性别
              </label>
              <select
                id="playerGender"
                v-model="playerForm.gender"
                class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500">
                <option value="male">男</option>
                <option value="female">女</option>
              </select>
            </div>
            <div class="mb-4">
              <label for="playerUtr" class="block text-sm font-medium text-gray-700 mb-2">
                UTR
              </label>
              <input
                type="number"
                id="playerUtr"
                v-model.number="playerForm.utr"
                step="0.1"
                min="0"
                max="16"
                class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                required />
            </div>
            <div class="mb-4">
              <label for="playerVerified" class="block text-sm font-medium text-gray-700 mb-2">
                已验证
              </label>
              <input
                type="checkbox"
                id="playerVerified"
                v-model="playerForm.verified"
                class="rounded border-gray-300 text-blue-600 focus:ring-blue-500" />
            </div>
            <div class="flex justify-end space-x-3">
              <button
                type="button"
                @click="cancelPlayerEdit"
                class="px-4 py-2 bg-gray-300 text-gray-700 rounded-md hover:bg-gray-400">
                取消
              </button>
              <button
                type="submit"
                :disabled="playersLoading"
                class="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50">
                {{ editingPlayer ? '更新' : '添加' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { useTeams } from '../composables/useTeams'
import { usePlayers } from '../composables/usePlayers'

const route = useRoute()
const teamId = route.params.id

const { team, loading: teamsLoading, fetchTeamById } = useTeams()
const { players, loading: playersLoading, fetchPlayers, addPlayer, updatePlayer, deletePlayer } = usePlayers(teamId)

const showAddPlayerModal = ref(false)
const editingPlayer = ref(null)
const playerForm = ref({
  name: '',
  gender: 'male',
  utr: 1.0,
  verified: false,
})

const formatDate = (dateString) => {
  return new Date(dateString).toLocaleDateString('zh-CN')
}

const loadTeam = async () => {
  await fetchTeamById(teamId)
  await fetchPlayers()
}

const editPlayer = (player) => {
  editingPlayer.value = player
  playerForm.value = { ...player }
}

const savePlayer = async () => {
  try {
    if (editingPlayer.value) {
      await updatePlayer(editingPlayer.value.id, playerForm.value)
    } else {
      await addPlayer(playerForm.value)
    }
    cancelPlayerEdit()
  } catch (err) {
    alert(`操作失败: ${err.message}`)
  }
}

const cancelPlayerEdit = () => {
  editingPlayer.value = null
  showAddPlayerModal.value = false
  playerForm.value = {
    name: '',
    gender: 'male',
    utr: 1.0,
    verified: false,
  }
}

const confirmDeletePlayer = (player) => {
  if (confirm(`确定要删除球员 "${player.name}" 吗？`)) {
    deletePlayer(player.id)
  }
}

onMounted(() => {
  loadTeam()
})

onUnmounted(() => {
  cancelPlayerEdit()
})
</script>