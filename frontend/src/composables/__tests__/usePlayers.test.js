import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { usePlayers } from '../usePlayers'

const TEAM_ID = 42

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

describe('usePlayers', () => {
  describe('fetchPlayers()', () => {
    it('populates players.value with fetched data', async () => {
      const data = [{ id: 1, name: 'Alice' }, { id: 2, name: 'Bob' }]
      vi.stubGlobal('fetch', makeFetchOk(data))
      const { players, fetchPlayers } = usePlayers(TEAM_ID)
      await fetchPlayers()
      expect(players.value).toEqual(data)
    })

    it('calls GET /api/teams/:teamId/players', async () => {
      const fetchMock = makeFetchOk([])
      vi.stubGlobal('fetch', fetchMock)
      const { fetchPlayers } = usePlayers(TEAM_ID)
      await fetchPlayers()
      expect(fetchMock).toHaveBeenCalledWith(`/api/teams/${TEAM_ID}/players`, expect.any(Object))
    })

    it('sets error and rethrows on failure', async () => {
      vi.stubGlobal('fetch', makeFetchError(500, 'Server error'))
      const { error, fetchPlayers } = usePlayers(TEAM_ID)
      await expect(fetchPlayers()).rejects.toThrow('Server error')
      expect(error.value).toBe('Server error')
    })
  })

  describe('addPlayer()', () => {
    it('appends new player to players.value', async () => {
      const existing = [{ id: 1, name: 'Alice' }]
      const newPlayer = { id: 2, name: 'Bob' }

      const fetchMock = vi.fn()
        .mockResolvedValueOnce({ ok: true, json: async () => existing })
        .mockResolvedValueOnce({ ok: true, json: async () => newPlayer })
      vi.stubGlobal('fetch', fetchMock)

      const { players, fetchPlayers, addPlayer } = usePlayers(TEAM_ID)
      await fetchPlayers()
      const result = await addPlayer({ name: 'Bob' })

      expect(result).toEqual(newPlayer)
      expect(players.value).toHaveLength(2)
      expect(players.value[1]).toEqual(newPlayer)
    })

    it('calls POST /api/teams/:teamId/players with playerData', async () => {
      const fetchMock = makeFetchOk({ id: 3, name: 'Carol' })
      vi.stubGlobal('fetch', fetchMock)
      const { addPlayer } = usePlayers(TEAM_ID)
      await addPlayer({ name: 'Carol', ranking: 5 })
      expect(fetchMock).toHaveBeenCalledWith(
        `/api/teams/${TEAM_ID}/players`,
        expect.objectContaining({
          method: 'POST',
          body: JSON.stringify({ name: 'Carol', ranking: 5 }),
        })
      )
    })

    it('sets error and rethrows on failure', async () => {
      vi.stubGlobal('fetch', makeFetchError(400, 'Invalid player data'))
      const { error, addPlayer } = usePlayers(TEAM_ID)
      await expect(addPlayer({})).rejects.toThrow('Invalid player data')
      expect(error.value).toBe('Invalid player data')
    })
  })

  describe('updatePlayer()', () => {
    it('updates the matching player in players.value', async () => {
      const initial = [{ id: 1, name: 'Alice' }, { id: 2, name: 'Bob' }]
      const updated = { id: 1, name: 'Alice Updated' }

      const fetchMock = vi.fn()
        .mockResolvedValueOnce({ ok: true, json: async () => initial })
        .mockResolvedValueOnce({ ok: true, json: async () => updated })
      vi.stubGlobal('fetch', fetchMock)

      const { players, fetchPlayers, updatePlayer } = usePlayers(TEAM_ID)
      await fetchPlayers()
      const result = await updatePlayer(1, { name: 'Alice Updated' })

      expect(result).toEqual(updated)
      expect(players.value[0]).toEqual(updated)
      expect(players.value[1]).toEqual({ id: 2, name: 'Bob' })
    })

    it('calls PUT /api/teams/:teamId/players/:playerId', async () => {
      const fetchMock = makeFetchOk({ id: 1, name: 'Updated' })
      vi.stubGlobal('fetch', fetchMock)
      const { updatePlayer } = usePlayers(TEAM_ID)
      await updatePlayer(1, { name: 'Updated' })
      expect(fetchMock).toHaveBeenCalledWith(
        `/api/teams/${TEAM_ID}/players/1`,
        expect.objectContaining({ method: 'PUT' })
      )
    })

    it('does not modify players.value when playerId not found', async () => {
      const initial = [{ id: 1, name: 'Alice' }]
      const updated = { id: 99, name: 'Ghost' }

      const fetchMock = vi.fn()
        .mockResolvedValueOnce({ ok: true, json: async () => initial })
        .mockResolvedValueOnce({ ok: true, json: async () => updated })
      vi.stubGlobal('fetch', fetchMock)

      const { players, fetchPlayers, updatePlayer } = usePlayers(TEAM_ID)
      await fetchPlayers()
      await updatePlayer(99, { name: 'Ghost' })

      expect(players.value).toEqual(initial)
    })

    it('sets error and rethrows on failure', async () => {
      vi.stubGlobal('fetch', makeFetchError(404, 'Player not found'))
      const { error, updatePlayer } = usePlayers(TEAM_ID)
      await expect(updatePlayer(1, {})).rejects.toThrow('Player not found')
      expect(error.value).toBe('Player not found')
    })
  })

  describe('deletePlayer()', () => {
    it('removes player from players.value when confirmed', async () => {
      const initial = [{ id: 1, name: 'Alice' }, { id: 2, name: 'Bob' }]
      vi.stubGlobal('confirm', vi.fn().mockReturnValue(true))

      const fetchMock = vi.fn()
        .mockResolvedValueOnce({ ok: true, json: async () => initial })
        .mockResolvedValueOnce({ ok: true, json: async () => ({}) })
      vi.stubGlobal('fetch', fetchMock)

      const { players, fetchPlayers, deletePlayer } = usePlayers(TEAM_ID)
      await fetchPlayers()
      await deletePlayer(1)

      expect(players.value).toEqual([{ id: 2, name: 'Bob' }])
    })

    it('does nothing when confirm returns false', async () => {
      const initial = [{ id: 1, name: 'Alice' }]
      vi.stubGlobal('confirm', vi.fn().mockReturnValue(false))
      const fetchMock = makeFetchOk(initial)
      vi.stubGlobal('fetch', fetchMock)

      const { players, fetchPlayers, deletePlayer } = usePlayers(TEAM_ID)
      await fetchPlayers()
      const callsBefore = fetchMock.mock.calls.length
      await deletePlayer(1)

      expect(players.value).toEqual(initial)
      expect(fetchMock.mock.calls.length).toBe(callsBefore)
    })

    it('sets error and rethrows on API failure when confirmed', async () => {
      vi.stubGlobal('confirm', vi.fn().mockReturnValue(true))
      vi.stubGlobal('fetch', makeFetchError(500, 'Delete failed'))
      const { error, deletePlayer } = usePlayers(TEAM_ID)
      await expect(deletePlayer(1)).rejects.toThrow('Delete failed')
      expect(error.value).toBe('Delete failed')
    })

    it('passes the correct confirmation message', async () => {
      const confirmMock = vi.fn().mockReturnValue(false)
      vi.stubGlobal('confirm', confirmMock)
      vi.stubGlobal('fetch', makeFetchOk([]))
      const { deletePlayer } = usePlayers(TEAM_ID)
      await deletePlayer(1)
      expect(confirmMock).toHaveBeenCalledWith('确定要删除这个球员吗？此操作不可撤销。')
    })

    it('calls DELETE /api/teams/:teamId/players/:playerId', async () => {
      vi.stubGlobal('confirm', vi.fn().mockReturnValue(true))
      const fetchMock = makeFetchOk({})
      vi.stubGlobal('fetch', fetchMock)
      const { deletePlayer } = usePlayers(TEAM_ID)
      await deletePlayer(7)
      expect(fetchMock).toHaveBeenCalledWith(
        `/api/teams/${TEAM_ID}/players/7`,
        expect.objectContaining({ method: 'DELETE' })
      )
    })
  })

  describe('bulkUpdateUtrs()', () => {
    it('returns empty result without making requests when no changes', async () => {
      const fetchMock = vi.fn()
      vi.stubGlobal('fetch', fetchMock)
      const { bulkUpdateUtrs } = usePlayers(TEAM_ID)
      const result = await bulkUpdateUtrs([])
      expect(fetchMock).not.toHaveBeenCalled()
      expect(result).toEqual({ succeeded: [], failed: [] })
    })

    it('updates all players on full success', async () => {
      const initial = [{ id: 'p1', name: 'Alice', utr: 8.0 }, { id: 'p2', name: 'Bob', utr: 7.0 }]
      const updated1 = { id: 'p1', name: 'Alice', utr: 8.50 }
      const updated2 = { id: 'p2', name: 'Bob', utr: 7.25 }

      const fetchMock = vi.fn()
        .mockResolvedValueOnce({ ok: true, json: async () => initial })
        .mockResolvedValueOnce({ ok: true, json: async () => updated1 })
        .mockResolvedValueOnce({ ok: true, json: async () => updated2 })
      vi.stubGlobal('fetch', fetchMock)

      const { players, fetchPlayers, bulkUpdateUtrs } = usePlayers(TEAM_ID)
      await fetchPlayers()
      const result = await bulkUpdateUtrs([{ playerId: 'p1', utr: 8.50 }, { playerId: 'p2', utr: 7.25 }])

      expect(result.succeeded).toEqual(['p1', 'p2'])
      expect(result.failed).toEqual([])
      expect(players.value[0].utr).toBe(8.50)
      expect(players.value[1].utr).toBe(7.25)
    })

    it('returns failed list when some requests fail', async () => {
      const initial = [{ id: 'p1', name: 'Alice', utr: 8.0 }, { id: 'p2', name: 'Bob', utr: 7.0 }]
      const updated1 = { id: 'p1', name: 'Alice', utr: 8.50 }

      const fetchMock = vi.fn()
        .mockResolvedValueOnce({ ok: true, json: async () => initial })
        .mockResolvedValueOnce({ ok: true, json: async () => updated1 })
        .mockResolvedValueOnce({ ok: false, status: 500, json: async () => ({ message: '服务器错误' }) })
      vi.stubGlobal('fetch', fetchMock)

      const { players, fetchPlayers, bulkUpdateUtrs } = usePlayers(TEAM_ID)
      await fetchPlayers()
      const result = await bulkUpdateUtrs([{ playerId: 'p1', utr: 8.50 }, { playerId: 'p2', utr: 7.25 }])

      expect(result.succeeded).toEqual(['p1'])
      expect(result.failed).toHaveLength(1)
      expect(result.failed[0].playerId).toBe('p2')
      expect(players.value[0].utr).toBe(8.50)
      expect(players.value[1].utr).toBe(7.0) // unchanged
    })
  })

  describe('loading state', () => {
    it('is true during fetch and false after', async () => {
      let loadingDuringRequest
      const { loading, fetchPlayers } = usePlayers(TEAM_ID)
      const fetchMock = vi.fn().mockImplementation(async () => {
        loadingDuringRequest = loading.value
        return { ok: true, json: async () => [] }
      })
      vi.stubGlobal('fetch', fetchMock)
      await fetchPlayers()
      expect(loadingDuringRequest).toBe(true)
      expect(loading.value).toBe(false)
    })
  })
})
