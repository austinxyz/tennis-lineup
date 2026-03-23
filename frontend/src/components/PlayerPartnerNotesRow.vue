<template>
  <div class="px-4 py-3 bg-purple-50 border-t border-purple-100 ml-7">
    <div class="text-xs font-semibold text-purple-700 uppercase tracking-wide mb-3">搭档笔记</div>

    <!-- Existing note rows -->
    <div
      v-for="row in localRows"
      :key="row.id"
      class="flex items-center gap-2 mb-2"
    >
      <select
        v-model="row.partnerId"
        :disabled="saving"
        class="px-2 py-1 border border-gray-300 rounded-md text-sm min-w-[88px] focus:outline-none focus:ring-2 focus:ring-purple-400 disabled:opacity-50"
      >
        <option v-for="p in partnerOptions(row.id)" :key="p.id" :value="p.id">{{ p.name }}</option>
      </select>
      <input
        v-model="row.note"
        :disabled="saving"
        class="flex-1 max-w-md px-2 py-1 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-purple-400 disabled:opacity-50"
      />
      <button
        @click="markDelete(row.id)"
        :disabled="saving"
        class="text-gray-300 hover:text-red-400 transition-colors disabled:opacity-50 text-base leading-none px-1"
      >✕</button>
    </div>

    <!-- Blank add row -->
    <div class="flex items-center gap-2 mb-3">
      <select
        v-model="newRow.partnerId"
        :disabled="saving"
        class="px-2 py-1 border border-dashed border-purple-300 rounded-md text-sm min-w-[88px] text-gray-400 focus:outline-none focus:ring-2 focus:ring-purple-400 disabled:opacity-50"
      >
        <option value="">选搭档…</option>
        <option v-for="p in newRowOptions" :key="p.id" :value="p.id">{{ p.name }}</option>
      </select>
      <input
        v-model="newRow.note"
        :disabled="saving"
        placeholder="添加笔记…"
        class="flex-1 max-w-md px-2 py-1 border border-dashed border-purple-300 rounded-md text-sm text-gray-400 placeholder-gray-300 focus:outline-none focus:ring-2 focus:ring-purple-400 disabled:opacity-50"
      />
      <div class="w-5"></div>
    </div>

    <!-- Error message -->
    <div v-if="saveError" class="text-sm text-red-600 mb-2">{{ saveError }}</div>

    <!-- Action buttons -->
    <div class="flex gap-2">
      <button
        @click="save"
        :disabled="saving"
        class="bg-purple-600 text-white px-4 py-1.5 rounded-md text-sm hover:bg-purple-700 disabled:opacity-50 transition-colors flex items-center gap-1.5"
      >
        <svg v-if="saving" class="animate-spin h-3.5 w-3.5" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z"/>
        </svg>
        {{ saving ? '保存中…' : '保存' }}
      </button>
      <button
        @click="$emit('cancel')"
        :disabled="saving"
        class="border border-gray-300 text-gray-600 px-3 py-1.5 rounded-md text-sm hover:bg-gray-50 disabled:opacity-50 transition-colors"
      >取消</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useApi } from '../composables/useApi'

const props = defineProps({
  teamId:     { type: String, required: true },
  playerId:   { type: String, required: true },
  playerName: { type: String, required: true },
  players:    { type: Array,  required: true },
  notes:      { type: Array,  required: true },
})

const emit = defineEmits(['saved', 'cancel'])

const { post, put, del } = useApi()

const localRows = ref([])   // { id, partnerId, note, originalNote }
const deletedIds = ref(new Set())
const newRow = ref({ partnerId: '', note: '' })
const saving = ref(false)
const saveError = ref('')

function partnerNameById(id) {
  return props.players.find(p => p.id === id)?.name ?? id
}

function reinitLocalState(notes) {
  deletedIds.value = new Set()
  newRow.value = { partnerId: '', note: '' }
  saveError.value = ''
  localRows.value = (notes ?? []).map(n => {
    const partnerId = n.player1Id === props.playerId ? n.player2Id : n.player1Id
    return { id: n.id, partnerId, note: n.note, originalNote: n.note }
  })
}

watch(() => props.notes, reinitLocalState, { immediate: true })

// IDs of partners already claimed by visible rows (for exclusion)
const usedPartnerIds = computed(() => {
  const ids = new Set([props.playerId])
  localRows.value.forEach(r => { if (r.partnerId) ids.add(r.partnerId) })
  return ids
})

// options for an existing row (can keep its current partner)
function partnerOptions(rowId) {
  const row = localRows.value.find(r => r.id === rowId)
  return props.players.filter(p => {
    if (p.id === props.playerId) return false
    if (p.id === row?.partnerId) return true  // keep current selection
    if (usedPartnerIds.value.has(p.id)) return false
    if (newRow.value.partnerId === p.id) return false
    return true
  })
}

// options for the blank new row
const newRowOptions = computed(() =>
  props.players.filter(p =>
    p.id !== props.playerId && !usedPartnerIds.value.has(p.id)
  )
)

function markDelete(rowId) {
  deletedIds.value = new Set([...deletedIds.value, rowId])
  localRows.value = localRows.value.filter(r => r.id !== rowId)
}

async function save() {
  saving.value = true
  saveError.value = ''
  const errors = []

  // DELETE removed rows
  for (const id of deletedIds.value) {
    try {
      await del(`/api/teams/${props.teamId}/partner-notes/${id}`)
    } catch (e) {
      errors.push(`删除失败: ${e.message}`)
    }
  }

  // PUT changed rows
  for (const row of localRows.value) {
    if (row.note !== row.originalNote) {
      try {
        await put(`/api/teams/${props.teamId}/partner-notes/${row.id}`, { note: row.note })
        row.originalNote = row.note
      } catch (e) {
        errors.push(`更新失败: ${e.message}`)
      }
    }
  }

  // POST new row if filled
  if (newRow.value.partnerId && newRow.value.note.trim()) {
    const partner = props.players.find(p => p.id === newRow.value.partnerId)
    try {
      await post(`/api/teams/${props.teamId}/partner-notes`, {
        player1Id:   props.playerId,
        player2Id:   partner.id,
        player1Name: props.playerName,
        player2Name: partner.name,
        note:        newRow.value.note.trim(),
      })
    } catch (e) {
      errors.push(`添加失败: ${e.message}`)
    }
  }

  saving.value = false

  if (errors.length) {
    saveError.value = '保存部分失败，请检查'
  } else {
    emit('saved')
  }
}
</script>
