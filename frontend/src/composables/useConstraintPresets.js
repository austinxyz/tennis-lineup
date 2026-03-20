import { ref } from 'vue'
import { useApi } from './useApi'

export function useConstraintPresets() {
  const { loading, error, get, post, del } = useApi()
  const presets = ref([])

  const fetchPresets = async (teamId) => {
    try {
      presets.value = await get(`/api/teams/${teamId}/constraint-presets`)
    } catch (err) {
      console.error('Failed to fetch constraint presets:', err)
      throw err
    }
  }

  const savePreset = async (teamId, name, constraints) => {
    try {
      const preset = await post(`/api/teams/${teamId}/constraint-presets`, {
        name,
        excludePlayers: constraints.excludePlayers || [],
        includePlayers: constraints.includePlayers || [],
        pinPlayers: constraints.pinPlayers || {},
      })
      presets.value = [preset, ...presets.value]
      return preset
    } catch (err) {
      console.error('Failed to save constraint preset:', err)
      throw err
    }
  }

  const deletePreset = async (teamId, presetId) => {
    try {
      await del(`/api/teams/${teamId}/constraint-presets/${presetId}`)
      presets.value = presets.value.filter(p => p.id !== presetId)
    } catch (err) {
      console.error('Failed to delete constraint preset:', err)
      throw err
    }
  }

  return {
    loading,
    error,
    presets,
    fetchPresets,
    savePreset,
    deletePreset,
  }
}
