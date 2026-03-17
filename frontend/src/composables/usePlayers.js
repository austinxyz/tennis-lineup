import { ref } from 'vue'
import { useApi } from './useApi'

export function usePlayers(teamId) {
  const { loading, error, get, post, put, del } = useApi()
  const players = ref([])

  const fetchPlayers = async () => {
    try {
      players.value = await get(`/api/teams/${teamId}/players`)
    } catch (err) {
      console.error('Failed to fetch players:', err)
      throw err
    }
  }

  const addPlayer = async (playerData) => {
    try {
      const newPlayer = await post(`/api/teams/${teamId}/players`, playerData)
      players.value.push(newPlayer)
      return newPlayer
    } catch (err) {
      console.error('Failed to add player:', err)
      throw err
    }
  }

  const updatePlayer = async (playerId, playerData) => {
    try {
      const updatedPlayer = await put(`/api/teams/${teamId}/players/${playerId}`, playerData)
      const index = players.value.findIndex(p => p.id === playerId)
      if (index !== -1) {
        players.value[index] = updatedPlayer
      }
      return updatedPlayer
    } catch (err) {
      console.error('Failed to update player:', err)
      throw err
    }
  }

  const deletePlayer = async (playerId) => {
    if (!confirm('确定要删除这个球员吗？此操作不可撤销。')) {
      return
    }

    try {
      await del(`/api/teams/${teamId}/players/${playerId}`)
      players.value = players.value.filter(p => p.id !== playerId)
    } catch (err) {
      console.error('Failed to delete player:', err)
      throw err
    }
  }

  const bulkUpdateUtrs = async (changes) => {
    // changes: [{ playerId, utr }] — only changed players
    if (!changes.length) return { succeeded: [], failed: [] }

    const results = await Promise.allSettled(
      changes.map(({ playerId, utr }) =>
        put(`/api/teams/${teamId}/players/${playerId}`, { utr })
          .then(updated => ({ playerId, updated }))
      )
    )

    const succeeded = []
    const failed = []
    results.forEach((result, i) => {
      if (result.status === 'fulfilled') {
        const { playerId, updated } = result.value
        const index = players.value.findIndex(p => p.id === playerId)
        if (index !== -1) players.value[index] = { ...players.value[index], ...updated }
        succeeded.push(playerId)
      } else {
        failed.push({ playerId: changes[i].playerId, message: result.reason?.message || '更新失败' })
      }
    })

    return { succeeded, failed }
  }

  return {
    loading,
    error,
    players,
    fetchPlayers,
    addPlayer,
    updatePlayer,
    deletePlayer,
    bulkUpdateUtrs,
  }
}