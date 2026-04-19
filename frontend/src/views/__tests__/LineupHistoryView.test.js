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

const TEAM_PLAYERS = [
  { id: 'p1', name: 'Alice', utr: 6.0, gender: 'female', verified: true },
  { id: 'p2', name: 'Bob', utr: 5.5, gender: 'male', verified: true },
  { id: 'p3', name: 'Carol', utr: 5.0, gender: 'female', verified: true },
  { id: 'p4', name: 'Dave', utr: 4.5, gender: 'male', verified: true },
  { id: 'new-p', name: 'Eve', utr: 4.0, gender: 'female', verified: true },
]

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
function buildLineup(id, overrides = {}) {
  return { id, strategy: 'balanced', totalUtr: 38.0, createdAt: '2026-01-01T10:00:00Z', pairs: [], sortOrder: 0, ...overrides }
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
  return mount(LineupHistoryView, { global: { stubs: { RouterLink: true } } })
}

beforeEach(() => {
  mockLineups.value = []
  mockLoading.value = false
  mockFetchLineups.mockReset().mockResolvedValue()
  mockDeleteLineup.mockReset()
  mockUpdateLineup.mockReset().mockResolvedValue()
  vi.spyOn(window, 'confirm').mockReturnValue(true)
  mockTeams.value = [{ id: 'team-1', name: '浙江队', players: TEAM_PLAYERS }]
})

// ── Tests ──────────────────────────────────────────────────────────────────────
describe('LineupHistoryView', () => {
  it('calls fetchLineups with teamId on mount', async () => {
    mountView()
    await flushPromises()
    expect(mockFetchLineups).toHaveBeenCalledWith('team-1')
  })

  it('shows empty state when no lineups', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('暂无保存的排阵')
  })

  it('renders a LineupCard for each lineup', async () => {
    mockLineups.value = [buildLineup('lineup-1'), buildLineup('lineup-2')]
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.findAll('[data-testid="lineup-card"]')).toHaveLength(2)
  })

  it('shows lineup name (label || strategy) in card header', async () => {
    mockLineups.value = [buildLineup('lineup-1', { label: 'My Label', strategy: 'balanced' })]
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('[data-testid="lineup-name"]').text()).toBe('My Label')
  })

  it('falls back to strategy when label is absent', async () => {
    mockLineups.value = [buildLineup('lineup-1', { strategy: 'aggressive' })]
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('[data-testid="lineup-name"]').text()).toBe('aggressive')
  })

  it('shows comment below name when present', async () => {
    mockLineups.value = [buildLineup('lineup-1', { comment: '赛前主选' })]
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('[data-testid="lineup-comment-view"]').text()).toBe('赛前主选')
  })

  it('does not show comment element when absent', async () => {
    mockLineups.value = [buildLineup('lineup-1')]
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('[data-testid="lineup-comment-view"]').exists()).toBe(false)
  })

  it('shows ⭐ 首选 badge only on index 0', async () => {
    mockLineups.value = [buildLineup('lineup-1'), buildLineup('lineup-2')]
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.findAll('[data-testid="preferred-badge"]')).toHaveLength(1)
  })

  // ── Delete ─────────────────────────────────────────────────────────────────
  it('calls deleteLineup on delete confirm', async () => {
    mockLineups.value = [buildLineup('lineup-1')]
    mockDeleteLineup.mockResolvedValue()
    const wrapper = mountView()
    await flushPromises()
    await wrapper.findAll('button').find(b => b.text() === '删除').trigger('click')
    await flushPromises()
    expect(mockDeleteLineup).toHaveBeenCalledWith('lineup-1')
  })

  it('does not call deleteLineup if confirm cancelled', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false)
    mockLineups.value = [buildLineup('lineup-1')]
    const wrapper = mountView()
    await flushPromises()
    await wrapper.findAll('button').find(b => b.text() === '删除').trigger('click')
    await flushPromises()
    expect(mockDeleteLineup).not.toHaveBeenCalled()
  })

  it('shows deleteError when delete fails', async () => {
    mockLineups.value = [buildLineup('lineup-1')]
    mockDeleteLineup.mockRejectedValue(new Error('删除失败'))
    const wrapper = mountView()
    await flushPromises()
    await wrapper.findAll('button').find(b => b.text() === '删除').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('删除失败')
  })

  // ── Edit mode ──────────────────────────────────────────────────────────────
  describe('edit mode', () => {
    it('shows 编辑 button per lineup', async () => {
      mockLineups.value = [buildLineup('lineup-1'), buildLineup('lineup-2')]
      const wrapper = mountView()
      await flushPromises()
      expect(wrapper.findAll('[data-testid="edit-btn"]')).toHaveLength(2)
    })

    it('edit panel is hidden before clicking 编辑', async () => {
      mockLineups.value = [buildLineup('lineup-1')]
      const wrapper = mountView()
      await flushPromises()
      expect(wrapper.find('[data-testid="edit-panel"]').exists()).toBe(false)
    })

    it('clicking 编辑 shows edit panel', async () => {
      mockLineups.value = [buildLineup('lineup-1', { label: 'My Label', comment: 'A note' })]
      const wrapper = mountView()
      await flushPromises()
      await wrapper.find('[data-testid="edit-btn"]').trigger('click')
      await flushPromises()
      expect(wrapper.find('[data-testid="edit-panel"]').exists()).toBe(true)
    })

    it('edit panel pre-fills label and comment', async () => {
      mockLineups.value = [buildLineup('lineup-1', { label: 'My Label', comment: 'A note' })]
      const wrapper = mountView()
      await flushPromises()
      await wrapper.find('[data-testid="edit-btn"]').trigger('click')
      await flushPromises()
      expect(wrapper.find('[data-testid="edit-label-input"]').element.value).toBe('My Label')
      expect(wrapper.find('[data-testid="edit-comment-input"]').element.value).toBe('A note')
    })

    it('clicking 取消 closes edit panel without saving', async () => {
      mockLineups.value = [buildLineup('lineup-1')]
      const wrapper = mountView()
      await flushPromises()
      await wrapper.find('[data-testid="edit-btn"]').trigger('click')
      await flushPromises()
      await wrapper.find('[data-testid="cancel-edit-btn"]').trigger('click')
      await flushPromises()
      expect(wrapper.find('[data-testid="edit-panel"]').exists()).toBe(false)
      expect(mockUpdateLineup).not.toHaveBeenCalled()
    })

    it('clicking 保存 calls updateLineup with label and comment then closes panel', async () => {
      mockLineups.value = [buildLineup('lineup-1', { label: 'Old', comment: '' })]
      const wrapper = mountView()
      await flushPromises()
      await wrapper.find('[data-testid="edit-btn"]').trigger('click')
      await flushPromises()
      await wrapper.find('[data-testid="edit-label-input"]').setValue('New Name')
      await wrapper.find('[data-testid="save-edit-btn"]').trigger('click')
      await flushPromises()
      expect(mockUpdateLineup).toHaveBeenCalledWith('team-1', 'lineup-1', expect.objectContaining({ label: 'New Name' }))
      expect(mockFetchLineups).toHaveBeenCalled()
      expect(wrapper.find('[data-testid="edit-panel"]').exists()).toBe(false)
    })

    it('updateLineup failure shows updateError and keeps panel open', async () => {
      mockUpdateLineup.mockRejectedValue(new Error('保存失败'))
      mockLineups.value = [buildLineup('lineup-1')]
      const wrapper = mountView()
      await flushPromises()
      await wrapper.find('[data-testid="edit-btn"]').trigger('click')
      await flushPromises()
      await wrapper.find('[data-testid="save-edit-btn"]').trigger('click')
      await flushPromises()
      expect(wrapper.find('[data-testid="update-error"]').exists()).toBe(true)
      expect(wrapper.find('[data-testid="edit-panel"]').exists()).toBe(true)
    })

    it('only one edit panel open at a time', async () => {
      mockLineups.value = [buildLineup('lineup-1'), buildLineup('lineup-2')]
      const wrapper = mountView()
      await flushPromises()
      const editBtns = wrapper.findAll('[data-testid="edit-btn"]')
      await editBtns[0].trigger('click')
      await flushPromises()
      await editBtns[1].trigger('click')
      await flushPromises()
      expect(wrapper.findAll('[data-testid="edit-panel"]')).toHaveLength(1)
    })
  })

  // ── Swap panel ─────────────────────────────────────────────────────────────
  describe('swap panel in edit mode', () => {
    it('swap panel renders inside edit panel', async () => {
      mockLineups.value = [buildLineup('lineup-1')]
      const wrapper = mountView()
      await flushPromises()
      await wrapper.find('[data-testid="edit-btn"]').trigger('click')
      await flushPromises()
      expect(wrapper.find('[data-testid="swap-panel"]').exists()).toBe(true)
    })

    it('swap emit calls updateLineup and fetchLineups', async () => {
      mockLineups.value = [buildLineup('lineup-1')]
      const wrapper = mountView()
      await flushPromises()
      await wrapper.find('[data-testid="edit-btn"]').trigger('click')
      await flushPromises()
      await wrapper.find('[data-testid="swap-panel"] button').trigger('click')
      await flushPromises()
      expect(mockUpdateLineup).toHaveBeenCalledWith('team-1', 'lineup-1', expect.objectContaining({ pairs: expect.any(Array) }))
      expect(mockFetchLineups).toHaveBeenCalled()
    })

    it('swap updateLineup failure shows updateError and still calls fetchLineups', async () => {
      mockUpdateLineup.mockRejectedValue(new Error('swap failed'))
      mockLineups.value = [buildLineup('lineup-1')]
      const wrapper = mountView()
      await flushPromises()
      await wrapper.find('[data-testid="edit-btn"]').trigger('click')
      await flushPromises()
      mockFetchLineups.mockReset().mockResolvedValue()
      await wrapper.find('[data-testid="swap-panel"] button').trigger('click')
      await flushPromises()
      expect(wrapper.find('[data-testid="update-error"]').exists()).toBe(true)
      expect(mockFetchLineups).toHaveBeenCalled()
    })
  })

  // ── Replace players ────────────────────────────────────────────────────────
  describe('player replacement in edit mode', () => {
    it('shows 选择替换 button inside edit panel', async () => {
      mockLineups.value = [buildLineupWithPairs('lineup-1')]
      const wrapper = mountView()
      await flushPromises()
      await wrapper.find('[data-testid="edit-btn"]').trigger('click')
      await flushPromises()
      expect(wrapper.find('[data-testid="start-replace-btn"]').exists()).toBe(true)
    })

    it('clicking 选择替换 shows select dropdowns', async () => {
      mockLineups.value = [buildLineupWithPairs('lineup-1')]
      const wrapper = mountView()
      await flushPromises()
      await wrapper.find('[data-testid="edit-btn"]').trigger('click')
      await flushPromises()
      await wrapper.find('[data-testid="start-replace-btn"]').trigger('click')
      await flushPromises()
      expect(wrapper.findAll('select').length).toBeGreaterThan(0)
    })

    it('save is blocked when violations exist', async () => {
      mockTeams.value = [{
        id: 'team-1', name: '浙江队',
        players: [
          { id: 'p1', name: 'Alice', utr: 12.0, gender: 'female', verified: true },
          { id: 'p2', name: 'Bob', utr: 11.0, gender: 'male', verified: true },
          { id: 'p3', name: 'Carol', utr: 10.0, gender: 'female', verified: true },
          { id: 'p4', name: 'Dave', utr: 9.0, gender: 'male', verified: true },
        ],
      }]
      mockLineups.value = [buildLineup('lineup-1', {
        pairs: [
          { position: 'D1', player1Id: 'p1', player2Id: 'p2', combinedUtr: 23.0 },
          { position: 'D2', player1Id: 'p3', player2Id: 'p4', combinedUtr: 19.0 },
        ],
        totalUtr: 42.0,
      })]
      const wrapper = mountView()
      await flushPromises()
      await wrapper.find('[data-testid="edit-btn"]').trigger('click')
      await flushPromises()
      await wrapper.find('[data-testid="start-replace-btn"]').trigger('click')
      await flushPromises()
      expect(wrapper.find('[data-testid="replace-violation"]').exists()).toBe(true)
      await wrapper.find('[data-testid="save-replace-btn"]').trigger('click')
      await flushPromises()
      expect(mockUpdateLineup).not.toHaveBeenCalled()
    })

    it('clicking 取消 hides replace UI', async () => {
      mockLineups.value = [buildLineupWithPairs('lineup-1')]
      const wrapper = mountView()
      await flushPromises()
      await wrapper.find('[data-testid="edit-btn"]').trigger('click')
      await flushPromises()
      await wrapper.find('[data-testid="start-replace-btn"]').trigger('click')
      await flushPromises()
      expect(wrapper.find('[data-testid="save-replace-btn"]').exists()).toBe(true)
      await wrapper.find('[data-testid="cancel-replace-btn"]').trigger('click')
      await flushPromises()
      expect(wrapper.find('[data-testid="save-replace-btn"]').exists()).toBe(false)
    })

    it('saving replace calls updateLineup then fetchLineups', async () => {
      mockLineups.value = [buildLineupWithPairs('lineup-1')]
      const wrapper = mountView()
      await flushPromises()
      await wrapper.find('[data-testid="edit-btn"]').trigger('click')
      await flushPromises()
      await wrapper.find('[data-testid="start-replace-btn"]').trigger('click')
      await flushPromises()
      await wrapper.find('[data-testid="save-replace-btn"]').trigger('click')
      await flushPromises()
      expect(mockUpdateLineup).toHaveBeenCalledWith('team-1', 'lineup-1', expect.objectContaining({ pairs: expect.any(Array) }))
      expect(mockFetchLineups).toHaveBeenCalled()
    })
  })

  // ── Reorder ────────────────────────────────────────────────────────────────
  describe('reorder up/down', () => {
    it('renders ↑ and ↓ buttons per lineup', async () => {
      mockLineups.value = [buildLineup('lineup-1'), buildLineup('lineup-2')]
      const wrapper = mountView()
      await flushPromises()
      expect(wrapper.findAll('[data-testid="move-up-btn"]')).toHaveLength(2)
      expect(wrapper.findAll('[data-testid="move-down-btn"]')).toHaveLength(2)
    })

    it('↑ is disabled for the first lineup', async () => {
      mockLineups.value = [buildLineup('lineup-1', { sortOrder: 0 }), buildLineup('lineup-2', { sortOrder: 1 })]
      const wrapper = mountView()
      await flushPromises()
      const upBtns = wrapper.findAll('[data-testid="move-up-btn"]')
      expect(upBtns[0].element.disabled).toBe(true)
      expect(upBtns[1].element.disabled).toBe(false)
    })

    it('↓ is disabled for the last lineup', async () => {
      mockLineups.value = [buildLineup('lineup-1', { sortOrder: 0 }), buildLineup('lineup-2', { sortOrder: 1 })]
      const wrapper = mountView()
      await flushPromises()
      const downBtns = wrapper.findAll('[data-testid="move-down-btn"]')
      expect(downBtns[0].element.disabled).toBe(false)
      expect(downBtns[1].element.disabled).toBe(true)
    })

    it('↑ swaps sortOrder of adjacent lineups and fetches', async () => {
      mockLineups.value = [
        buildLineup('lineup-1', { sortOrder: 0 }),
        buildLineup('lineup-2', { sortOrder: 1 }),
      ]
      const wrapper = mountView()
      await flushPromises()
      await wrapper.findAll('[data-testid="move-up-btn"]')[1].trigger('click')
      await flushPromises()
      expect(mockUpdateLineup).toHaveBeenCalledTimes(2)
      expect(mockFetchLineups).toHaveBeenCalled()
    })

    it('shows updateError when reorder fails and still refreshes', async () => {
      mockUpdateLineup.mockRejectedValue(new Error('网络错误'))
      mockLineups.value = [buildLineup('lineup-1', { sortOrder: 0 }), buildLineup('lineup-2', { sortOrder: 1 })]
      const wrapper = mountView()
      await flushPromises()
      mockFetchLineups.mockReset().mockResolvedValue()
      await wrapper.findAll('[data-testid="move-up-btn"]')[1].trigger('click')
      await flushPromises()
      expect(wrapper.find('[data-testid="update-error"]').exists()).toBe(true)
      expect(mockFetchLineups).toHaveBeenCalled()
    })
  })
})
