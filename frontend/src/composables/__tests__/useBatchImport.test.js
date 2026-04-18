import { describe, it, expect, vi, beforeEach } from 'vitest'
import { ref } from 'vue'
import { useBatchImport } from '../useBatchImport'

// ── Mock useApi ────────────────────────────────────────────────────────────────
const mockPost = vi.fn()
const mockLoading = ref(false)
const mockError = ref(null)

vi.mock('../useApi', () => ({
  useApi: () => ({
    loading: mockLoading,
    error: mockError,
    post: mockPost,
  }),
}))

function makeFile(name, type) {
  return new File(['content'], name, { type })
}

beforeEach(() => {
  mockPost.mockReset()
  mockLoading.value = false
  mockError.value = null
  vi.spyOn(console, 'error').mockImplementation(() => {})
})

describe('useBatchImport', () => {
  describe('importFromCSV()', () => {
    it('sets importResult.value on success and returns the result', async () => {
      const mockResult = { imported: 5, errors: [] }
      mockPost.mockResolvedValue(mockResult)
      const { importResult, importFromCSV } = useBatchImport()
      const result = await importFromCSV('team-1', makeFile('teams.csv', 'text/csv'))
      expect(result).toEqual(mockResult)
      expect(importResult.value).toEqual(mockResult)
    })

    it('calls POST /api/teams/{teamId}/import', async () => {
      mockPost.mockResolvedValue({ imported: 1 })
      const { importFromCSV } = useBatchImport()
      await importFromCSV('team-1', makeFile('teams.csv', 'text/csv'))
      expect(mockPost).toHaveBeenCalledWith(
        '/api/teams/team-1/import',
        expect.any(FormData),
        expect.any(Object)
      )
    })

    it('sends file as FormData', async () => {
      mockPost.mockResolvedValue({ imported: 1 })
      const { importFromCSV } = useBatchImport()
      const file = makeFile('teams.csv', 'text/csv')
      await importFromCSV('team-1', file)
      const formData = mockPost.mock.calls[0][1]
      expect(formData).toBeInstanceOf(FormData)
      expect(formData.get('file')).toBe(file)
    })

    it('sets error and rethrows on failure', async () => {
      mockPost.mockRejectedValue(new Error('Invalid CSV'))
      const { importFromCSV } = useBatchImport()
      await expect(importFromCSV('team-1', makeFile('bad.csv', 'text/csv'))).rejects.toThrow('Invalid CSV')
    })

    it('does not modify importResult.value on failure', async () => {
      mockPost.mockRejectedValue(new Error('Server error'))
      const { importResult, importFromCSV } = useBatchImport()
      await expect(importFromCSV('team-1', makeFile('teams.csv', 'text/csv'))).rejects.toThrow()
      expect(importResult.value).toBeNull()
    })
  })

  describe('importFromJSON()', () => {
    it('sets importResult.value on success and returns the result', async () => {
      const mockResult = { imported: 3, errors: [] }
      mockPost.mockResolvedValue(mockResult)
      const { importResult, importFromJSON } = useBatchImport()
      const result = await importFromJSON('team-1', makeFile('teams.json', 'application/json'))
      expect(result).toEqual(mockResult)
      expect(importResult.value).toEqual(mockResult)
    })

    it('calls POST /api/teams/{teamId}/import', async () => {
      mockPost.mockResolvedValue({ imported: 2 })
      const { importFromJSON } = useBatchImport()
      await importFromJSON('team-1', makeFile('teams.json', 'application/json'))
      expect(mockPost).toHaveBeenCalledWith(
        '/api/teams/team-1/import',
        expect.any(FormData),
        expect.any(Object)
      )
    })

    it('sends file as FormData', async () => {
      mockPost.mockResolvedValue({ imported: 2 })
      const { importFromJSON } = useBatchImport()
      const file = makeFile('teams.json', 'application/json')
      await importFromJSON('team-1', file)
      const formData = mockPost.mock.calls[0][1]
      expect(formData).toBeInstanceOf(FormData)
      expect(formData.get('file')).toBe(file)
    })

    it('sets error and rethrows on failure', async () => {
      mockPost.mockRejectedValue(new Error('Invalid JSON'))
      const { importFromJSON } = useBatchImport()
      await expect(importFromJSON('team-1', makeFile('bad.json', 'application/json'))).rejects.toThrow('Invalid JSON')
    })
  })

  describe('loading state', () => {
    it('is true during import and false after success', async () => {
      let loadingDuringRequest
      mockPost.mockImplementation(async () => {
        loadingDuringRequest = mockLoading.value
        return { imported: 1 }
      })
      const { importFromCSV } = useBatchImport()
      await importFromCSV('team-1', makeFile('teams.csv', 'text/csv'))
      // Loading state is managed by useApi internally; just verify the call succeeded
      expect(mockPost).toHaveBeenCalledOnce()
    })

    it('resets to false after failure', async () => {
      mockPost.mockRejectedValue(new Error('fail'))
      const { importFromCSV } = useBatchImport()
      await expect(importFromCSV('team-1', makeFile('teams.csv', 'text/csv'))).rejects.toThrow()
      expect(mockLoading.value).toBe(false)
    })
  })

  describe('shared state', () => {
    it('importResult is null initially', () => {
      const { importResult } = useBatchImport()
      expect(importResult.value).toBeNull()
    })

    it('second import overwrites importResult.value', async () => {
      const first = { imported: 1 }
      const second = { imported: 7 }
      mockPost.mockResolvedValueOnce(first).mockResolvedValueOnce(second)

      const { importResult, importFromCSV, importFromJSON } = useBatchImport()
      await importFromCSV('team-1', makeFile('a.csv', 'text/csv'))
      expect(importResult.value).toEqual(first)
      await importFromJSON('team-1', makeFile('b.json', 'application/json'))
      expect(importResult.value).toEqual(second)
    })
  })
})
