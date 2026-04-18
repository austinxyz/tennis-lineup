import { ref } from 'vue'
import { useApi } from './useApi'

export function useLineupHistory() {
  const { loading, error, get, del, post } = useApi()
  const lineups = ref([])

  const fetchLineups = async (teamId) => {
    try {
      lineups.value = await get(`/api/teams/${teamId}/lineups`)
    } catch (err) {
      console.error('Failed to fetch lineups:', err)
      throw err
    }
  }

  const deleteLineup = async (lineupId) => {
    try {
      await del(`/api/lineups/${lineupId}`)
      lineups.value = lineups.value.filter(l => l.id !== lineupId)
    } catch (err) {
      console.error('Failed to delete lineup:', err)
      throw err
    }
  }

  const exportLineups = (teamId, teamName) => {
    const date = new Date().toISOString().slice(0, 10)
    const filename = `lineups-${teamName}-${date}.json`
    const a = document.createElement('a')
    a.href = `/api/teams/${teamId}/lineups/export`
    a.download = filename
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
  }

  const importLineups = async (teamId, file) => {
    const formData = new FormData()
    formData.append('file', file)
    return await post(`/api/teams/${teamId}/lineups/import`, formData)
  }

  return {
    loading,
    error,
    lineups,
    fetchLineups,
    deleteLineup,
    exportLineups,
    importLineups,
  }
}
