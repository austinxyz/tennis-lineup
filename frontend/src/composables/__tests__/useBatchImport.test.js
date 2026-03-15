import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { useBatchImport } from '../useBatchImport'

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

function makeFile(name, type) {
  return new File(['content'], name, { type })
}

beforeEach(() => {
  vi.spyOn(console, 'error').mockImplementation(() => {})
})

afterEach(() => {
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

describe('useBatchImport', () => {
  describe('importFromCSV()', () => {
    it('sets importResult.value on success and returns the result', async () => {
      const mockResult = { imported: 5, errors: [] }
      vi.stubGlobal('fetch', makeFetchOk(mockResult))
      const { importResult, importFromCSV } = useBatchImport()
      const file = makeFile('teams.csv', 'text/csv')
      const result = await importFromCSV(file)
      expect(result).toEqual(mockResult)
      expect(importResult.value).toEqual(mockResult)
    })

    it('calls POST /api/teams/import', async () => {
      const fetchMock = makeFetchOk({ imported: 1 })
      vi.stubGlobal('fetch', fetchMock)
      const { importFromCSV } = useBatchImport()
      await importFromCSV(makeFile('teams.csv', 'text/csv'))
      expect(fetchMock).toHaveBeenCalledWith('/api/teams/import', expect.any(Object))
    })

    it('sends file as FormData', async () => {
      const fetchMock = makeFetchOk({ imported: 1 })
      vi.stubGlobal('fetch', fetchMock)
      const { importFromCSV } = useBatchImport()
      const file = makeFile('teams.csv', 'text/csv')
      await importFromCSV(file)
      const [, options] = fetchMock.mock.calls[0]
      expect(options.body).toBeInstanceOf(FormData)
      expect(options.body.get('file')).toBe(file)
    })

    it('sets error and rethrows on failure', async () => {
      vi.stubGlobal('fetch', makeFetchError(400, 'Invalid CSV'))
      const { error, importFromCSV } = useBatchImport()
      await expect(importFromCSV(makeFile('bad.csv', 'text/csv'))).rejects.toThrow('Invalid CSV')
      expect(error.value).toBe('Invalid CSV')
    })

    it('does not modify importResult.value on failure', async () => {
      vi.stubGlobal('fetch', makeFetchError(500, 'Server error'))
      const { importResult, importFromCSV } = useBatchImport()
      await expect(importFromCSV(makeFile('teams.csv', 'text/csv'))).rejects.toThrow()
      expect(importResult.value).toBeNull()
    })
  })

  describe('importFromJSON()', () => {
    it('sets importResult.value on success and returns the result', async () => {
      const mockResult = { imported: 3, errors: [] }
      vi.stubGlobal('fetch', makeFetchOk(mockResult))
      const { importResult, importFromJSON } = useBatchImport()
      const file = makeFile('teams.json', 'application/json')
      const result = await importFromJSON(file)
      expect(result).toEqual(mockResult)
      expect(importResult.value).toEqual(mockResult)
    })

    it('calls POST /api/teams/import', async () => {
      const fetchMock = makeFetchOk({ imported: 2 })
      vi.stubGlobal('fetch', fetchMock)
      const { importFromJSON } = useBatchImport()
      await importFromJSON(makeFile('teams.json', 'application/json'))
      expect(fetchMock).toHaveBeenCalledWith('/api/teams/import', expect.any(Object))
    })

    it('sends file as FormData', async () => {
      const fetchMock = makeFetchOk({ imported: 2 })
      vi.stubGlobal('fetch', fetchMock)
      const { importFromJSON } = useBatchImport()
      const file = makeFile('teams.json', 'application/json')
      await importFromJSON(file)
      const [, options] = fetchMock.mock.calls[0]
      expect(options.body).toBeInstanceOf(FormData)
      expect(options.body.get('file')).toBe(file)
    })

    it('sets error and rethrows on failure', async () => {
      vi.stubGlobal('fetch', makeFetchError(400, 'Invalid JSON'))
      const { error, importFromJSON } = useBatchImport()
      await expect(importFromJSON(makeFile('bad.json', 'application/json'))).rejects.toThrow('Invalid JSON')
      expect(error.value).toBe('Invalid JSON')
    })
  })

  describe('loading state', () => {
    it('is true during import and false after success', async () => {
      let loadingDuringRequest
      const { loading, importFromCSV } = useBatchImport()
      const fetchMock = vi.fn().mockImplementation(async () => {
        loadingDuringRequest = loading.value
        return { ok: true, json: async () => ({ imported: 1 }) }
      })
      vi.stubGlobal('fetch', fetchMock)
      await importFromCSV(makeFile('teams.csv', 'text/csv'))
      expect(loadingDuringRequest).toBe(true)
      expect(loading.value).toBe(false)
    })

    it('resets to false after failure', async () => {
      vi.stubGlobal('fetch', makeFetchError(500, 'fail'))
      const { loading, importFromCSV } = useBatchImport()
      await expect(importFromCSV(makeFile('teams.csv', 'text/csv'))).rejects.toThrow()
      expect(loading.value).toBe(false)
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
      const fetchMock = vi.fn()
        .mockResolvedValueOnce({ ok: true, json: async () => first })
        .mockResolvedValueOnce({ ok: true, json: async () => second })
      vi.stubGlobal('fetch', fetchMock)

      const { importResult, importFromCSV, importFromJSON } = useBatchImport()
      await importFromCSV(makeFile('a.csv', 'text/csv'))
      expect(importResult.value).toEqual(first)
      await importFromJSON(makeFile('b.json', 'application/json'))
      expect(importResult.value).toEqual(second)
    })
  })
})
