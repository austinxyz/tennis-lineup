import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { ref } from 'vue'
import TeamDetail from '../TeamDetail.vue'

// ── Shared mock state ──────────────────────────────────────────────────────────
const mockTeam = ref(null)
const mockTeamsLoading = ref(false)
const mockFetchTeamById = vi.fn()

const mockPlayers = ref([])
const mockPlayersLoading = ref(false)
const mockFetchPlayers = vi.fn()
const mockAddPlayer = vi.fn()
const mockUpdatePlayer = vi.fn()
const mockDeletePlayer = vi.fn()
const mockBulkUpdateUtrs = vi.fn()

// ── Module mocks ───────────────────────────────────────────────────────────────
vi.mock('vue-router', () => ({
  useRoute: vi.fn(() => ({
    params: { id: 'team1' },
    path: '/teams/team1',
  })),
  useRouter: vi.fn(() => ({ push: vi.fn() })),
  onBeforeRouteLeave: vi.fn(),
}))

vi.mock('../../composables/useTeams', () => ({
  useTeams: vi.fn(() => ({
    team: mockTeam,
    loading: mockTeamsLoading,
    fetchTeamById: mockFetchTeamById,
  })),
}))

vi.mock('../../composables/usePlayers', () => ({
  usePlayers: vi.fn(() => ({
    players: mockPlayers,
    loading: mockPlayersLoading,
    fetchPlayers: mockFetchPlayers,
    addPlayer: mockAddPlayer,
    updatePlayer: mockUpdatePlayer,
    deletePlayer: mockDeletePlayer,
    bulkUpdateUtrs: mockBulkUpdateUtrs,
  })),
}))

// ── Helpers ────────────────────────────────────────────────────────────────────
function mountDetail() {
  return mount(TeamDetail, {
    global: {
      stubs: { RouterLink: true },
    },
  })
}

const sampleTeam = {
  id: 'team1',
  name: 'Ace Squad',
  createdAt: '2024-01-15T00:00:00Z',
}

const samplePlayers = [
  { id: 'p1', name: 'Alice', gender: 'female', utr: 8.5, verified: true, profileUrl: 'https://app.utrsports.net/profiles/111' },
  { id: 'p2', name: 'Bob', gender: 'male', utr: 7.0, verified: false, profileUrl: null },
]

// ── Tests ──────────────────────────────────────────────────────────────────────
describe('TeamDetail', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockTeam.value = null
    mockTeamsLoading.value = false
    mockPlayers.value = []
    mockPlayersLoading.value = false
    mockFetchTeamById.mockResolvedValue(undefined)
    mockFetchPlayers.mockResolvedValue(undefined)
    mockAddPlayer.mockResolvedValue({ id: 'p3', name: 'New', gender: 'male', utr: 5.0, verified: false })
    mockUpdatePlayer.mockResolvedValue({ id: 'p1', name: 'Alice Updated', gender: 'female', utr: 9.0, verified: true })
    mockDeletePlayer.mockResolvedValue(undefined)
    mockBulkUpdateUtrs.mockResolvedValue({ succeeded: [], failed: [] })
  })

  it('does not render content when team is null', () => {
    mockTeam.value = null
    const wrapper = mountDetail()
    expect(wrapper.find('h2').exists()).toBe(false)
  })

  it('renders team name when team is loaded', async () => {
    mockTeam.value = sampleTeam
    mockPlayers.value = []
    const wrapper = mountDetail()
    expect(wrapper.find('h2').text()).toBe('Ace Squad')
  })

  it('renders player table with columns', async () => {
    mockTeam.value = sampleTeam
    mockPlayers.value = samplePlayers
    const wrapper = mountDetail()
    expect(wrapper.text()).toContain('姓名')
    expect(wrapper.text()).toContain('性别')
    expect(wrapper.text()).toContain('UTR')
    expect(wrapper.text()).toContain('已验证')
    expect(wrapper.text()).toContain('操作')
  })

  it('renders players in table', async () => {
    mockTeam.value = sampleTeam
    mockPlayers.value = samplePlayers
    const wrapper = mountDetail()
    expect(wrapper.text()).toContain('Alice')
    expect(wrapper.text()).toContain('Bob')
    expect(wrapper.text()).toContain('8.50')
    expect(wrapper.text()).toContain('7.00')
  })

  it('shows loading spinner when playersLoading is true', () => {
    mockTeam.value = sampleTeam
    mockPlayersLoading.value = true
    const wrapper = mountDetail()
    expect(wrapper.find('.animate-spin').exists()).toBe(true)
  })

  it('hides spinner and shows player section when not loading', () => {
    mockTeam.value = sampleTeam
    mockPlayersLoading.value = false
    mockPlayers.value = []
    const wrapper = mountDetail()
    expect(wrapper.find('.animate-spin').exists()).toBe(false)
    expect(wrapper.text()).toContain('球员列表')
  })

  it('shows empty row message when no players', () => {
    mockTeam.value = sampleTeam
    mockPlayers.value = []
    const wrapper = mountDetail()
    expect(wrapper.text()).toContain('暂无球员')
  })

  it('opens add player modal when 添加球员 is clicked', async () => {
    mockTeam.value = sampleTeam
    mockPlayers.value = []
    const wrapper = mountDetail()
    // Modal form should not be visible initially
    expect(wrapper.find('#playerName').exists()).toBe(false)
    const addBtn = wrapper.findAll('button').find(b => b.text().includes('添加球员'))
    await addBtn.trigger('click')
    // After clicking, the modal form appears
    expect(wrapper.find('#playerName').exists()).toBe(true)
  })

  it('submits player form with correct data for new player', async () => {
    mockTeam.value = sampleTeam
    mockPlayers.value = []
    const wrapper = mountDetail()

    // Open modal
    const addBtn = wrapper.findAll('button').find(b => b.text().includes('添加球员'))
    await addBtn.trigger('click')

    // Fill form fields
    await wrapper.find('#playerName').setValue('Carol')
    await wrapper.find('#playerGender').setValue('female')
    await wrapper.find('#playerUtr').setValue(9.5)

    // Submit
    await wrapper.find('form').trigger('submit')

    expect(mockAddPlayer).toHaveBeenCalledWith(
      expect.objectContaining({ name: 'Carol', gender: 'female', utr: 9.5 })
    )
  })

  it('edit player populates the form with player data', async () => {
    mockTeam.value = sampleTeam
    mockPlayers.value = samplePlayers
    const wrapper = mountDetail()

    // Click edit button for first player
    const editBtns = wrapper.findAll('button').filter(b => b.text() === '编辑')
    await editBtns[0].trigger('click')

    // Modal should show with 编辑球员 title
    expect(wrapper.findAll('h3').some(h => h.text().includes('编辑球员'))).toBe(true)

    // Form should be pre-populated
    const nameInput = wrapper.find('#playerName')
    expect(nameInput.element.value).toBe('Alice')
  })

  it('submits update when editing existing player', async () => {
    mockTeam.value = sampleTeam
    mockPlayers.value = samplePlayers
    const wrapper = mountDetail()

    // Click edit for Alice
    const editBtns = wrapper.findAll('button').filter(b => b.text() === '编辑')
    await editBtns[0].trigger('click')

    // Change name
    await wrapper.find('#playerName').setValue('Alice Renamed')

    // Submit
    await wrapper.find('form').trigger('submit')

    expect(mockUpdatePlayer).toHaveBeenCalledWith(
      'p1',
      expect.objectContaining({ name: 'Alice Renamed' })
    )
  })

  it('calls confirmDeletePlayer when 删除 is clicked', async () => {
    mockTeam.value = sampleTeam
    mockPlayers.value = samplePlayers
    vi.spyOn(window, 'confirm').mockReturnValue(false)

    const wrapper = mountDetail()
    const deleteBtns = wrapper.findAll('button').filter(b => b.text().includes('删除'))
    await deleteBtns[0].trigger('click')

    expect(window.confirm).toHaveBeenCalled()
  })

  it('calls deletePlayer when deletion is confirmed', async () => {
    mockTeam.value = sampleTeam
    mockPlayers.value = samplePlayers
    vi.spyOn(window, 'confirm').mockReturnValue(true)

    const wrapper = mountDetail()
    const deleteBtns = wrapper.findAll('button').filter(b => b.text().includes('删除'))
    await deleteBtns[0].trigger('click')

    expect(mockDeletePlayer).toHaveBeenCalledWith('p1')
  })

  it('does not call deletePlayer when deletion is cancelled', async () => {
    mockTeam.value = sampleTeam
    mockPlayers.value = samplePlayers
    vi.spyOn(window, 'confirm').mockReturnValue(false)

    const wrapper = mountDetail()
    const deleteBtns = wrapper.findAll('button').filter(b => b.text().includes('删除'))
    await deleteBtns[0].trigger('click')

    expect(mockDeletePlayer).not.toHaveBeenCalled()
  })

  it('shows player count in heading', () => {
    mockTeam.value = sampleTeam
    mockPlayers.value = samplePlayers
    const wrapper = mountDetail()
    expect(wrapper.text()).toContain('球员列表 (2)')
  })

  it('cancels edit and resets form', async () => {
    mockTeam.value = sampleTeam
    mockPlayers.value = samplePlayers
    const wrapper = mountDetail()

    // Open edit
    const editBtns = wrapper.findAll('button').filter(b => b.text() === '编辑')
    await editBtns[0].trigger('click')

    // Click cancel
    const cancelBtn = wrapper.findAll('button').find(b => b.text().includes('取消'))
    await cancelBtn.trigger('click')

    // Modal should be gone
    expect(wrapper.findAll('h3').some(h => h.text().includes('编辑球员'))).toBe(false)
  })

  it('displays UTR with two decimal places', () => {
    mockTeam.value = sampleTeam
    mockPlayers.value = [{ id: 'p1', name: 'Alice', gender: 'female', utr: 8.5, verified: true, profileUrl: null }]
    const wrapper = mountDetail()
    expect(wrapper.text()).toContain('8.50')
  })

  it('shows UTR profile link when profileUrl is set', () => {
    mockTeam.value = sampleTeam
    mockPlayers.value = samplePlayers
    const wrapper = mountDetail()
    const links = wrapper.findAll('a')
    const utrLink = links.find(a => a.text().includes('UTR主页'))
    expect(utrLink).toBeTruthy()
    expect(utrLink.attributes('href')).toBe('https://app.utrsports.net/profiles/111')
    expect(utrLink.attributes('target')).toBe('_blank')
  })

  it('does not show UTR profile link when profileUrl is null', () => {
    mockTeam.value = sampleTeam
    mockPlayers.value = [{ id: 'p2', name: 'Bob', gender: 'male', utr: 7.0, verified: false, profileUrl: null }]
    const wrapper = mountDetail()
    const links = wrapper.findAll('a')
    const utrLink = links.find(a => a.text().includes('UTR主页'))
    expect(utrLink).toBeFalsy()
  })

  it('calls fetchTeamById and fetchPlayers on mount', async () => {
    mockTeam.value = sampleTeam
    const wrapper = mountDetail()
    await vi.waitFor(() => {
      expect(mockFetchTeamById).toHaveBeenCalledWith('team1')
      expect(mockFetchPlayers).toHaveBeenCalled()
    })
  })

  // ── Bulk edit UTR ──────────────────────────────────────────────────────────────

  it('shows 批量编辑 UTR button in normal mode', () => {
    mockTeam.value = sampleTeam
    mockPlayers.value = samplePlayers
    const wrapper = mountDetail()
    const btn = wrapper.findAll('button').find(b => b.text().includes('批量编辑 UTR'))
    expect(btn).toBeTruthy()
  })

  it('enters bulk edit mode showing 保存 and 取消 buttons', async () => {
    mockTeam.value = sampleTeam
    mockPlayers.value = samplePlayers
    const wrapper = mountDetail()
    const bulkBtn = wrapper.findAll('button').find(b => b.text().includes('批量编辑 UTR'))
    await bulkBtn.trigger('click')
    expect(wrapper.findAll('button').some(b => b.text() === '保存')).toBe(true)
    expect(wrapper.findAll('button').some(b => b.text() === '取消')).toBe(true)
    expect(wrapper.findAll('button').some(b => b.text().includes('批量编辑 UTR'))).toBe(false)
  })

  it('shows number inputs for UTR in bulk edit mode', async () => {
    mockTeam.value = sampleTeam
    mockPlayers.value = samplePlayers
    const wrapper = mountDetail()
    const bulkBtn = wrapper.findAll('button').find(b => b.text().includes('批量编辑 UTR'))
    await bulkBtn.trigger('click')
    const utrInputs = wrapper.findAll('input[type="number"]').filter(i => i.attributes('step') === '0.01')
    expect(utrInputs.length).toBe(samplePlayers.length)
  })

  it('cancels bulk edit and reverts to normal view', async () => {
    mockTeam.value = sampleTeam
    mockPlayers.value = samplePlayers
    const wrapper = mountDetail()
    const bulkBtn = wrapper.findAll('button').find(b => b.text().includes('批量编辑 UTR'))
    await bulkBtn.trigger('click')
    const cancelBtn = wrapper.findAll('button').find(b => b.text() === '取消')
    await cancelBtn.trigger('click')
    expect(wrapper.findAll('button').some(b => b.text().includes('批量编辑 UTR'))).toBe(true)
    expect(wrapper.findAll('button').some(b => b.text() === '保存')).toBe(false)
  })

  it('calls bulkUpdateUtrs with changed players on save', async () => {
    mockTeam.value = sampleTeam
    mockPlayers.value = samplePlayers
    mockBulkUpdateUtrs.mockResolvedValue({ succeeded: ['p1'], failed: [] })
    const wrapper = mountDetail()

    const bulkBtn = wrapper.findAll('button').find(b => b.text().includes('批量编辑 UTR'))
    await bulkBtn.trigger('click')

    // Change Alice's UTR
    const utrInputs = wrapper.findAll('input[type="number"]').filter(i => i.attributes('step') === '0.01')
    await utrInputs[0].setValue(9.0)

    const saveBtn = wrapper.findAll('button').find(b => b.text() === '保存')
    await saveBtn.trigger('click')

    expect(mockBulkUpdateUtrs).toHaveBeenCalledWith(
      expect.arrayContaining([expect.objectContaining({ playerId: 'p1', utr: 9 })])
    )
  })

  it('does not call bulkUpdateUtrs when no UTR values changed', async () => {
    mockTeam.value = sampleTeam
    mockPlayers.value = samplePlayers
    const wrapper = mountDetail()

    const bulkBtn = wrapper.findAll('button').find(b => b.text().includes('批量编辑 UTR'))
    await bulkBtn.trigger('click')

    const saveBtn = wrapper.findAll('button').find(b => b.text() === '保存')
    await saveBtn.trigger('click')

    expect(mockBulkUpdateUtrs).not.toHaveBeenCalled()
    expect(wrapper.findAll('button').some(b => b.text().includes('批量编辑 UTR'))).toBe(true)
  })

  it('shows error list when some bulk updates fail', async () => {
    mockTeam.value = sampleTeam
    mockPlayers.value = samplePlayers
    mockBulkUpdateUtrs.mockResolvedValue({
      succeeded: [],
      failed: [{ playerId: 'p1', message: '服务器错误' }]
    })
    const wrapper = mountDetail()

    const bulkBtn = wrapper.findAll('button').find(b => b.text().includes('批量编辑 UTR'))
    await bulkBtn.trigger('click')

    const utrInputs = wrapper.findAll('input[type="number"]').filter(i => i.attributes('step') === '0.01')
    await utrInputs[0].setValue(9.0)

    const saveBtn = wrapper.findAll('button').find(b => b.text() === '保存')
    await saveBtn.trigger('click')

    await wrapper.vm.$nextTick()
    expect(wrapper.text()).toContain('以下球员更新失败')
    expect(wrapper.text()).toContain('服务器错误')
  })
})
