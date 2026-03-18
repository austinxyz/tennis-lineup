import { ref } from 'vue'
import { useApi } from './useApi'

export function useLineupHistory() {
  const { loading, error, get, del } = useApi()
  const lineups = ref([])

  const fetchLineups = async (teamId) => {
    try {
      lineups.value = await get(`/api/teams/${teamId}/lineups`)
    } catch (err) {
      console.error('Failed to fetch lineups:', err)
      throw err
    }
  }

  const deleteLineup = async (lineupId) => {
    try {
      await del(`/api/lineups/${lineupId}`)
      lineups.value = lineups.value.filter(l => l.id !== lineupId)
    } catch (err) {
      console.error('Failed to delete lineup:', err)
      throw err
    }
  }

  return {
    loading,
    error,
    lineups,
    fetchLineups,
    deleteLineup,
  }
}
