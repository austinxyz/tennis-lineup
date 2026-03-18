import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { useLineup } from '../useLineup'

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

beforeEach(() => {
  vi.spyOn(console, 'error').mockImplementation(() => {})
})

afterEach(() => {
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

describe('useLineup', () => {
  describe('generateLineup()', () => {
    it('sets lineups.value on success (array response)', async () => {
      const mockLineups = [{ id: 'lineup-1', strategy: 'balanced', valid: true, pairs: [] }]
      vi.stubGlobal('fetch', makeFetchOk(mockLineups))
      const { lineups, generateLineup } = useLineup()
      await generateLineup({ teamId: 'team-1', strategyType: 'preset', preset: 'balanced' })
      expect(lineups.value).toEqual(mockLineups)
    })

    it('calls POST /api/lineups/generate with correct body', async () => {
      const mockFetch = makeFetchOk([{ id: 'lineup-1', pairs: [] }])
      vi.stubGlobal('fetch', mockFetch)
      const { generateLineup } = useLineup()
      await generateLineup({ teamId: 'team-1', strategyType: 'preset', preset: 'aggressive' })
      expect(mockFetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/lineups/generate'),
        expect.objectContaining({
          method: 'POST',
          body: expect.stringContaining('"teamId":"team-1"'),
        })
      )
    })

    it('throws and logs error on failure', async () => {
      vi.stubGlobal('fetch', makeFetchError(400, '队伍球员不足8人，无法生成排阵'))
      const { generateLineup } = useLineup()
      await expect(generateLineup({ teamId: 'team-1', strategyType: 'preset', preset: 'balanced' }))
        .rejects.toThrow()
      expect(console.error).toHaveBeenCalled()
    })

    it('includes naturalLanguage when strategyType is custom', async () => {
      const mockFetch = makeFetchOk([{ id: 'lineup-2', pairs: [] }])
      vi.stubGlobal('fetch', mockFetch)
      const { generateLineup } = useLineup()
      await generateLineup({ teamId: 'team-1', strategyType: 'custom', naturalLanguage: '让前三线强' })
      expect(mockFetch).toHaveBeenCalledWith(
        expect.anything(),
        expect.objectContaining({
          body: expect.stringContaining('让前三线强'),
        })
      )
    })

    it('sends pinPlayers, includePlayers and excludePlayers in request body', async () => {
      const mockFetch = makeFetchOk([{ id: 'lineup-3', pairs: [] }])
      vi.stubGlobal('fetch', mockFetch)
      const { generateLineup } = useLineup()
      await generateLineup({
        teamId: 'team-1',
        strategyType: 'preset',
        preset: 'balanced',
        pinPlayers: { p1: 'D1', p2: 'D2' },
        includePlayers: ['p3'],
        excludePlayers: ['p8'],
      })
      const body = JSON.parse(mockFetch.mock.calls[0][1].body)
      expect(body.pinPlayers).toEqual({ p1: 'D1', p2: 'D2' })
      expect(body.includePlayers).toEqual(['p3'])
      expect(body.excludePlayers).toEqual(['p8'])
    })
  })

  describe('saveLineup()', () => {
    it('calls POST /api/teams/{teamId}/lineups with lineup body', async () => {
      const savedLineup = { id: 'lineup-saved', strategy: 'balanced' }
      const mockFetch = makeFetchOk(savedLineup)
      vi.stubGlobal('fetch', mockFetch)
      const { saveLineup } = useLineup()
      const lineup = { id: 'lineup-1', strategy: 'balanced', pairs: [] }
      const result = await saveLineup('team-1', lineup)
      expect(mockFetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/teams/team-1/lineups'),
        expect.objectContaining({
          method: 'POST',
          body: expect.stringContaining('"id":"lineup-1"'),
        })
      )
      expect(result).toEqual(savedLineup)
    })

    it('throws and logs error on failure', async () => {
      vi.stubGlobal('fetch', makeFetchError(404, '队伍不存在'))
      const { saveLineup } = useLineup()
      await expect(saveLineup('unknown', {})).rejects.toThrow()
      expect(console.error).toHaveBeenCalled()
    })
  })

  describe('fetchLineupHistory()', () => {
    it('sets lineupHistory.value with fetched data', async () => {
      const history = [
        { id: 'lineup-2', strategy: 'aggressive' },
        { id: 'lineup-1', strategy: 'balanced' },
      ]
      vi.stubGlobal('fetch', makeFetchOk(history))
      const { lineupHistory, fetchLineupHistory } = useLineup()
      await fetchLineupHistory('team-1')
      expect(lineupHistory.value).toEqual(history)
    })

    it('calls GET /api/teams/:id/lineups', async () => {
      const mockFetch = makeFetchOk([])
      vi.stubGlobal('fetch', mockFetch)
      const { fetchLineupHistory } = useLineup()
      await fetchLineupHistory('team-42')
      expect(mockFetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/teams/team-42/lineups'),
        expect.anything()
      )
    })

    it('throws and logs error on failure', async () => {
      vi.stubGlobal('fetch', makeFetchError(404, '队伍不存在'))
      const { fetchLineupHistory } = useLineup()
      await expect(fetchLineupHistory('unknown')).rejects.toThrow()
      expect(console.error).toHaveBeenCalled()
    })
  })

  describe('deleteLineup()', () => {
    it('removes lineup from lineupHistory on success', async () => {
      const history = [{ id: 'lineup-1' }, { id: 'lineup-2' }]
      vi.stubGlobal('fetch', makeFetchOk(history))
      const { lineupHistory, fetchLineupHistory } = useLineup()
      await fetchLineupHistory('team-1')

      vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, json: vi.fn().mockResolvedValue(null) }))
      const { deleteLineup } = useLineup()
      // Create fresh instance with the history populated
      const instance = useLineup()
      instance.lineupHistory.value = [...history]
      await instance.deleteLineup('lineup-1')
      expect(instance.lineupHistory.value).toEqual([{ id: 'lineup-2' }])
    })

    it('calls DELETE /api/lineups/:id', async () => {
      const mockFetch = vi.fn().mockResolvedValue({ ok: true, json: vi.fn().mockResolvedValue(null) })
      vi.stubGlobal('fetch', mockFetch)
      const { deleteLineup } = useLineup()
      await deleteLineup('lineup-99')
      expect(mockFetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/lineups/lineup-99'),
        expect.objectContaining({ method: 'DELETE' })
      )
    })

    it('throws and logs error on 404', async () => {
      vi.stubGlobal('fetch', makeFetchError(404, '排阵不存在'))
      const { deleteLineup } = useLineup()
      await expect(deleteLineup('nonexistent')).rejects.toThrow()
      expect(console.error).toHaveBeenCalled()
    })
  })
})
