import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { useOpponentAnalysis } from '../useOpponentAnalysis'

function makeFetchOk(data) {
  return vi.fn().mockResolvedValue({
    ok: true,
    json: vi.fn().mockResolvedValue(data),
  })
}

function makeFetchError(status, message) {
  return vi.fn().mockResolvedValue({
    ok: false,
    status,
    json: vi.fn().mockResolvedValue({ message }),
  })
}

const mockResponse = {
  utrRecommendation: {
    lineup: { id: 'lineup-1', pairs: [] },
    lineAnalysis: [
      { position: 'D1', ownCombinedUtr: 9.5, opponentCombinedUtr: 8.5, delta: 1.0, winProbability: 0.6, label: '60% 赢' },
    ],
    expectedScore: 6.0,
    opponentExpectedScore: 4.0,
  },
  aiRecommendation: {
    lineup: { id: 'lineup-1', pairs: [] },
    explanation: 'AI 根据对手排阵选择最优方案',
    aiUsed: true,
  },
}

beforeEach(() => {
  vi.spyOn(console, 'error').mockImplementation(() => {})
})

afterEach(() => {
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

describe('useOpponentAnalysis', () => {
  describe('analyzeOpponent()', () => {
    it('posts to /api/lineups/analyze-opponent and sets result', async () => {
      const mockFetch = makeFetchOk(mockResponse)
      vi.stubGlobal('fetch', mockFetch)
      const { result, analyzeOpponent } = useOpponentAnalysis()

      await analyzeOpponent('team-1', 'team-2', 'lineup-opp-1')

      expect(result.value).toEqual(mockResponse)
      expect(mockFetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/lineups/analyze-opponent'),
        expect.objectContaining({ method: 'POST' })
      )
    })

    it('sends correct fields in request body', async () => {
      const mockFetch = makeFetchOk(mockResponse)
      vi.stubGlobal('fetch', mockFetch)
      const { analyzeOpponent } = useOpponentAnalysis()

      await analyzeOpponent('team-1', 'team-2', 'lineup-99', {
        strategyType: 'custom',
        naturalLanguage: '打D3',
        includePlayers: ['p1'],
        excludePlayers: ['p2'],
        pinPlayers: { p3: 'D1' },
      })

      const body = JSON.parse(mockFetch.mock.calls[0][1].body)
      expect(body.teamId).toBe('team-1')
      expect(body.opponentTeamId).toBe('team-2')
      expect(body.opponentLineupId).toBe('lineup-99')
      expect(body.strategyType).toBe('custom')
      expect(body.naturalLanguage).toBe('打D3')
      expect(body.includePlayers).toEqual(['p1'])
      expect(body.excludePlayers).toEqual(['p2'])
      expect(body.pinPlayers).toEqual({ p3: 'D1' })
    })

    it('resets result.value to null before each call', async () => {
      const mockFetch = makeFetchOk(mockResponse)
      vi.stubGlobal('fetch', mockFetch)
      const { result, analyzeOpponent } = useOpponentAnalysis()
      result.value = { old: 'data' }

      const promise = analyzeOpponent('team-1', 'team-2', 'lineup-1')
      expect(result.value).toBeNull()
      await promise
    })

    it('sets loading state during call', async () => {
      let resolveResponse
      const mockFetch = vi.fn().mockReturnValue(
        new Promise(resolve => { resolveResponse = () => resolve({ ok: true, json: () => mockResponse }) })
      )
      vi.stubGlobal('fetch', mockFetch)
      const { loading, analyzeOpponent } = useOpponentAnalysis()

      const promise = analyzeOpponent('team-1', 'team-2', 'lineup-1')
      expect(loading.value).toBe(true)
      resolveResponse()
      await promise
      expect(loading.value).toBe(false)
    })

    it('throws and logs error on API failure', async () => {
      vi.stubGlobal('fetch', makeFetchError(404, '队伍不存在'))
      const { analyzeOpponent } = useOpponentAnalysis()

      await expect(analyzeOpponent('bad-team', 'team-2', 'lineup-1')).rejects.toThrow()
      expect(console.error).toHaveBeenCalled()
    })
  })
})
