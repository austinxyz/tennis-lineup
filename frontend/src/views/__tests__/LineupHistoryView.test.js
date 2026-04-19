import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { ref } from 'vue'
import LineupHistoryView from '../LineupHistoryView.vue'

// ── Mock state ─────────────────────────────────────────────────────────────────
const mockLineups = ref([])
const mockTeams = ref([{ id: 'team-1', name: '浙江队' }])
const mockLoading = ref(false)
const mockFetchLineups = vi.fn()
const mockDeleteLineup = vi.fn()
const mockExportLineups = vi.fn()
const mockImportLineups = vi.fn()
const mockUpdateLineup = vi.fn()

// ── Module mocks ───────────────────────────────────────────────────────────────
vi.mock('vue-router', () => ({
  useRoute: vi.fn(() => ({ params: { id: 'team-1' } })),
}))

vi.mock('../../composables/useLineupHistory', () => ({
  useLineupHistory: vi.fn(() => ({
    loading: mockLoading,
    lineups: mockLineups,
    fetchLineups: mockFetchLineups,
    deleteLineup: mockDeleteLineup,
    exportLineups: mockExportLineups,
    importLineups: mockImportLineups,
    updateLineup: mockUpdateLineup,
  })),
}))

vi.mock('../../composables/useTeams', () => ({
  useTeams: vi.fn(() => ({
    teams: mockTeams,
    fetchTeams: vi.fn().mockResolvedValue(),
  })),
}))

vi.mock('../../components/LineupCard.vue', () => ({
  default: {
    name: 'LineupCard',
    props: ['lineup', 'showPlayerUtr'],
    template: '<div data-testid="lineup-card">{{ lineup.id }}</div>',
  },
}))

// ── Helpers ────────────────────────────────────────────────────────────────────
function buildLineup(id, overrides = {}) {
  return {
    id,
    strategy: 'balanced',
    totalUtr: 38.0,
    createdAt: '2026-01-01T10:00:00Z',
    pairs: [],
    sortOrder: 0,
    ...overrides,
  }
}

function mountView() {
  return mount(LineupHistoryView, {
    global: {
      stubs: { RouterLink: true },
    },
  })
}

beforeEach(() => {
  mockLineups.value = []
  mockLoading.value = false
  mockFetchLineups.mockReset().mockResolvedValue()
  mockDeleteLineup.mockReset()
  mockUpdateLineup.mockReset().mockResolvedValue()
  vi.spyOn(window, 'confirm').mockReturnValue(true)
})

// ── Tests ──────────────────────────────────────────────────────────────────────
describe('LineupHistoryView', () => {
  it('calls fetchLineups with teamId on mount', async () => {
    mountView()
    await flushPromises()
    expect(mockFetchLineups).toHaveBeenCalledWith('team-1')
  })

  it('shows empty state when no lineups', async () => {
    mockLineups.value = []
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('暂无保存的排阵')
  })

  it('renders a LineupCard for each lineup', async () => {
    mockLineups.value = [buildLineup('lineup-1'), buildLineup('lineup-2')]
    const wrapper = mountView()
    await flushPromises()
    const cards = wrapper.findAll('[data-testid="lineup-card"]')
    expect(cards).toHaveLength(2)
  })

  it('does not show empty state when lineups exist', async () => {
    mockLineups.value = [buildLineup('lineup-1')]
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).not.toContain('暂无保存的排阵')
  })

  it('calls deleteLineup and removes card on delete success', async () => {
    mockLineups.value = [buildLineup('lineup-1'), buildLineup('lineup-2')]
    mockDeleteLineup.mockResolvedValue()
    const wrapper = mountView()
    await flushPromises()

    const deleteBtn = wrapper.findAll('button').find(b => b.text() === '删除')
    await deleteBtn.trigger('click')
    await flushPromises()

    expect(mockDeleteLineup).toHaveBeenCalledWith('lineup-1')
  })

  it('shows error message when delete fails', async () => {
    mockLineups.value = [buildLineup('lineup-1')]
    mockDeleteLineup.mockRejectedValue(new Error('排阵不存在'))
    const wrapper = mountView()
    await flushPromises()

    const deleteBtn = wrapper.findAll('button').find(b => b.text() === '删除')
    await deleteBtn.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('排阵不存在')
  })

  it('does not call deleteLineup if confirm is cancelled', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false)
    mockLineups.value = [buildLineup('lineup-1')]
    const wrapper = mountView()
    await flushPromises()

    const deleteBtn = wrapper.findAll('button').find(b => b.text() === '删除')
    await deleteBtn.trigger('click')
    await flushPromises()

    expect(mockDeleteLineup).not.toHaveBeenCalled()
  })

  // ── Task 4.1: Inline label editing ──────────────────────────────────────────
  describe('inline label editing', () => {
    it('shows lineup.label as display name when label is set', async () => {
      mockLineups.value = [buildLineup('lineup-1', { label: 'My Label' })]
      const wrapper = mountView()
      await flushPromises()
      expect(wrapper.text()).toContain('My Label')
    })

    it('falls back to lineup.strategy when label is absent', async () => {
      mockLineups.value = [buildLineup('lineup-1', { strategy: 'aggressive' })]
      const wrapper = mountView()
      await flushPromises()
      expect(wrapper.text()).toContain('aggressive')
    })

    it('shows a pencil button next to the name', async () => {
      mockLineups.value = [buildLineup('lineup-1', { label: 'Test' })]
      const wrapper = mountView()
      await flushPromises()
      const editBtn = wrapper.find('[data-testid="label-edit-btn"]')
      expect(editBtn.exists()).toBe(true)
    })

    it('clicking pencil button enters edit mode showing an input', async () => {
      mockLineups.value = [buildLineup('lineup-1', { label: 'Original' })]
      const wrapper = mountView()
      await flushPromises()
      const editBtn = wrapper.find('[data-testid="label-edit-btn"]')
      await editBtn.trigger('click')
      const input = wrapper.find('[data-testid="label-input"]')
      expect(input.exists()).toBe(true)
      expect(input.element.value).toBe('Original')
    })

    it('clicking name text enters edit mode', async () => {
      mockLineups.value = [buildLineup('lineup-1', { label: 'Clickable' })]
      const wrapper = mountView()
      await flushPromises()
      const nameSpan = wrapper.find('[data-testid="lineup-label"]')
      await nameSpan.trigger('click')
      const input = wrapper.find('[data-testid="label-input"]')
      expect(input.exists()).toBe(true)
    })

    it('on Enter calls updateLineup and fetchLineups', async () => {
      mockLineups.value = [buildLineup('lineup-1', { label: 'Old', sortOrder: 0 })]
      const wrapper = mountView()
      await flushPromises()
      const editBtn = wrapper.find('[data-testid="label-edit-btn"]')
      await editBtn.trigger('click')
      const input = wrapper.find('[data-testid="label-input"]')
      await input.setValue('New Label')
      await input.trigger('keydown', { key: 'Enter' })
      await flushPromises()
      expect(mockUpdateLineup).toHaveBeenCalledWith('team-1', 'lineup-1', { label: 'New Label' })
      expect(mockFetchLineups).toHaveBeenCalledWith('team-1')
    })

    it('on blur calls updateLineup and fetchLineups', async () => {
      mockLineups.value = [buildLineup('lineup-1', { label: 'Old' })]
      const wrapper = mountView()
      await flushPromises()
      const editBtn = wrapper.find('[data-testid="label-edit-btn"]')
      await editBtn.trigger('click')
      const input = wrapper.find('[data-testid="label-input"]')
      await input.setValue('Blurred Label')
      await input.trigger('blur')
      await flushPromises()
      expect(mockUpdateLineup).toHaveBeenCalledWith('team-1', 'lineup-1', { label: 'Blurred Label' })
    })

    it('on Escape cancels without calling updateLineup', async () => {
      mockLineups.value = [buildLineup('lineup-1', { label: 'Unchanged' })]
      const wrapper = mountView()
      await flushPromises()
      const editBtn = wrapper.find('[data-testid="label-edit-btn"]')
      await editBtn.trigger('click')
      const input = wrapper.find('[data-testid="label-input"]')
      await input.setValue('Discarded')
      await input.trigger('keydown', { key: 'Escape' })
      await flushPromises()
      expect(mockUpdateLineup).not.toHaveBeenCalled()
    })

    it('sends empty string when input is cleared', async () => {
      mockLineups.value = [buildLineup('lineup-1', { label: 'Has Label' })]
      const wrapper = mountView()
      await flushPromises()
      const editBtn = wrapper.find('[data-testid="label-edit-btn"]')
      await editBtn.trigger('click')
      const input = wrapper.find('[data-testid="label-input"]')
      await input.setValue('')
      await input.trigger('keydown', { key: 'Enter' })
      await flushPromises()
      expect(mockUpdateLineup).toHaveBeenCalledWith('team-1', 'lineup-1', { label: '' })
    })

    it('hides input and shows name after saving', async () => {
      mockLineups.value = [buildLineup('lineup-1', { label: 'Test' })]
      const wrapper = mountView()
      await flushPromises()
      await wrapper.find('[data-testid="label-edit-btn"]').trigger('click')
      await wrapper.find('[data-testid="label-input"]').trigger('keydown', { key: 'Enter' })
      await flushPromises()
      expect(wrapper.find('[data-testid="label-input"]').exists()).toBe(false)
    })
  })

  // ── Task 4.2: Inline comment editing ────────────────────────────────────────
  describe('inline comment editing', () => {
    it('shows existing comment text when comment is set', async () => {
      mockLineups.value = [buildLineup('lineup-1', { comment: 'Great pick' })]
      const wrapper = mountView()
      await flushPromises()
      expect(wrapper.text()).toContain('Great pick')
    })

    it('shows "+ 添加备注" link when no comment', async () => {
      mockLineups.value = [buildLineup('lineup-1')]
      const wrapper = mountView()
      await flushPromises()
      expect(wrapper.text()).toContain('+ 添加备注')
    })

    it('clicking comment text enters edit mode with textarea', async () => {
      mockLineups.value = [buildLineup('lineup-1', { comment: 'Click me' })]
      const wrapper = mountView()
      await flushPromises()
      await wrapper.find('[data-testid="lineup-comment"]').trigger('click')
      const textarea = wrapper.find('[data-testid="comment-input"]')
      expect(textarea.exists()).toBe(true)
      expect(textarea.element.value).toBe('Click me')
    })

    it('clicking "+ 添加备注" enters edit mode with empty textarea', async () => {
      mockLineups.value = [buildLineup('lineup-1')]
      const wrapper = mountView()
      await flushPromises()
      await wrapper.find('[data-testid="add-comment-btn"]').trigger('click')
      const textarea = wrapper.find('[data-testid="comment-input"]')
      expect(textarea.exists()).toBe(true)
      expect(textarea.element.value).toBe('')
    })

    it('textarea has rows=2', async () => {
      mockLineups.value = [buildLineup('lineup-1', { comment: 'Test' })]
      const wrapper = mountView()
      await flushPromises()
      await wrapper.find('[data-testid="lineup-comment"]').trigger('click')
      const textarea = wrapper.find('[data-testid="comment-input"]')
      expect(textarea.attributes('rows')).toBe('2')
    })

    it('on blur saves comment via updateLineup', async () => {
      mockLineups.value = [buildLineup('lineup-1', { comment: 'Old comment' })]
      const wrapper = mountView()
      await flushPromises()
      await wrapper.find('[data-testid="lineup-comment"]').trigger('click')
      const textarea = wrapper.find('[data-testid="comment-input"]')
      await textarea.setValue('New comment')
      await textarea.trigger('blur')
      await flushPromises()
      expect(mockUpdateLineup).toHaveBeenCalledWith('team-1', 'lineup-1', { comment: 'New comment' })
    })

    it('on Escape cancels without saving', async () => {
      mockLineups.value = [buildLineup('lineup-1', { comment: 'Keep me' })]
      const wrapper = mountView()
      await flushPromises()
      await wrapper.find('[data-testid="lineup-comment"]').trigger('click')
      const textarea = wrapper.find('[data-testid="comment-input"]')
      await textarea.setValue('Discard this')
      await textarea.trigger('keydown', { key: 'Escape' })
      await flushPromises()
      expect(mockUpdateLineup).not.toHaveBeenCalled()
    })
  })

  // ── Tasks 5.1-5.4: Reorder (up/down) ────────────────────────────────────────
  describe('reorder up/down', () => {
    it('shows ↑ and ↓ buttons for each lineup', async () => {
      mockLineups.value = [
        buildLineup('lineup-1', { sortOrder: 0 }),
        buildLineup('lineup-2', { sortOrder: 1 }),
      ]
      const wrapper = mountView()
      await flushPromises()
      const upBtns = wrapper.findAll('[data-testid="move-up-btn"]')
      const downBtns = wrapper.findAll('[data-testid="move-down-btn"]')
      expect(upBtns).toHaveLength(2)
      expect(downBtns).toHaveLength(2)
    })

    it('disables ↑ button for first lineup', async () => {
      mockLineups.value = [
        buildLineup('lineup-1', { sortOrder: 0 }),
        buildLineup('lineup-2', { sortOrder: 1 }),
      ]
      const wrapper = mountView()
      await flushPromises()
      const upBtns = wrapper.findAll('[data-testid="move-up-btn"]')
      expect(upBtns[0].attributes('disabled')).toBeDefined()
      expect(upBtns[1].attributes('disabled')).toBeUndefined()
    })

    it('disables ↓ button for last lineup', async () => {
      mockLineups.value = [
        buildLineup('lineup-1', { sortOrder: 0 }),
        buildLineup('lineup-2', { sortOrder: 1 }),
      ]
      const wrapper = mountView()
      await flushPromises()
      const downBtns = wrapper.findAll('[data-testid="move-down-btn"]')
      expect(downBtns[1].attributes('disabled')).toBeDefined()
      expect(downBtns[0].attributes('disabled')).toBeUndefined()
    })

    it('clicking ↓ swaps sortOrder with next lineup and refreshes', async () => {
      mockLineups.value = [
        buildLineup('lineup-1', { sortOrder: 0 }),
        buildLineup('lineup-2', { sortOrder: 1 }),
      ]
      const wrapper = mountView()
      await flushPromises()
      const downBtns = wrapper.findAll('[data-testid="move-down-btn"]')
      await downBtns[0].trigger('click')
      await flushPromises()
      expect(mockUpdateLineup).toHaveBeenCalledWith('team-1', 'lineup-1', { sortOrder: 1 })
      expect(mockUpdateLineup).toHaveBeenCalledWith('team-1', 'lineup-2', { sortOrder: 0 })
      expect(mockFetchLineups).toHaveBeenCalledWith('team-1')
    })

    it('clicking ↑ swaps sortOrder with previous lineup and refreshes', async () => {
      mockLineups.value = [
        buildLineup('lineup-1', { sortOrder: 0 }),
        buildLineup('lineup-2', { sortOrder: 1 }),
      ]
      const wrapper = mountView()
      await flushPromises()
      const upBtns = wrapper.findAll('[data-testid="move-up-btn"]')
      await upBtns[1].trigger('click')
      await flushPromises()
      expect(mockUpdateLineup).toHaveBeenCalledWith('team-1', 'lineup-2', { sortOrder: 0 })
      expect(mockUpdateLineup).toHaveBeenCalledWith('team-1', 'lineup-1', { sortOrder: 1 })
      expect(mockFetchLineups).toHaveBeenCalledWith('team-1')
    })

    it('shows ⭐ 首选 badge on the first lineup (index 0)', async () => {
      mockLineups.value = [
        buildLineup('lineup-1', { sortOrder: 0 }),
        buildLineup('lineup-2', { sortOrder: 1 }),
      ]
      const wrapper = mountView()
      await flushPromises()
      expect(wrapper.text()).toContain('⭐ 首选')
    })

    it('shows ⭐ 首选 badge only on first lineup, not second', async () => {
      mockLineups.value = [
        buildLineup('lineup-1', { sortOrder: 0 }),
        buildLineup('lineup-2', { sortOrder: 1 }),
      ]
      const wrapper = mountView()
      await flushPromises()
      const badges = wrapper.findAll('[data-testid="preferred-badge"]')
      expect(badges).toHaveLength(1)
    })

    it('shows ⭐ 首选 badge even when only one lineup exists', async () => {
      mockLineups.value = [buildLineup('lineup-1', { sortOrder: 0 })]
      const wrapper = mountView()
      await flushPromises()
      // Single lineup at index 0 should still show badge
      const badges = wrapper.findAll('[data-testid="preferred-badge"]')
      expect(badges).toHaveLength(1)
    })

    it('disabled ↑ button click does not call updateLineup', async () => {
      mockLineups.value = [
        buildLineup('lineup-1', { sortOrder: 0 }),
        buildLineup('lineup-2', { sortOrder: 1 }),
      ]
      const wrapper = mountView()
      await flushPromises()
      const upBtns = wrapper.findAll('[data-testid="move-up-btn"]')
      await upBtns[0].trigger('click')
      await flushPromises()
      expect(mockUpdateLineup).not.toHaveBeenCalled()
    })

    it('shows updateError when reorder PATCH fails and still refreshes list', async () => {
      mockUpdateLineup.mockRejectedValue(new Error('网络错误'))
      mockLineups.value = [
        buildLineup('lineup-1', { sortOrder: 0 }),
        buildLineup('lineup-2', { sortOrder: 1 }),
      ]
      const wrapper = mountView()
      await flushPromises()
      mockFetchLineups.mockReset().mockResolvedValue()
      const upBtns = wrapper.findAll('[data-testid="move-up-btn"]')
      await upBtns[1].trigger('click')
      await flushPromises()
      expect(wrapper.find('[data-testid="update-error"]').exists()).toBe(true)
      expect(mockFetchLineups).toHaveBeenCalled() // still refreshes
    })

    it('disabled ↓ button click does not call updateLineup', async () => {
      mockLineups.value = [
        buildLineup('lineup-1', { sortOrder: 0 }),
        buildLineup('lineup-2', { sortOrder: 1 }),
      ]
      const wrapper = mountView()
      await flushPromises()
      const downBtns = wrapper.findAll('[data-testid="move-down-btn"]')
      await downBtns[1].trigger('click')
      await flushPromises()
      expect(mockUpdateLineup).not.toHaveBeenCalled()
    })
  })
})
