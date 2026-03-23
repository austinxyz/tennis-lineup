import { ref } from 'vue'
import { useApi } from './useApi'

export function usePartnerNotes(teamId) {
  const notes = ref([])
  const loading = ref(false)
  const error = ref(null)
  const { get, post, put, del } = useApi()

  async function fetchPartnerNotes() {
    loading.value = true
    error.value = null
    try {
      notes.value = await get(`/api/teams/${teamId}/partner-notes`)
    } catch (err) {
      error.value = err.message
    } finally {
      loading.value = false
    }
  }

  async function savePartnerNote({ player1Id, player2Id, player1Name, player2Name, note }) {
    const result = await post(`/api/teams/${teamId}/partner-notes`, {
      player1Id,
      player2Id,
      player1Name,
      player2Name,
      note,
    })
    await fetchPartnerNotes()
    return result
  }

  async function updatePartnerNote(noteId, note) {
    const result = await put(`/api/teams/${teamId}/partner-notes/${noteId}`, { note })
    await fetchPartnerNotes()
    return result
  }

  async function deletePartnerNote(noteId) {
    await del(`/api/teams/${teamId}/partner-notes/${noteId}`)
    await fetchPartnerNotes()
  }

  async function bulkUpdatePersonalNotes(updates) {
    await fetch(`/api/teams/${teamId}/players/notes`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(updates),
    }).then(r => {
      if (!r.ok) throw new Error(`HTTP ${r.status}`)
    })
  }

  return {
    notes,
    loading,
    error,
    fetchPartnerNotes,
    savePartnerNote,
    updatePartnerNote,
    deletePartnerNote,
    bulkUpdatePersonalNotes,
  }
}
