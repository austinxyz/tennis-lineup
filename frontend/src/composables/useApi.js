import { ref } from 'vue'

export function useApi() {
  const loading = ref(false)
  const error = ref(null)

  const request = async (url, options = {}) => {
    loading.value = true
    error.value = null

    const isFormData = options.body instanceof FormData
    try {
      const response = await fetch(url, {
        headers: isFormData ? {} : {
          'Content-Type': 'application/json',
          ...options.headers,
        },
        ...options,
      })

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}))
        throw new Error(errorData.message || `HTTP error! status: ${response.status}`)
      }

      return await response.json()
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  const get = async (url) => {
    return request(url, { method: 'GET' })
  }

  const post = async (url, data) => {
    return request(url, {
      method: 'POST',
      body: data instanceof FormData ? data : JSON.stringify(data),
    })
  }

  const put = async (url, data) => {
    return request(url, {
      method: 'PUT',
      body: JSON.stringify(data),
    })
  }

  const del = async (url) => {
    return request(url, { method: 'DELETE' })
  }

  return {
    loading,
    error,
    get,
    post,
    put,
    del,
  }
}