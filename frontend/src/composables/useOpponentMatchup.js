import { ref } from 'vue'
import { useApi } from './useApi'

const MAX_PARTNER_NOTES = 10

async function fetchPartnerNotesForAi(teamId, get) {
  try {
    const notes = await get(`/api/teams/${teamId}/partner-notes`)
    if (!Array.isArray(notes) || notes.length === 0) return []
    return notes
      .filter(n => n.updatedAt)
      .sort((a, b) => new Date(b.updatedAt) - new Date(a.updatedAt))
      .slice(0, MAX_PARTNER_NOTES)
      .map(n => ({ player1Name: n.player1Name, player2Name: n.player2Name, note: n.note }))
  } catch {
    return []
  }
}

export function useOpponentMatchup() {
  const loading = ref(false)
  const error = ref('')
  const { post, get } = useApi()

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
      const [ownPartnerNotes, opponentPartnerNotes] = await Promise.all([
        fetchPartnerNotesForAi(ownTeamId, get),
        fetchPartnerNotesForAi(opponentTeamId, get),
      ])
      const res = await post('/api/lineups/matchup', {
        teamId: ownTeamId,
        opponentTeamId,
        opponentLineupId,
        includeAi: true,
        ownPartnerNotes: ownPartnerNotes.length > 0 ? ownPartnerNotes : undefined,
        opponentPartnerNotes: opponentPartnerNotes.length > 0 ? opponentPartnerNotes : undefined,
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
