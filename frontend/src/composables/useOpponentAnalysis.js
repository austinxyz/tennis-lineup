import { ref } from 'vue'
import { useApi } from './useApi'

export function useOpponentAnalysis() {
  const { loading, error, post } = useApi()
  const result = ref(null)

  const analyzeOpponent = async (ownTeamId, opponentTeamId, opponentLineupId, constraints = {}, includeAi = false) => {
    if (!includeAi) result.value = null
    try {
      const res = await post('/api/lineups/analyze-opponent', {
        teamId: ownTeamId,
        opponentTeamId,
        opponentLineupId,
        strategyType: constraints.strategyType || 'preset',
        naturalLanguage: constraints.naturalLanguage || null,
        includePlayers: constraints.includePlayers || [],
        excludePlayers: constraints.excludePlayers || [],
        pinPlayers: constraints.pinPlayers || {},
        includeAi,
      })
      if (includeAi) {
        // Merge AI result into existing result
        result.value = { ...result.value, aiRecommendation: res.aiRecommendation }
      } else {
        result.value = res
      }
      return result.value
    } catch (err) {
      console.error('Failed to analyze opponent:', err)
      throw err
    }
  }

  return {
    loading,
    error,
    result,
    analyzeOpponent,
  }
}
