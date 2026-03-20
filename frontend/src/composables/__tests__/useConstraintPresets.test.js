import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { useConstraintPresets } from '../useConstraintPresets'

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

function makeDeleteOk() {
  return vi.fn().mockResolvedValue({ ok: true, status: 204, json: vi.fn().mockResolvedValue(null) })
}

beforeEach(() => {
  vi.spyOn(console, 'error').mockImplementation(() => {})
})

afterEach(() => {
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

describe('useConstraintPresets', () => {
  describe('fetchPresets()', () => {
    it('sets presets.value with fetched data', async () => {
      const mockData = [
        { id: 'preset-1', name: '全攻', excludePlayers: [], includePlayers: [], pinPlayers: {} },
      ]
      vi.stubGlobal('fetch', makeFetchOk(mockData))
      const { presets, fetchPresets } = useConstraintPresets()
      await fetchPresets('team-1')
      expect(presets.value).toEqual(mockData)
    })

    it('calls GET /api/teams/{teamId}/constraint-presets', async () => {
      const mockFetch = makeFetchOk([])
      vi.stubGlobal('fetch', mockFetch)
      const { fetchPresets } = useConstraintPresets()
      await fetchPresets('team-42')
      expect(mockFetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/teams/team-42/constraint-presets'),
        expect.anything()
      )
    })

    it('throws and logs error on failure', async () => {
      vi.stubGlobal('fetch', makeFetchError(404, '队伍不存在'))
      const { fetchPresets } = useConstraintPresets()
      await expect(fetchPresets('unknown')).rejects.toThrow()
      expect(console.error).toHaveBeenCalled()
    })
  })

  describe('savePreset()', () => {
    it('posts to correct URL and prepends new preset', async () => {
      const newPreset = { id: 'preset-2', name: '保守', excludePlayers: [], includePlayers: [], pinPlayers: {} }
      const mockFetch = makeFetchOk(newPreset)
      vi.stubGlobal('fetch', mockFetch)
      const { presets, savePreset } = useConstraintPresets()
      const result = await savePreset('team-1', '保守', {
        excludePlayers: [],
        includePlayers: ['p1'],
        pinPlayers: {},
      })
      expect(result).toEqual(newPreset)
      expect(presets.value[0]).toEqual(newPreset)
      expect(mockFetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/teams/team-1/constraint-presets'),
        expect.objectContaining({
          method: 'POST',
          body: expect.stringContaining('"name":"保守"'),
        })
      )
    })

    it('sends includePlayers and excludePlayers in body', async () => {
      const newPreset = { id: 'preset-3', name: 'Test' }
      const mockFetch = makeFetchOk(newPreset)
      vi.stubGlobal('fetch', mockFetch)
      const { savePreset } = useConstraintPresets()
      await savePreset('team-1', 'Test', {
        excludePlayers: ['p5'],
        includePlayers: ['p1', 'p2'],
        pinPlayers: { p3: 'D1' },
      })
      const body = JSON.parse(mockFetch.mock.calls[0][1].body)
      expect(body.excludePlayers).toEqual(['p5'])
      expect(body.includePlayers).toEqual(['p1', 'p2'])
      expect(body.pinPlayers).toEqual({ p3: 'D1' })
    })

    it('throws and logs error on failure', async () => {
      vi.stubGlobal('fetch', makeFetchError(500, '服务器错误'))
      const { savePreset } = useConstraintPresets()
      await expect(savePreset('team-1', 'Bad', {})).rejects.toThrow()
      expect(console.error).toHaveBeenCalled()
    })
  })

  describe('deletePreset()', () => {
    it('removes preset from presets.value on success', async () => {
      const instance = useConstraintPresets()
      instance.presets.value = [
        { id: 'preset-1', name: 'Keep' },
        { id: 'preset-2', name: 'Delete Me' },
      ]
      vi.stubGlobal('fetch', makeDeleteOk())
      await instance.deletePreset('team-1', 'preset-2')
      expect(instance.presets.value).toHaveLength(1)
      expect(instance.presets.value[0].id).toBe('preset-1')
    })

    it('calls DELETE /api/teams/{teamId}/constraint-presets/{presetId}', async () => {
      const mockFetch = makeDeleteOk()
      vi.stubGlobal('fetch', mockFetch)
      const { deletePreset } = useConstraintPresets()
      await deletePreset('team-1', 'preset-99')
      expect(mockFetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/teams/team-1/constraint-presets/preset-99'),
        expect.objectContaining({ method: 'DELETE' })
      )
    })

    it('throws and logs error on 404', async () => {
      vi.stubGlobal('fetch', makeFetchError(404, '约束预设不存在'))
      const { deletePreset } = useConstraintPresets()
      await expect(deletePreset('team-1', 'nonexistent')).rejects.toThrow()
      expect(console.error).toHaveBeenCalled()
    })
  })
})
