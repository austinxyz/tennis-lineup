import { ref } from 'vue'
import { useApi } from './useApi'

export function useOpponentMatchup() {
  const loading = ref(false)
  const error = ref('')
  const { post } = useApi()

  async function runBestThree(ownTeamId, opponentTeamId, opponentLineupId) {
    loading.value = true
    error.value = ''
    try {
      const res = await post('/api/lineups/matchup', {
        teamId: ownTeamId,
        opponentTeamId,
        opponentLineupId,
      })
      return (res?.results ?? []).slice(0, 3)
    } catch (err) {
      error.value = err.message || '对比失败，请重试'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function runHeadToHead(ownTeamId, ownLineupId, opponentTeamId, opponentLineupId) {
    loading.value = true
    error.value = ''
    try {
      const res = await post('/api/lineups/matchup', {
        teamId: ownTeamId,
        opponentTeamId,
        opponentLineupId,
        ownLineupId,
      })
      return res?.results?.[0] ?? null
    } catch (err) {
      error.value = err.message || '对比失败，请重试'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function runAiAnalysis(ownTeamId, opponentTeamId, opponentLineupId) {
    loading.value = true
    error.value = ''
    try {
      const res = await post('/api/lineups/matchup', {
        teamId: ownTeamId,
        opponentTeamId,
        opponentLineupId,
        includeAi: true,
      })
      return res?.aiRecommendation ?? null
    } catch (err) {
      error.value = err.message || 'AI 分析失败，请重试'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function runCommentary(ownTeamId, ownLineupId, opponentTeamId, opponentLineupId) {
    loading.value = true
    error.value = ''
    try {
      const res = await post('/api/lineups/matchup-commentary', {
        teamId: ownTeamId,
        ownLineupId,
        opponentTeamId,
        opponentLineupId,
      })
      return res
    } catch (err) {
      error.value = err.message || 'AI 评析失败，请重试'
      throw err
    } finally {
      loading.value = false
    }
  }

  return { loading, error, runBestThree, runHeadToHead, runAiAnalysis, runCommentary }
}
