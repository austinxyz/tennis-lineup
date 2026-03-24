import { ref } from 'vue'
import { useApi } from './useApi'

export function useBatchImport() {
  const { loading, error, post } = useApi()
  const importResult = ref(null)

  const importFromCSV = async (teamId, file) => {
    const formData = new FormData()
    formData.append('file', file)

    try {
      importResult.value = await post(`/api/teams/${teamId}/import`, formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      })
      return importResult.value
    } catch (err) {
      console.error('Failed to import CSV:', err)
      throw err
    }
  }

  const importFromJSON = async (teamId, file) => {
    const formData = new FormData()
    formData.append('file', file)

    try {
      importResult.value = await post(`/api/teams/${teamId}/import`, formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      })
      return importResult.value
    } catch (err) {
      console.error('Failed to import JSON:', err)
      throw err
    }
  }

  return {
    loading,
    error,
    importResult,
    importFromCSV,
    importFromJSON,
  }
}
