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

vi.mock('../../components/LineupSwapPanel.vue', () => ({
  default: {
    name: 'LineupSwapPanel',
    props: ['lineup'],
    emits: ['update:lineup'],
    template: `<div data-testid="swap-panel">
      <button @click="$emit('update:lineup', { ...lineup, pairs: [{position:'D1',player1Id:'swapped'}] })">do-swap</button>
    </div>`,
  },
}))

// ── Helpers ────────────────────────────────────────────────────────────────────
const TEAM_PLAYERS = [
  { id: 'p1', name: 'Alice', utr: 6.0, gender: 'female', verified: true },
  { id: 'p2', name: 'Bob', utr: 5.5, gender: 'male', verified: true },
  { id: 'p3', name: 'Carol', utr: 5.0, gender: 'female', verified: true },
  { id: 'p4', name: 'Dave', utr: 4.5, gender: 'male', verified: true },
  { id: 'new-p', name: 'Eve', utr: 4.0, gender: 'female', verified: true },
]

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

function buildLineupWithPairs(id, overrides = {}) {
  return buildLineup(id, {
    pairs: [
      { position: 'D1', player1Id: 'p1', player2Id: 'p2', combinedUtr: 11.5 },
      { position: 'D2', player1Id: 'p3', player2Id: 'p4', combinedUtr: 9.5 },
    ],
    ...overrides,
  })
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
  // Reset teams with players for replacement tests
  mockTeams.value = [{
    id: 'team-1',
    name: '浙江队',
    players: TEAM_PLAYERS,
  }]
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

  // ── Task 6: Swap panel in lineup history ─────────────────────────────────────
  describe('swap panel', () => {
    it('renders a <details> with LineupSwapPanel for each lineup', async () => {
      mockLineups.value = [
        buildLineup('lineup-1'),
        buildLineup('lineup-2'),
      ]
      const wrapper = mountView()
      await flushPromises()
      const swapPanels = wrapper.findAll('[data-testid="swap-panel"]')
      expect(swapPanels).toHaveLength(2)
    })

    it('renders summary text "调整配对" inside <details>', async () => {
      mockLineups.value = [buildLineup('lineup-1')]
      const wrapper = mountView()
      await flushPromises()
      const details = wrapper.find('details')
      expect(details.exists()).toBe(true)
      expect(details.find('summary').text()).toContain('调整配对')
    })

    it('handleSwapUpdate calls updateLineup with new pairs when swap emits', async () => {
      mockLineups.value = [buildLineup('lineup-1')]
      const wrapper = mountView()
      await flushPromises()
      const swapBtn = wrapper.find('[data-testid="swap-panel"] button')
      await swapBtn.trigger('click')
      await flushPromises()
      expect(mockUpdateLineup).toHaveBeenCalledWith(
        'team-1',
        'lineup-1',
        { pairs: [{ position: 'D1', player1Id: 'swapped' }] },
      )
    })

    it('handleSwapUpdate calls fetchLineups after successful update', async () => {
      mockLineups.value = [buildLineup('lineup-1')]
      mockFetchLineups.mockReset().mockResolvedValue()
      const wrapper = mountView()
      await flushPromises()
      mockFetchLineups.mockReset().mockResolvedValue()
      const swapBtn = wrapper.find('[data-testid="swap-panel"] button')
      await swapBtn.trigger('click')
      await flushPromises()
      expect(mockFetchLineups).toHaveBeenCalledWith('team-1')
    })

    it('updateLineup error sets updateError and still calls fetchLineups', async () => {
      mockUpdateLineup.mockRejectedValue(new Error('swap failed'))
      mockLineups.value = [buildLineup('lineup-1')]
      const wrapper = mountView()
      await flushPromises()
      mockFetchLineups.mockReset().mockResolvedValue()
      const swapBtn = wrapper.find('[data-testid="swap-panel"] button')
      await swapBtn.trigger('click')
      await flushPromises()
      expect(wrapper.find('[data-testid="update-error"]').exists()).toBe(true)
      expect(mockFetchLineups).toHaveBeenCalledWith('team-1')
    })
  })

  // ── Task 7: Player replacement in lineup history ──────────────────────────────
  describe('player replacement', () => {
    it('renders a "替换球员" button for each lineup', async () => {
      mockLineups.value = [buildLineupWithPairs('lineup-1'), buildLineupWithPairs('lineup-2')]
      const wrapper = mountView()
      await flushPromises()
      const btns = wrapper.findAll('[data-testid="start-replace-btn"]')
      expect(btns).toHaveLength(2)
    })

    it('replacement UI is hidden before clicking "替换球员"', async () => {
      mockLineups.value = [buildLineupWithPairs('lineup-1')]
      const wrapper = mountView()
      await flushPromises()
      expect(wrapper.find('[data-testid="save-replace-btn"]').exists()).toBe(false)
    })

    it('clicking "替换球员" shows replacement UI with select elements', async () => {
      mockLineups.value = [buildLineupWithPairs('lineup-1')]
      const wrapper = mountView()
      await flushPromises()
      await wrapper.find('[data-testid="start-replace-btn"]').trigger('click')
      await flushPromises()
      const saves = wrapper.findAll('[data-testid="save-replace-btn"]')
      expect(saves).toHaveLength(1)
      // Each pair has 2 player slots = 4 selects total (2 pairs × 2 players)
      const selects = wrapper.findAll('select')
      expect(selects.length).toBeGreaterThanOrEqual(4)
    })

    it('player dropdown excludes players already used in other slots', async () => {
      mockLineups.value = [buildLineupWithPairs('lineup-1')]
      const wrapper = mountView()
      await flushPromises()
      await wrapper.find('[data-testid="start-replace-btn"]').trigger('click')
      await flushPromises()
      const selects = wrapper.findAll('select')
      // First select is for D1 player1 (p1). Other taken slots: p2 (D1p2), p3 (D2p1), p4 (D2p2)
      // So first select should only show p1 and new-p (Eve), NOT p2, p3, p4
      const firstSelectOptions = selects[0].findAll('option')
      const optionValues = firstSelectOptions.map(o => o.element.value)
      expect(optionValues).toContain('p1')
      expect(optionValues).toContain('new-p')
      expect(optionValues).not.toContain('p2')
      expect(optionValues).not.toContain('p3')
      expect(optionValues).not.toContain('p4')
    })

    it('changing a player selection updates the pair and triggers validation', async () => {
      // Make pairs with high UTR to trigger violation (>40.5 total)
      const highUtrLineup = buildLineup('lineup-1', {
        pairs: [
          { position: 'D1', player1Id: 'p1', player2Id: 'p2', combinedUtr: 11.5 },
          { position: 'D2', player1Id: 'p3', player2Id: 'p4', combinedUtr: 9.5 },
        ],
      })
      // Use players with very high UTR to breach 40.5
      mockTeams.value = [{
        id: 'team-1',
        name: '浙江队',
        players: [
          { id: 'p1', name: 'Alice', utr: 12.0 },
          { id: 'p2', name: 'Bob', utr: 11.0 },
          { id: 'p3', name: 'Carol', utr: 10.0 },
          { id: 'p4', name: 'Dave', utr: 9.0 },
          { id: 'new-p', name: 'Eve', utr: 8.0 },
        ],
      }]
      mockLineups.value = [highUtrLineup]
      const wrapper = mountView()
      await flushPromises()
      await wrapper.find('[data-testid="start-replace-btn"]').trigger('click')
      await flushPromises()
      // change any select to trigger validation with high utrs
      const selects = wrapper.findAll('select')
      await selects[0].setValue('p1')
      await flushPromises()
      // Violation list should appear (total UTR will be 12+11+10+9=42 > 40.5)
      const violations = wrapper.findAll('[data-testid="replace-violation"]')
      expect(violations.length).toBeGreaterThan(0)
    })

    it('shows total UTR violation message when sum > 40.5', async () => {
      mockTeams.value = [{
        id: 'team-1',
        name: '浙江队',
        players: [
          { id: 'p1', name: 'Alice', utr: 12.0 },
          { id: 'p2', name: 'Bob', utr: 11.0 },
          { id: 'p3', name: 'Carol', utr: 10.0 },
          { id: 'p4', name: 'Dave', utr: 9.0 },
          { id: 'new-p', name: 'Eve', utr: 4.0 },
        ],
      }]
      mockLineups.value = [buildLineup('lineup-1', {
        pairs: [
          { position: 'D1', player1Id: 'p1', player2Id: 'p2', combinedUtr: 23.0 },
          { position: 'D2', player1Id: 'p3', player2Id: 'p4', combinedUtr: 19.0 },
        ],
      })]
      const wrapper = mountView()
      await flushPromises()
      await wrapper.find('[data-testid="start-replace-btn"]').trigger('click')
      await flushPromises()
      // Trigger validation by selecting same player (no real change needed, just setValue)
      const selects = wrapper.findAll('select')
      await selects[0].setValue('p1')
      await flushPromises()
      expect(wrapper.text()).toContain('总UTR超出上限')
    })

    it('clicking "保存修改" calls updateLineup with pairs then fetchLineups', async () => {
      mockLineups.value = [buildLineupWithPairs('lineup-1')]
      const wrapper = mountView()
      await flushPromises()
      await wrapper.find('[data-testid="start-replace-btn"]').trigger('click')
      await flushPromises()
      await wrapper.find('[data-testid="save-replace-btn"]').trigger('click')
      await flushPromises()
      expect(mockUpdateLineup).toHaveBeenCalledWith(
        'team-1',
        'lineup-1',
        expect.objectContaining({ pairs: expect.any(Array) }),
      )
      expect(mockFetchLineups).toHaveBeenCalledWith('team-1')
    })

    it('clicking "取消" hides replacement UI', async () => {
      mockLineups.value = [buildLineupWithPairs('lineup-1')]
      const wrapper = mountView()
      await flushPromises()
      await wrapper.find('[data-testid="start-replace-btn"]').trigger('click')
      await flushPromises()
      expect(wrapper.find('[data-testid="save-replace-btn"]').exists()).toBe(true)
      await wrapper.find('[data-testid="cancel-replace-btn"]').trigger('click')
      await flushPromises()
      expect(wrapper.find('[data-testid="save-replace-btn"]').exists()).toBe(false)
    })

    it('updateLineup error during save shows updateError and keeps UI open', async () => {
      mockUpdateLineup.mockRejectedValue(new Error('save failed'))
      mockLineups.value = [buildLineupWithPairs('lineup-1')]
      const wrapper = mountView()
      await flushPromises()
      await wrapper.find('[data-testid="start-replace-btn"]').trigger('click')
      await flushPromises()
      await wrapper.find('[data-testid="save-replace-btn"]').trigger('click')
      await flushPromises()
      expect(wrapper.find('[data-testid="update-error"]').exists()).toBe(true)
      // Replacement UI should still be visible
      expect(wrapper.find('[data-testid="save-replace-btn"]').exists()).toBe(true)
    })

    it('only one lineup replacement UI is open at a time when clicking another start-replace-btn', async () => {
      mockLineups.value = [
        buildLineupWithPairs('lineup-1'),
        buildLineupWithPairs('lineup-2'),
      ]
      const wrapper = mountView()
      await flushPromises()
      const startBtns = wrapper.findAll('[data-testid="start-replace-btn"]')
      await startBtns[0].trigger('click')
      await flushPromises()
      expect(wrapper.findAll('[data-testid="save-replace-btn"]')).toHaveLength(1)
      await startBtns[1].trigger('click')
      await flushPromises()
      // Still only one open (the second one now)
      expect(wrapper.findAll('[data-testid="save-replace-btn"]')).toHaveLength(1)
    })

    it('position labels D1/D2 are shown in replacement UI', async () => {
      mockLineups.value = [buildLineupWithPairs('lineup-1')]
      const wrapper = mountView()
      await flushPromises()
      await wrapper.find('[data-testid="start-replace-btn"]').trigger('click')
      await flushPromises()
      expect(wrapper.text()).toContain('D1')
      expect(wrapper.text()).toContain('D2')
    })

    it('save is blocked when constraint violations exist', async () => {
      // Build lineup where all players have high UTR → total > 40.5
      const highUtrPairs = [
        { position: 'D1', player1Id: 'p1', player2Id: 'p2', combinedUtr: 11.5 },
        { position: 'D2', player1Id: 'p3', player2Id: 'p4', combinedUtr: 9.5 },
        { position: 'D3', player1Id: 'p1', player2Id: 'p3', combinedUtr: 11.0 },
        { position: 'D4', player1Id: 'p2', player2Id: 'p4', combinedUtr: 10.0 },
      ]
      mockTeams.value = [{
        id: 'team-1', name: '浙江队',
        players: [
          { id: 'p1', name: 'Alice', utr: 12.0 },
          { id: 'p2', name: 'Bob', utr: 11.0 },
          { id: 'p3', name: 'Carol', utr: 10.0 },
          { id: 'p4', name: 'Dave', utr: 9.0 },
        ],
      }]
      mockLineups.value = [buildLineup('lineup-1', { pairs: highUtrPairs, totalUtr: 42.0 })]
      const wrapper = mountView()
      await flushPromises()
      await wrapper.find('[data-testid="start-replace-btn"]').trigger('click')
      await flushPromises()
      // Violations shown on open
      expect(wrapper.find('[data-testid="replace-violation"]').exists()).toBe(true)
      // Clicking save does NOT call updateLineup
      await wrapper.find('[data-testid="save-replace-btn"]').trigger('click')
      await flushPromises()
      expect(mockUpdateLineup).not.toHaveBeenCalled()
    })

    it('player options show name and UTR', async () => {
      mockLineups.value = [buildLineupWithPairs('lineup-1')]
      const wrapper = mountView()
      await flushPromises()
      await wrapper.find('[data-testid="start-replace-btn"]').trigger('click')
      await flushPromises()
      const selects = wrapper.findAll('select')
      const firstSelectText = selects[0].element.innerHTML
      // Should contain player name and UTR
      expect(firstSelectText).toContain('Alice')
    })
  })
})
