import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { useOpponentMatchup } from '../useOpponentMatchup'

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

const mockResults = [
  { lineup: { id: 'l1' }, expectedScore: 8.0, opponentExpectedScore: 2.0, verdict: '能赢', lineAnalysis: [] },
  { lineup: { id: 'l2' }, expectedScore: 6.5, opponentExpectedScore: 3.5, verdict: '势均力敌', lineAnalysis: [] },
  { lineup: { id: 'l3' }, expectedScore: 5.5, opponentExpectedScore: 4.5, verdict: '势均力敌', lineAnalysis: [] },
  { lineup: { id: 'l4' }, expectedScore: 4.0, opponentExpectedScore: 6.0, verdict: '劣势', lineAnalysis: [] },
]

const mockAiRecommendation = {
  aiUsed: true,
  lineup: { id: 'l1' },
  explanation: 'D1组合UTR优势明显',
  lineAnalysis: [],
  expectedScore: 8.0,
  opponentExpectedScore: 2.0,
}

beforeEach(() => {
  vi.spyOn(console, 'error').mockImplementation(() => {})
})

afterEach(() => {
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

describe('useOpponentMatchup', () => {
  // runBestThree
  describe('runBestThree', () => {
    it('posts to /api/lineups/matchup and returns top 3 results', async () => {
      vi.stubGlobal('fetch', makeFetchOk({ results: mockResults }))
      const { runBestThree } = useOpponentMatchup()

      const result = await runBestThree('own-team', 'opp-team', 'opp-lineup-1')

      expect(result).toHaveLength(3)
      expect(result[0].lineup.id).toBe('l1')
      expect(result[2].lineup.id).toBe('l3')
    })

    it('sends correct request body fields', async () => {
      const mockFetch = makeFetchOk({ results: mockResults })
      vi.stubGlobal('fetch', mockFetch)
      const { runBestThree } = useOpponentMatchup()

      await runBestThree('team-A', 'team-B', 'lineup-X')

      const body = JSON.parse(mockFetch.mock.calls[0][1].body)
      expect(body.teamId).toBe('team-A')
      expect(body.opponentTeamId).toBe('team-B')
      expect(body.opponentLineupId).toBe('lineup-X')
      expect(body.ownLineupId).toBeUndefined()
      expect(body.includeAi).toBeUndefined()
    })

    it('returns empty array when results is missing', async () => {
      vi.stubGlobal('fetch', makeFetchOk({}))
      const { runBestThree } = useOpponentMatchup()

      const result = await runBestThree('t', 'o', 'l')
      expect(result).toEqual([])
    })

    it('sets loading state during call', async () => {
      let resolveResponse
      vi.stubGlobal('fetch', vi.fn().mockReturnValue(
        new Promise(resolve => {
          resolveResponse = () => resolve({ ok: true, json: () => ({ results: [] }) })
        })
      ))
      const { loading, runBestThree } = useOpponentMatchup()

      const promise = runBestThree('t', 'o', 'l')
      expect(loading.value).toBe(true)
      resolveResponse()
      await promise
      expect(loading.value).toBe(false)
    })

    it('throws on API error and sets error message', async () => {
      vi.stubGlobal('fetch', makeFetchError(404, '队伍不存在'))
      const { error, runBestThree } = useOpponentMatchup()

      await expect(runBestThree('bad', 'opp', 'l')).rejects.toThrow()
      expect(error.value).toBeTruthy()
    })
  })

  // runHeadToHead
  describe('runHeadToHead', () => {
    it('posts with ownLineupId and returns single result', async () => {
      const mockFetch = makeFetchOk({ results: [mockResults[0]] })
      vi.stubGlobal('fetch', mockFetch)
      const { runHeadToHead } = useOpponentMatchup()

      const result = await runHeadToHead('own-team', 'own-1', 'opp-team', 'opp-lineup-1')

      expect(result).toEqual(mockResults[0])
    })

    it('sends ownLineupId in request body', async () => {
      const mockFetch = makeFetchOk({ results: [mockResults[0]] })
      vi.stubGlobal('fetch', mockFetch)
      const { runHeadToHead } = useOpponentMatchup()

      await runHeadToHead('team-A', 'lineup-mine', 'team-B', 'lineup-opp')

      const body = JSON.parse(mockFetch.mock.calls[0][1].body)
      expect(body.teamId).toBe('team-A')
      expect(body.ownLineupId).toBe('lineup-mine')
      expect(body.opponentTeamId).toBe('team-B')
      expect(body.opponentLineupId).toBe('lineup-opp')
    })

    it('returns null when results array is empty', async () => {
      vi.stubGlobal('fetch', makeFetchOk({ results: [] }))
      const { runHeadToHead } = useOpponentMatchup()

      const result = await runHeadToHead('t', 'l1', 'o', 'l2')
      expect(result).toBeNull()
    })

    it('throws on API error', async () => {
      vi.stubGlobal('fetch', makeFetchError(500, '服务器错误'))
      const { runHeadToHead } = useOpponentMatchup()

      await expect(runHeadToHead('t', 'l1', 'o', 'l2')).rejects.toThrow()
    })
  })

  // runAiAnalysis
  describe('runAiAnalysis', () => {
    it('posts with includeAi:true and returns aiRecommendation', async () => {
      const mockFetch = makeFetchOk({ results: [], aiRecommendation: mockAiRecommendation })
      vi.stubGlobal('fetch', mockFetch)
      const { runAiAnalysis } = useOpponentMatchup()

      const result = await runAiAnalysis('own-team', 'opp-team', 'opp-lineup-1')

      expect(result).toEqual(mockAiRecommendation)
    })

    it('sends includeAi:true in request body', async () => {
      const mockFetch = makeFetchOk({ results: [], aiRecommendation: mockAiRecommendation })
      vi.stubGlobal('fetch', mockFetch)
      const { runAiAnalysis } = useOpponentMatchup()

      await runAiAnalysis('team-A', 'team-B', 'lineup-X')

      const body = JSON.parse(mockFetch.mock.calls[0][1].body)
      expect(body.includeAi).toBe(true)
      expect(body.ownLineupId).toBeUndefined()
    })

    it('returns null when aiRecommendation is missing', async () => {
      vi.stubGlobal('fetch', makeFetchOk({ results: [] }))
      const { runAiAnalysis } = useOpponentMatchup()

      const result = await runAiAnalysis('t', 'o', 'l')
      expect(result).toBeNull()
    })

    it('throws on API error and sets error message', async () => {
      vi.stubGlobal('fetch', makeFetchError(500, 'AI 分析失败'))
      const { error, runAiAnalysis } = useOpponentMatchup()

      await expect(runAiAnalysis('t', 'o', 'l')).rejects.toThrow()
      expect(error.value).toBeTruthy()
    })
  })

  // runCommentary
  describe('runCommentary', () => {
    const mockCommentaryResponse = {
      aiUsed: true,
      lines: [
        { position: 'D1', commentary: '己方UTR优势，建议主动进攻' },
        { position: 'D2', commentary: '势均力敌，注重稳定发挥' },
        { position: 'D3', commentary: '处于劣势，多以防守反击为主' },
        { position: 'D4', commentary: '势均力敌，注重稳定发挥' },
      ],
    }

    it('posts to /api/lineups/matchup-commentary and returns lines', async () => {
      vi.stubGlobal('fetch', makeFetchOk(mockCommentaryResponse))
      const { runCommentary } = useOpponentMatchup()

      const result = await runCommentary('own-team', 'own-lineup', 'opp-team', 'opp-lineup')

      expect(result.aiUsed).toBe(true)
      expect(result.lines).toHaveLength(4)
      expect(result.lines[0].position).toBe('D1')
      expect(result.lines[0].commentary).toBe('己方UTR优势，建议主动进攻')
    })

    it('sends correct request body fields', async () => {
      const mockFetch = makeFetchOk(mockCommentaryResponse)
      vi.stubGlobal('fetch', mockFetch)
      const { runCommentary } = useOpponentMatchup()

      await runCommentary('team-A', 'lineup-own', 'team-B', 'lineup-opp')

      const [url, opts] = mockFetch.mock.calls[0]
      expect(url).toContain('/api/lineups/matchup-commentary')
      const body = JSON.parse(opts.body)
      expect(body.teamId).toBe('team-A')
      expect(body.ownLineupId).toBe('lineup-own')
      expect(body.opponentTeamId).toBe('team-B')
      expect(body.opponentLineupId).toBe('lineup-opp')
    })

    it('returns fallback commentary when aiUsed is false', async () => {
      const fallbackResponse = {
        aiUsed: false,
        lines: [
          { position: 'D1', commentary: '具备优势，建议主动进攻' },
          { position: 'D2', commentary: '势均力敌，注重稳定发挥' },
          { position: 'D3', commentary: '势均力敌，注重稳定发挥' },
          { position: 'D4', commentary: '处于劣势，多以防守反击为主' },
        ],
      }
      vi.stubGlobal('fetch', makeFetchOk(fallbackResponse))
      const { runCommentary } = useOpponentMatchup()

      const result = await runCommentary('t', 'l1', 'o', 'l2')
      expect(result.aiUsed).toBe(false)
      expect(result.lines).toHaveLength(4)
    })

    it('throws on 404 and sets error message', async () => {
      vi.stubGlobal('fetch', makeFetchError(404, '排阵不存在'))
      const { error, runCommentary } = useOpponentMatchup()

      await expect(runCommentary('t', 'l1', 'o', 'l2')).rejects.toThrow()
      expect(error.value).toBeTruthy()
    })
  })
})
