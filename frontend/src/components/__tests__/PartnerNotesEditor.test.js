import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { ref } from 'vue'
import PartnerNotesEditor from '../PartnerNotesEditor.vue'

const players = [
  { id: 'p1', name: '张三', gender: 'male', utr: 6.0 },
  { id: 'p2', name: '李四', gender: 'male', utr: 5.5 },
  { id: 'p3', name: '王五', gender: 'female', utr: 4.0 },
]

const mockNotes = ref([
  { id: 'pn1', player1Id: 'p1', player2Id: 'p2', player1Name: '张三', player2Name: '李四',
    note: '默契好', createdAt: '2024-01-01T00:00:00Z', updatedAt: '2024-01-01T00:00:00Z' },
])

const mockSave = vi.fn().mockResolvedValue({})
const mockUpdate = vi.fn().mockResolvedValue({})
const mockDelete = vi.fn().mockResolvedValue(undefined)
const mockFetch = vi.fn().mockResolvedValue(undefined)

vi.mock('../../composables/usePartnerNotes', () => ({
  usePartnerNotes: () => ({
    notes: mockNotes,
    loading: ref(false),
    error: ref(null),
    fetchPartnerNotes: mockFetch,
    savePartnerNote: mockSave,
    updatePartnerNote: mockUpdate,
    deletePartnerNote: mockDelete,
    bulkUpdatePersonalNotes: vi.fn(),
  }),
}))

function mountEditor(teamId = 'team-1') {
  return mount(PartnerNotesEditor, {
    props: { teamId, players },
  })
}

describe('PartnerNotesEditor', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockSave.mockResolvedValue({})
    mockUpdate.mockResolvedValue({})
    mockDelete.mockResolvedValue(undefined)
    mockFetch.mockResolvedValue(undefined)
    mockNotes.value = [
      { id: 'pn1', player1Id: 'p1', player2Id: 'p2', player1Name: '张三', player2Name: '李四',
        note: '默契好', createdAt: '2024-01-01T00:00:00Z', updatedAt: '2024-01-01T00:00:00Z' },
    ]
  })

  it('renders existing partner notes', async () => {
    const wrapper = mountEditor()
    await flushPromises()
    expect(wrapper.text()).toContain('张三 + 李四')
    expect(wrapper.text()).toContain('默契好')
  })

  it('player B dropdown excludes player A selection', async () => {
    const wrapper = mountEditor()
    await flushPromises()
    const selects = wrapper.findAll('select')
    await selects[0].setValue('p1')
    await flushPromises()

    // Player B options should not include p1
    const optB = selects[1].findAll('option').filter(o => o.element.value !== '')
    expect(optB.every(o => o.element.value !== 'p1')).toBe(true)
  })

  it('add button disabled when fields incomplete', async () => {
    const wrapper = mountEditor()
    await flushPromises()
    const addBtn = wrapper.findAll('button').find(b => b.text().includes('添加搭档笔记'))
    expect(addBtn.element.disabled).toBe(true)
  })

  it('calls savePartnerNote with correct payload', async () => {
    const wrapper = mountEditor('opponent-1')
    await flushPromises()
    const selects = wrapper.findAll('select')
    await selects[0].setValue('p1')
    await selects[1].setValue('p3')
    await wrapper.find('textarea').setValue('搭配稳定')
    const addBtn = wrapper.findAll('button').find(b => b.text().includes('添加搭档笔记'))
    await addBtn.trigger('click')
    await flushPromises()
    expect(mockSave).toHaveBeenCalledWith(
      expect.objectContaining({ player1Id: 'p1', player2Id: 'p3', note: '搭配稳定' })
    )
  })

  it('shows edit and delete buttons on existing notes', async () => {
    const wrapper = mountEditor()
    await flushPromises()
    const editBtn = wrapper.findAll('button').find(b => b.text() === '编辑')
    const delBtn = wrapper.findAll('button').find(b => b.text() === '删除')
    expect(editBtn).toBeTruthy()
    expect(delBtn).toBeTruthy()
  })

  it('clicking edit shows inline editable textarea', async () => {
    const wrapper = mountEditor()
    await flushPromises()
    const editBtn = wrapper.findAll('button').find(b => b.text() === '编辑')
    await editBtn.trigger('click')
    await flushPromises()
    // After clicking edit, the save/cancel buttons should appear
    const saveBtn = wrapper.findAll('button').find(b => b.text() === '保存')
    expect(saveBtn).toBeTruthy()
    // The inline textarea should have the existing note value
    const inlineTextarea = wrapper.findAll('textarea').find(t => t.element.value === '默契好')
    expect(inlineTextarea).toBeTruthy()
  })

  it('works with any teamId (own or opponent)', () => {
    const wrapperOwn = mountEditor('own-team-1')
    const wrapperOpp = mountEditor('opp-team-99')
    expect(wrapperOwn.exists()).toBe(true)
    expect(wrapperOpp.exists()).toBe(true)
  })
})
