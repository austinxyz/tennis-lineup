import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { useApi } from '../useApi'

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

function makeFetchNetworkError(message) {
  return vi.fn().mockRejectedValue(new Error(message))
}

describe('useApi', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  describe('get()', () => {
    it('returns parsed JSON on success', async () => {
      vi.stubGlobal('fetch', makeFetchOk({ id: 1 }))
      const { get } = useApi()
      const result = await get('/api/test')
      expect(result).toEqual({ id: 1 })
    })

    it('calls fetch with GET method and Content-Type header', async () => {
      const fetchMock = makeFetchOk({})
      vi.stubGlobal('fetch', fetchMock)
      const { get } = useApi()
      await get('/api/test')
      expect(fetchMock).toHaveBeenCalledWith('/api/test', expect.objectContaining({
        method: 'GET',
        headers: expect.objectContaining({ 'Content-Type': 'application/json' }),
      }))
    })

    it('sets loading to true during request, false after', async () => {
      let loadingDuringRequest
      const fetchMock = vi.fn().mockImplementation(async () => {
        loadingDuringRequest = loading.value
        return { ok: true, json: async () => ({}) }
      })
      vi.stubGlobal('fetch', fetchMock)
      const { loading, get } = useApi()
      expect(loading.value).toBe(false)
      await get('/api/test')
      expect(loadingDuringRequest).toBe(true)
      expect(loading.value).toBe(false)
    })

    it('sets error.value and rethrows on HTTP error', async () => {
      vi.stubGlobal('fetch', makeFetchError(404, 'Not found'))
      const { error, get } = useApi()
      await expect(get('/api/test')).rejects.toThrow('Not found')
      expect(error.value).toBe('Not found')
    })

    it('uses generic message when error JSON has no message', async () => {
      vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
        ok: false,
        status: 500,
        json: vi.fn().mockResolvedValue({}),
      }))
      const { error, get } = useApi()
      await expect(get('/api/test')).rejects.toThrow('HTTP error! status: 500')
      expect(error.value).toBe('HTTP error! status: 500')
    })

    it('sets error.value on network failure', async () => {
      vi.stubGlobal('fetch', makeFetchNetworkError('Network down'))
      const { error, get } = useApi()
      await expect(get('/api/test')).rejects.toThrow('Network down')
      expect(error.value).toBe('Network down')
    })

    it('clears previous error on new request', async () => {
      vi.stubGlobal('fetch', makeFetchError(500, 'Server error'))
      const { error, get } = useApi()
      await expect(get('/api/test')).rejects.toThrow()
      expect(error.value).toBe('Server error')

      vi.stubGlobal('fetch', makeFetchOk({ ok: true }))
      await get('/api/test')
      expect(error.value).toBeNull()
    })
  })

  describe('post()', () => {
    it('calls fetch with POST method and serialized body', async () => {
      const fetchMock = makeFetchOk({ id: 2 })
      vi.stubGlobal('fetch', fetchMock)
      const { post } = useApi()
      const result = await post('/api/test', { name: 'Alice' })
      expect(result).toEqual({ id: 2 })
      expect(fetchMock).toHaveBeenCalledWith('/api/test', expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ name: 'Alice' }),
      }))
    })

    it('sets error on failure', async () => {
      vi.stubGlobal('fetch', makeFetchError(400, 'Bad request'))
      const { error, post } = useApi()
      await expect(post('/api/test', {})).rejects.toThrow('Bad request')
      expect(error.value).toBe('Bad request')
    })
  })

  describe('put()', () => {
    it('calls fetch with PUT method and serialized body', async () => {
      const fetchMock = makeFetchOk({ id: 1, name: 'Updated' })
      vi.stubGlobal('fetch', fetchMock)
      const { put } = useApi()
      const result = await put('/api/test/1', { name: 'Updated' })
      expect(result).toEqual({ id: 1, name: 'Updated' })
      expect(fetchMock).toHaveBeenCalledWith('/api/test/1', expect.objectContaining({
        method: 'PUT',
        body: JSON.stringify({ name: 'Updated' }),
      }))
    })
  })

  describe('patch()', () => {
    it('calls fetch with PATCH method and serialized body', async () => {
      const fetchMock = makeFetchOk({ id: 1, label: 'New Label' })
      vi.stubGlobal('fetch', fetchMock)
      const { patch } = useApi()
      const result = await patch('/api/test/1', { label: 'New Label' })
      expect(result).toEqual({ id: 1, label: 'New Label' })
      expect(fetchMock).toHaveBeenCalledWith('/api/test/1', expect.objectContaining({
        method: 'PATCH',
        body: JSON.stringify({ label: 'New Label' }),
      }))
    })

    it('sets error and rethrows on HTTP error', async () => {
      vi.stubGlobal('fetch', makeFetchError(404, 'Not Found'))
      const { error, patch } = useApi()
      await expect(patch('/api/test/99', {})).rejects.toThrow('Not Found')
      expect(error.value).toBe('Not Found')
    })
  })

  describe('del()', () => {
    it('calls fetch with DELETE method', async () => {
      const fetchMock = makeFetchOk({})
      vi.stubGlobal('fetch', fetchMock)
      const { del } = useApi()
      await del('/api/test/1')
      expect(fetchMock).toHaveBeenCalledWith('/api/test/1', expect.objectContaining({
        method: 'DELETE',
      }))
    })

    it('sets error on failure', async () => {
      vi.stubGlobal('fetch', makeFetchError(403, 'Forbidden'))
      const { error, del } = useApi()
      await expect(del('/api/test/1')).rejects.toThrow('Forbidden')
      expect(error.value).toBe('Forbidden')
    })
  })

  describe('loading state', () => {
    it('resets loading to false even when request fails', async () => {
      vi.stubGlobal('fetch', makeFetchNetworkError('fail'))
      const { loading, get } = useApi()
      await expect(get('/api/test')).rejects.toThrow()
      expect(loading.value).toBe(false)
    })
  })
})
