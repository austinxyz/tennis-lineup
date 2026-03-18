import { ref } from 'vue'
import { useApi } from './useApi'

export function useLineup() {
  const { loading, error, post, get, del } = useApi()
  const lineups = ref([])
  const lineupHistory = ref([])

  const generateLineup = async ({ teamId, strategyType, preset, naturalLanguage, pinPlayers = {}, excludePlayers = [] }) => {
    try {
      lineups.value = await post('/api/lineups/generate', {
        teamId,
        strategyType,
        preset,
        naturalLanguage,
        pinPlayers,
        excludePlayers,
      })
      return lineups.value
    } catch (err) {
      console.error('Failed to generate lineup:', err)
      throw err
    }
  }

  const fetchLineupHistory = async (teamId) => {
    try {
      lineupHistory.value = await get(`/api/teams/${teamId}/lineups`)
      return lineupHistory.value
    } catch (err) {
      console.error('Failed to fetch lineup history:', err)
      throw err
    }
  }

  const deleteLineup = async (lineupId) => {
    try {
      await del(`/api/lineups/${lineupId}`)
      lineupHistory.value = lineupHistory.value.filter(l => l.id !== lineupId)
    } catch (err) {
      console.error('Failed to delete lineup:', err)
      throw err
    }
  }

  return {
    loading,
    error,
    lineups,
    lineupHistory,
    generateLineup,
    fetchLineupHistory,
    deleteLineup,
  }
}
