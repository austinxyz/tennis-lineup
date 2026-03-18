import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { ref } from 'vue'
import LineupHistoryView from '../LineupHistoryView.vue'

// ── Mock state ─────────────────────────────────────────────────────────────────
const mockLineups = ref([])
const mockLoading = ref(false)
const mockFetchLineups = vi.fn()
const mockDeleteLineup = vi.fn()

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
function buildLineup(id) {
  return {
    id,
    strategy: 'balanced',
    totalUtr: 38.0,
    createdAt: '2026-01-01T10:00:00Z',
    pairs: [],
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

    const deleteButtons = wrapper.findAll('button')
    await deleteButtons[0].trigger('click')
    await flushPromises()

    expect(mockDeleteLineup).toHaveBeenCalledWith('lineup-1')
  })

  it('shows error message when delete fails', async () => {
    mockLineups.value = [buildLineup('lineup-1')]
    mockDeleteLineup.mockRejectedValue(new Error('排阵不存在'))
    const wrapper = mountView()
    await flushPromises()

    const deleteBtn = wrapper.find('button')
    await deleteBtn.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('排阵不存在')
  })

  it('does not call deleteLineup if confirm is cancelled', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false)
    mockLineups.value = [buildLineup('lineup-1')]
    const wrapper = mountView()
    await flushPromises()

    const deleteBtn = wrapper.find('button')
    await deleteBtn.trigger('click')
    await flushPromises()

    expect(mockDeleteLineup).not.toHaveBeenCalled()
  })
})
