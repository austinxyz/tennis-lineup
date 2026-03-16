import { ref } from 'vue'
import { useApi } from './useApi'

// Module-level shared state — all components see the same refs
const teams = ref([])
const team = ref(null)

export function useTeams() {
  const { loading, error, get, post, put, del } = useApi()

  const fetchTeams = async () => {
    try {
      teams.value = await get('/api/teams')
    } catch (err) {
      console.error('Failed to fetch teams:', err)
      throw err
    }
  }

  const fetchTeamById = async (id) => {
    try {
      team.value = await get(`/api/teams/${id}`)
      // 同步更新 teams 列表中对应队伍的球员数据
      const index = teams.value.findIndex(t => t.id === id)
      if (index !== -1) {
        teams.value[index] = team.value
      }
    } catch (err) {
      console.error('Failed to fetch team:', err)
      throw err
    }
  }

  const createTeam = async (name) => {
    try {
      const newTeam = await post('/api/teams', { name })
      teams.value.unshift(newTeam)
      return newTeam
    } catch (err) {
      console.error('Failed to create team:', err)
      throw err
    }
  }

  const updateTeam = async (id, name) => {
    try {
      const updatedTeam = await put(`/api/teams/${id}`, { name })
      const index = teams.value.findIndex(t => t.id === id)
      if (index !== -1) {
        teams.value[index] = updatedTeam
      }
      return updatedTeam
    } catch (err) {
      console.error('Failed to update team:', err)
      throw err
    }
  }

  const deleteTeam = async (id) => {
    if (!confirm('确定要删除这个队伍吗？此操作不可撤销。')) {
      return
    }

    try {
      await del(`/api/teams/${id}`)
      teams.value = teams.value.filter(t => t.id !== id)
    } catch (err) {
      console.error('Failed to delete team:', err)
      throw err
    }
  }

  return {
    loading,
    error,
    teams,
    team,
    fetchTeams,
    fetchTeamById,
    createTeam,
    updateTeam,
    deleteTeam,
  }
}
