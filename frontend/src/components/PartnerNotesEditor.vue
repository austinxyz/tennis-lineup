<template>
  <div class="space-y-5">
    <!-- Add new partner note form -->
    <div class="bg-gray-50 rounded-lg p-4 space-y-3 border border-gray-200">
      <h4 class="text-sm font-semibold text-gray-700">添加搭档笔记</h4>
      <div class="grid grid-cols-2 gap-3">
        <div>
          <label class="text-xs text-gray-500 mb-1 block">球员 A</label>
          <select
            v-model="form.player1Id"
            class="w-full px-2 py-1.5 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">选择球员…</option>
            <option v-for="p in players" :key="p.id" :value="p.id">{{ p.name }}</option>
          </select>
        </div>
        <div>
          <label class="text-xs text-gray-500 mb-1 block">球员 B</label>
          <select
            v-model="form.player2Id"
            class="w-full px-2 py-1.5 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">选择球员…</option>
            <option
              v-for="p in playerBOptions"
              :key="p.id"
              :value="p.id"
            >{{ p.name }}</option>
          </select>
        </div>
      </div>
      <div>
        <label class="text-xs text-gray-500 mb-1 block">笔记</label>
        <textarea
          v-model="form.note"
          rows="3"
          placeholder="一起队训、USTA比赛情况、搭档胜率等…"
          class="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
        />
      </div>
      <div v-if="addError" class="text-sm text-red-600">{{ addError }}</div>
      <button
        @click="addNote"
        :disabled="!canAdd || adding"
        class="bg-blue-600 text-white px-4 py-2 rounded-md text-sm hover:bg-blue-700 disabled:opacity-50 transition-colors"
      >
        {{ adding ? '保存中…' : '添加搭档笔记' }}
      </button>
    </div>

    <!-- Existing partner notes list -->
    <div v-if="notes.length === 0" class="text-sm text-gray-400 text-center py-4">
      暂无搭档笔记
    </div>
    <div v-else class="space-y-3">
      <div
        v-for="note in notes"
        :key="note.id"
        class="bg-white rounded-lg border border-gray-200 p-4"
      >
        <div class="flex justify-between items-start gap-2">
          <div class="font-medium text-sm text-gray-800">
            {{ note.player1Name }} + {{ note.player2Name }}
          </div>
          <div class="flex gap-2 shrink-0">
            <button
              v-if="editingId !== note.id"
              @click="startEdit(note)"
              class="text-xs text-indigo-600 hover:text-indigo-800"
            >
              编辑
            </button>
            <button
              @click="removeNote(note.id)"
              class="text-xs text-red-500 hover:text-red-700"
            >
              删除
            </button>
          </div>
        </div>

        <template v-if="editingId === note.id">
          <textarea
            v-model="editText"
            rows="3"
            class="mt-2 w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
          />
          <div class="flex gap-2 mt-2">
            <button
              @click="saveEdit(note.id)"
              :disabled="saving"
              class="bg-blue-600 text-white px-3 py-1 rounded text-xs hover:bg-blue-700 disabled:opacity-50"
            >
              {{ saving ? '保存中…' : '保存' }}
            </button>
            <button
              @click="cancelEdit"
              class="bg-gray-200 text-gray-700 px-3 py-1 rounded text-xs hover:bg-gray-300"
            >
              取消
            </button>
          </div>
        </template>
        <p v-else class="mt-1 text-sm text-gray-600 whitespace-pre-wrap">{{ note.note }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { usePartnerNotes } from '../composables/usePartnerNotes'

const props = defineProps({
  teamId: { type: String, required: true },
  players: { type: Array, required: true },
})

const { notes, loading, fetchPartnerNotes, savePartnerNote, updatePartnerNote, deletePartnerNote } =
  usePartnerNotes(props.teamId)

onMounted(fetchPartnerNotes)

// Add form
const form = ref({ player1Id: '', player2Id: '', note: '' })
const adding = ref(false)
const addError = ref('')

const playerBOptions = computed(() =>
  props.players.filter(p => p.id !== form.value.player1Id)
)

const canAdd = computed(
  () => form.value.player1Id && form.value.player2Id && form.value.note.trim()
)

async function addNote() {
  if (!canAdd.value) return
  adding.value = true
  addError.value = ''
  try {
    const p1 = props.players.find(p => p.id === form.value.player1Id)
    const p2 = props.players.find(p => p.id === form.value.player2Id)
    await savePartnerNote({
      player1Id: form.value.player1Id,
      player2Id: form.value.player2Id,
      player1Name: p1?.name ?? '',
      player2Name: p2?.name ?? '',
      note: form.value.note.trim(),
    })
    form.value = { player1Id: '', player2Id: '', note: '' }
  } catch (err) {
    addError.value = `添加失败: ${err.message}`
  } finally {
    adding.value = false
  }
}

// Inline edit
const editingId = ref(null)
const editText = ref('')
const saving = ref(false)

function startEdit(note) {
  editingId.value = note.id
  editText.value = note.note
}

function cancelEdit() {
  editingId.value = null
  editText.value = ''
}

async function saveEdit(noteId) {
  saving.value = true
  try {
    await updatePartnerNote(noteId, editText.value.trim())
    editingId.value = null
  } catch (err) {
    alert(`保存失败: ${err.message}`)
  } finally {
    saving.value = false
  }
}

async function removeNote(noteId) {
  if (!confirm('确定删除这条搭档笔记吗？')) return
  try {
    await deletePartnerNote(noteId)
  } catch (err) {
    alert(`删除失败: ${err.message}`)
  }
}
</script>
