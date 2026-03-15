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

// ── Module mocks ───────────────────────────────────────────────────────────────
vi.mock('vue-router', () => ({
  useRoute: vi.fn(() => ({
    params: { id: 'team1' },
    path: '/teams/team1',
  })),
  useRouter: vi.fn(() => ({ push: vi.fn() })),
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
  { id: 'p1', name: 'Alice', gender: 'female', utr: 8.5, verified: true },
  { id: 'p2', name: 'Bob', gender: 'male', utr: 7.0, verified: false },
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
    expect(wrapper.text()).toContain('8.5')
    expect(wrapper.text()).toContain('7')
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
    const editBtns = wrapper.findAll('button').filter(b => b.text().includes('编辑'))
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
    const editBtns = wrapper.findAll('button').filter(b => b.text().includes('编辑'))
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
    const editBtns = wrapper.findAll('button').filter(b => b.text().includes('编辑'))
    await editBtns[0].trigger('click')

    // Click cancel
    const cancelBtn = wrapper.findAll('button').find(b => b.text().includes('取消'))
    await cancelBtn.trigger('click')

    // Modal should be gone
    expect(wrapper.findAll('h3').some(h => h.text().includes('编辑球员'))).toBe(false)
  })

  it('calls fetchTeamById and fetchPlayers on mount', async () => {
    mockTeam.value = sampleTeam
    const wrapper = mountDetail()
    await vi.waitFor(() => {
      expect(mockFetchTeamById).toHaveBeenCalledWith('team1')
      expect(mockFetchPlayers).toHaveBeenCalled()
    })
  })
})
