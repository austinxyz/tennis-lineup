import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import PlayerPartnerNotesRow from '../PlayerPartnerNotesRow.vue'

const players = [
  { id: 'p1', name: '张三', gender: 'male', utr: 7.0 },
  { id: 'p2', name: '李四', gender: 'male', utr: 6.5 },
  { id: 'p3', name: '王五', gender: 'female', utr: 5.0 },
  { id: 'p4', name: '赵六', gender: 'male', utr: 4.5 },
]

const existingNotes = [
  { id: 'n1', player1Id: 'p1', player2Id: 'p2', player1Name: '张三', player2Name: '李四', note: '默契好' },
]

const mockDel = vi.fn().mockResolvedValue(undefined)
const mockPut = vi.fn().mockResolvedValue({})
const mockPost = vi.fn().mockResolvedValue({})

vi.mock('../../composables/useApi', () => ({
  useApi: () => ({
    get: vi.fn(),
    post: mockPost,
    put: mockPut,
    del: mockDel,
  }),
}))

function mountRow(props = {}) {
  return mount(PlayerPartnerNotesRow, {
    props: {
      teamId: 'team-1',
      playerId: 'p1',
      playerName: '张三',
      players,
      notes: existingNotes,
      ...props,
    },
  })
}

describe('PlayerPartnerNotesRow', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockDel.mockResolvedValue(undefined)
    mockPut.mockResolvedValue({})
    mockPost.mockResolvedValue({})
  })

  it('renders existing note rows', async () => {
    const wrapper = mountRow()
    await flushPromises()
    // Should show partner name in dropdown and note in input
    const inputs = wrapper.findAll('input')
    expect(inputs[0].element.value).toBe('默契好')
  })

  it('dropdown excludes current player', async () => {
    const wrapper = mountRow({ notes: [] })
    await flushPromises()
    // The blank row select options should not include p1 (current player)
    const selects = wrapper.findAll('select')
    const options = selects[0].findAll('option').map(o => o.element.value)
    expect(options).not.toContain('p1')
  })

  it('dropdown excludes already-used partners', async () => {
    const wrapper = mountRow()
    await flushPromises()
    // p2 is used in existingNotes; blank row dropdown should not include p2
    const selects = wrapper.findAll('select')
    const blankSelect = selects[selects.length - 1]
    const options = blankSelect.findAll('option').map(o => o.element.value)
    expect(options).not.toContain('p2')
  })

  it('clicking ✕ removes row from UI', async () => {
    const wrapper = mountRow()
    await flushPromises()
    const deleteBtn = wrapper.findAll('button').find(b => b.text() === '✕')
    await deleteBtn.trigger('click')
    await flushPromises()
    // Row should be gone from visible rows
    const inputs = wrapper.findAll('input')
    // Only the blank row input should remain
    expect(inputs).toHaveLength(1)
  })

  it('emits saved after successful save', async () => {
    const wrapper = mountRow({ notes: [] })
    await flushPromises()
    // Fill blank row
    const selects = wrapper.findAll('select')
    await selects[0].setValue('p2')
    const inputs = wrapper.findAll('input')
    await inputs[0].setValue('新搭档笔记')
    const saveBtn = wrapper.findAll('button').find(b => b.text().includes('保存'))
    await saveBtn.trigger('click')
    await flushPromises()
    expect(wrapper.emitted('saved')).toBeTruthy()
  })

  it('sends DELETE for ✕-ed rows on save', async () => {
    const wrapper = mountRow()
    await flushPromises()
    const deleteBtn = wrapper.findAll('button').find(b => b.text() === '✕')
    await deleteBtn.trigger('click')
    const saveBtn = wrapper.findAll('button').find(b => b.text().includes('保存'))
    await saveBtn.trigger('click')
    await flushPromises()
    expect(mockDel).toHaveBeenCalledWith('/api/teams/team-1/partner-notes/n1')
  })

  it('sends PUT for changed note text', async () => {
    const wrapper = mountRow()
    await flushPromises()
    const inputs = wrapper.findAll('input')
    await inputs[0].setValue('改过的备注')
    const saveBtn = wrapper.findAll('button').find(b => b.text().includes('保存'))
    await saveBtn.trigger('click')
    await flushPromises()
    expect(mockPut).toHaveBeenCalledWith(
      '/api/teams/team-1/partner-notes/n1',
      { note: '改过的备注' }
    )
  })

  it('sends POST for filled blank row', async () => {
    const wrapper = mountRow({ notes: [] })
    await flushPromises()
    const selects = wrapper.findAll('select')
    await selects[0].setValue('p2')
    const inputs = wrapper.findAll('input')
    await inputs[0].setValue('好搭档')
    const saveBtn = wrapper.findAll('button').find(b => b.text().includes('保存'))
    await saveBtn.trigger('click')
    await flushPromises()
    expect(mockPost).toHaveBeenCalledWith(
      '/api/teams/team-1/partner-notes',
      expect.objectContaining({ player1Id: 'p1', player2Id: 'p2', note: '好搭档' })
    )
  })

  it('shows error message on save failure', async () => {
    mockPost.mockRejectedValue(new Error('网络错误'))
    const wrapper = mountRow({ notes: [] })
    await flushPromises()
    const selects = wrapper.findAll('select')
    await selects[0].setValue('p2')
    const inputs = wrapper.findAll('input')
    await inputs[0].setValue('好搭档')
    const saveBtn = wrapper.findAll('button').find(b => b.text().includes('保存'))
    await saveBtn.trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('保存部分失败')
    expect(wrapper.emitted('saved')).toBeFalsy()
  })

  it('re-initializes state when notes prop changes', async () => {
    const wrapper = mountRow({ notes: [] })
    await flushPromises()
    // Initially no input rows for existing notes
    expect(wrapper.findAll('input')).toHaveLength(1)
    // Update prop
    await wrapper.setProps({ notes: existingNotes })
    await flushPromises()
    // Now should show the existing note
    const inputs = wrapper.findAll('input')
    expect(inputs[0].element.value).toBe('默契好')
  })

  it('emits cancel when cancel button is clicked', async () => {
    const wrapper = mountRow()
    await flushPromises()
    const cancelBtn = wrapper.findAll('button').find(b => b.text() === '取消')
    await cancelBtn.trigger('click')
    expect(wrapper.emitted('cancel')).toBeTruthy()
  })
})
