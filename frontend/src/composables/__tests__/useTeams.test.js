import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { useTeams } from '../useTeams'

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

describe('useTeams', () => {
  describe('fetchTeams()', () => {
    it('populates teams.value with fetched data', async () => {
      const data = [{ id: 1, name: 'Team A' }, { id: 2, name: 'Team B' }]
      vi.stubGlobal('fetch', makeFetchOk(data))
      const { teams, fetchTeams } = useTeams()
      await fetchTeams()
      expect(teams.value).toEqual(data)
    })

    it('sets error and rethrows on failure', async () => {
      vi.stubGlobal('fetch', makeFetchError(500, 'Server error'))
      const { error, fetchTeams } = useTeams()
      await expect(fetchTeams()).rejects.toThrow('Server error')
      expect(error.value).toBe('Server error')
    })
  })

  describe('fetchTeamById()', () => {
    it('sets team.value with fetched data', async () => {
      const data = { id: 1, name: 'Team A' }
      vi.stubGlobal('fetch', makeFetchOk(data))
      const { team, fetchTeamById } = useTeams()
      await fetchTeamById(1)
      expect(team.value).toEqual(data)
    })

    it('calls GET /api/teams/:id', async () => {
      const fetchMock = makeFetchOk({ id: 5, name: 'Team E' })
      vi.stubGlobal('fetch', fetchMock)
      const { fetchTeamById } = useTeams()
      await fetchTeamById(5)
      expect(fetchMock).toHaveBeenCalledWith('/api/teams/5', expect.any(Object))
    })

    it('sets error and rethrows on failure', async () => {
      vi.stubGlobal('fetch', makeFetchError(404, 'Not found'))
      const { error, fetchTeamById } = useTeams()
      await expect(fetchTeamById(99)).rejects.toThrow('Not found')
      expect(error.value).toBe('Not found')
    })
  })

  describe('createTeam()', () => {
    it('prepends new team to teams.value', async () => {
      const existing = [{ id: 1, name: 'Old Team' }]
      const newTeam = { id: 2, name: 'New Team' }

      const fetchMock = vi.fn()
        // First call: fetchTeams
        .mockResolvedValueOnce({ ok: true, json: async () => existing })
        // Second call: createTeam
        .mockResolvedValueOnce({ ok: true, json: async () => newTeam })
      vi.stubGlobal('fetch', fetchMock)

      const { teams, fetchTeams, createTeam } = useTeams()
      await fetchTeams()
      const result = await createTeam('New Team')

      expect(result).toEqual(newTeam)
      expect(teams.value[0]).toEqual(newTeam)
      expect(teams.value).toHaveLength(2)
    })

    it('calls POST /api/teams with name', async () => {
      const fetchMock = makeFetchOk({ id: 3, name: 'Team C' })
      vi.stubGlobal('fetch', fetchMock)
      const { createTeam } = useTeams()
      await createTeam('Team C')
      expect(fetchMock).toHaveBeenCalledWith('/api/teams', expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ name: 'Team C' }),
      }))
    })

    it('sets error and rethrows on failure', async () => {
      vi.stubGlobal('fetch', makeFetchError(400, 'Invalid name'))
      const { error, createTeam } = useTeams()
      await expect(createTeam('')).rejects.toThrow('Invalid name')
      expect(error.value).toBe('Invalid name')
    })
  })

  describe('updateTeam()', () => {
    it('updates matching team in teams.value', async () => {
      const initial = [{ id: 1, name: 'Old' }, { id: 2, name: 'Keep' }]
      const updated = { id: 1, name: 'Updated' }

      const fetchMock = vi.fn()
        .mockResolvedValueOnce({ ok: true, json: async () => initial })
        .mockResolvedValueOnce({ ok: true, json: async () => updated })
      vi.stubGlobal('fetch', fetchMock)

      const { teams, fetchTeams, updateTeam } = useTeams()
      await fetchTeams()
      const result = await updateTeam(1, 'Updated')

      expect(result).toEqual(updated)
      expect(teams.value[0]).toEqual(updated)
      expect(teams.value[1]).toEqual({ id: 2, name: 'Keep' })
    })

    it('does not modify teams.value when id not found', async () => {
      const initial = [{ id: 1, name: 'Team A' }]
      const updated = { id: 99, name: 'Ghost' }

      const fetchMock = vi.fn()
        .mockResolvedValueOnce({ ok: true, json: async () => initial })
        .mockResolvedValueOnce({ ok: true, json: async () => updated })
      vi.stubGlobal('fetch', fetchMock)

      const { teams, fetchTeams, updateTeam } = useTeams()
      await fetchTeams()
      await updateTeam(99, 'Ghost')

      expect(teams.value).toEqual(initial)
    })

    it('sets error and rethrows on failure', async () => {
      vi.stubGlobal('fetch', makeFetchError(404, 'Team not found'))
      const { error, updateTeam } = useTeams()
      await expect(updateTeam(1, 'X')).rejects.toThrow('Team not found')
      expect(error.value).toBe('Team not found')
    })
  })

  describe('deleteTeam()', () => {
    it('removes team from teams.value when confirmed', async () => {
      const initial = [{ id: 1, name: 'Team A' }, { id: 2, name: 'Team B' }]
      vi.stubGlobal('confirm', vi.fn().mockReturnValue(true))

      const fetchMock = vi.fn()
        .mockResolvedValueOnce({ ok: true, json: async () => initial })
        .mockResolvedValueOnce({ ok: true, json: async () => ({}) })
      vi.stubGlobal('fetch', fetchMock)

      const { teams, fetchTeams, deleteTeam } = useTeams()
      await fetchTeams()
      await deleteTeam(1)

      expect(teams.value).toEqual([{ id: 2, name: 'Team B' }])
    })

    it('does nothing when confirm returns false', async () => {
      const initial = [{ id: 1, name: 'Team A' }]
      vi.stubGlobal('confirm', vi.fn().mockReturnValue(false))
      const fetchMock = makeFetchOk(initial)
      vi.stubGlobal('fetch', fetchMock)

      const { teams, fetchTeams, deleteTeam } = useTeams()
      await fetchTeams()
      const callCountBefore = fetchMock.mock.calls.length
      await deleteTeam(1)

      expect(teams.value).toEqual(initial)
      expect(fetchMock.mock.calls.length).toBe(callCountBefore)
    })

    it('sets error and rethrows on API failure when confirmed', async () => {
      vi.stubGlobal('confirm', vi.fn().mockReturnValue(true))
      vi.stubGlobal('fetch', makeFetchError(500, 'Delete failed'))
      const { error, deleteTeam } = useTeams()
      await expect(deleteTeam(1)).rejects.toThrow('Delete failed')
      expect(error.value).toBe('Delete failed')
    })

    it('passes the correct confirmation message', async () => {
      const confirmMock = vi.fn().mockReturnValue(false)
      vi.stubGlobal('confirm', confirmMock)
      vi.stubGlobal('fetch', makeFetchOk([]))
      const { deleteTeam } = useTeams()
      await deleteTeam(1)
      expect(confirmMock).toHaveBeenCalledWith('确定要删除这个队伍吗？此操作不可撤销。')
    })
  })

  describe('loading state', () => {
    it('is true during fetch and false after', async () => {
      let loadingDuringRequest
      const { loading, fetchTeams } = useTeams()
      const fetchMock = vi.fn().mockImplementation(async () => {
        loadingDuringRequest = loading.value
        return { ok: true, json: async () => [] }
      })
      vi.stubGlobal('fetch', fetchMock)
      await fetchTeams()
      expect(loadingDuringRequest).toBe(true)
      expect(loading.value).toBe(false)
    })
  })
})
