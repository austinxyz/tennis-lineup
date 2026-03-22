import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { useSavedLineupMatchup } from '../useSavedLineupMatchup'

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

const mockMatchupResponse = {
  results: [
    {
      lineup: { id: 'own-lineup-2', pairs: [] },
      lineAnalysis: [
        { position: 'D1', ownCombinedUtr: 10.0, opponentCombinedUtr: 9.0, delta: 1.0, winProbability: 0.8, label: '80% 赢' },
      ],
      expectedScore: 7.2,
      opponentExpectedScore: 2.8,
      verdict: '能赢',
    },
    {
      lineup: { id: 'own-lineup-1', pairs: [] },
      lineAnalysis: [
        { position: 'D1', ownCombinedUtr: 8.0, opponentCombinedUtr: 9.0, delta: -1.0, winProbability: 0.4, label: '60% 输' },
      ],
      expectedScore: 4.0,
      opponentExpectedScore: 6.0,
      verdict: '势均力敌',
    },
  ],
}

beforeEach(() => {
  vi.spyOn(console, 'error').mockImplementation(() => {})
})

afterEach(() => {
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

describe('useSavedLineupMatchup', () => {
  it('posts to /api/lineups/matchup and sets matchupResults', async () => {
    const mockFetch = makeFetchOk(mockMatchupResponse)
    vi.stubGlobal('fetch', mockFetch)
    const { matchupResults, runMatchup } = useSavedLineupMatchup()

    await runMatchup('own-team', 'opp-team', 'opp-lineup-1')

    expect(matchupResults.value).toEqual(mockMatchupResponse.results)
    expect(mockFetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/lineups/matchup'),
      expect.objectContaining({ method: 'POST' })
    )
  })

  it('sends correct fields in request body', async () => {
    const mockFetch = makeFetchOk(mockMatchupResponse)
    vi.stubGlobal('fetch', mockFetch)
    const { runMatchup } = useSavedLineupMatchup()

    await runMatchup('team-A', 'team-B', 'lineup-X')

    const body = JSON.parse(mockFetch.mock.calls[0][1].body)
    expect(body.teamId).toBe('team-A')
    expect(body.opponentTeamId).toBe('team-B')
    expect(body.opponentLineupId).toBe('lineup-X')
  })

  it('resets matchupResults to empty before each call', async () => {
    const mockFetch = makeFetchOk(mockMatchupResponse)
    vi.stubGlobal('fetch', mockFetch)
    const { matchupResults, runMatchup } = useSavedLineupMatchup()
    matchupResults.value = [{ old: 'data' }]

    const promise = runMatchup('team-A', 'team-B', 'lineup-X')
    expect(matchupResults.value).toEqual([])
    await promise
  })

  it('sets loading state during call', async () => {
    let resolveResponse
    const mockFetch = vi.fn().mockReturnValue(
      new Promise(resolve => {
        resolveResponse = () =>
          resolve({ ok: true, json: () => mockMatchupResponse })
      })
    )
    vi.stubGlobal('fetch', mockFetch)
    const { loading, runMatchup } = useSavedLineupMatchup()

    const promise = runMatchup('team-A', 'team-B', 'lineup-X')
    expect(loading.value).toBe(true)
    resolveResponse()
    await promise
    expect(loading.value).toBe(false)
  })

  it('throws on API failure and matchupResults stays empty', async () => {
    vi.stubGlobal('fetch', makeFetchError(404, '队伍不存在'))
    const { matchupResults, runMatchup } = useSavedLineupMatchup()

    await expect(runMatchup('bad-team', 'opp-team', 'lineup-1')).rejects.toThrow()
    expect(matchupResults.value).toEqual([])
  })
})
