import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { useLineupHistory } from '../useLineupHistory'

const TEAM_ID = 'team-1'

function makeFetchOk(data) {
  return vi.fn().mockResolvedValue({
    ok: true,
    status: 200,
    json: vi.fn().mockResolvedValue(data),
  })
}

function makeFetchNoContent() {
  return vi.fn().mockResolvedValue({
    ok: true,
    status: 204,
    json: vi.fn().mockResolvedValue(null),
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

describe('useLineupHistory', () => {
  describe('fetchLineups()', () => {
    it('populates lineups.value with fetched data', async () => {
      const data = [{ id: 'lineup-1' }, { id: 'lineup-2' }]
      vi.stubGlobal('fetch', makeFetchOk(data))
      const { lineups, fetchLineups } = useLineupHistory()
      await fetchLineups(TEAM_ID)
      expect(lineups.value).toEqual(data)
    })

    it('calls GET /api/teams/:teamId/lineups', async () => {
      const fetchMock = makeFetchOk([])
      vi.stubGlobal('fetch', fetchMock)
      const { fetchLineups } = useLineupHistory()
      await fetchLineups(TEAM_ID)
      expect(fetchMock).toHaveBeenCalledWith(`/api/teams/${TEAM_ID}/lineups`, expect.any(Object))
    })

    it('sets error and rethrows on failure', async () => {
      vi.stubGlobal('fetch', makeFetchError(500, 'Server error'))
      const { error, fetchLineups } = useLineupHistory()
      await expect(fetchLineups(TEAM_ID)).rejects.toThrow('Server error')
      expect(error.value).toBe('Server error')
    })
  })

  describe('deleteLineup()', () => {
    it('removes deleted lineup from lineups.value', async () => {
      const data = [{ id: 'lineup-1' }, { id: 'lineup-2' }]
      const fetchMock = vi.fn()
        .mockResolvedValueOnce({ ok: true, status: 200, json: async () => data })
        .mockResolvedValueOnce({ ok: true, status: 204, json: async () => null })
      vi.stubGlobal('fetch', fetchMock)

      const { lineups, fetchLineups, deleteLineup } = useLineupHistory()
      await fetchLineups(TEAM_ID)
      await deleteLineup('lineup-1')

      expect(lineups.value).toHaveLength(1)
      expect(lineups.value[0].id).toBe('lineup-2')
    })

    it('calls DELETE /api/lineups/:lineupId', async () => {
      const fetchMock = makeFetchNoContent()
      vi.stubGlobal('fetch', fetchMock)
      const { deleteLineup } = useLineupHistory()
      await deleteLineup('lineup-abc')
      expect(fetchMock).toHaveBeenCalledWith('/api/lineups/lineup-abc', expect.any(Object))
    })

    it('sets error and rethrows on failure', async () => {
      vi.stubGlobal('fetch', makeFetchError(404, '排阵不存在'))
      const { error, deleteLineup } = useLineupHistory()
      await expect(deleteLineup('nonexistent')).rejects.toThrow('排阵不存在')
      expect(error.value).toBe('排阵不存在')
    })

    it('does not modify lineups.value on delete failure', async () => {
      const data = [{ id: 'lineup-1' }]
      const fetchMock = vi.fn()
        .mockResolvedValueOnce({ ok: true, status: 200, json: async () => data })
        .mockResolvedValueOnce({ ok: false, status: 404, json: async () => ({ message: '排阵不存在' }) })
      vi.stubGlobal('fetch', fetchMock)

      const { lineups, fetchLineups, deleteLineup } = useLineupHistory()
      await fetchLineups(TEAM_ID)
      await expect(deleteLineup('lineup-1')).rejects.toThrow()

      expect(lineups.value).toHaveLength(1)
    })
  })
})
