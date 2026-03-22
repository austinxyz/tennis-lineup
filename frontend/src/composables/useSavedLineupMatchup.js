import { ref } from 'vue'
import { useApi } from './useApi'

export function useSavedLineupMatchup() {
  const { loading, error, post } = useApi()
  const matchupResults = ref([])

  const runMatchup = async (ownTeamId, opponentTeamId, opponentLineupId) => {
    matchupResults.value = []
    const response = await post('/api/lineups/matchup', {
      teamId: ownTeamId,
      opponentTeamId,
      opponentLineupId,
    })
    matchupResults.value = response?.results ?? []
    return matchupResults.value
  }

  return {
    loading,
    error,
    matchupResults,
    runMatchup,
  }
}
