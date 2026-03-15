import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { ref } from 'vue'
import TeamListPanel from '../TeamListPanel.vue'

// ── Shared mock state ──────────────────────────────────────────────────────────
const mockTeams = ref([])
const mockLoading = ref(false)
const mockFetchTeams = vi.fn()
const mockCreateTeam = vi.fn()
const mockDeleteTeam = vi.fn()

const mockImportLoading = ref(false)
const mockImportError = ref(null)
const mockImportResult = ref(null)
const mockImportFromCSV = vi.fn()
const mockImportFromJSON = vi.fn()

const mockRouteParams = ref({})
const mockRouterPush = vi.fn()

// ── Module mocks ───────────────────────────────────────────────────────────────
vi.mock('vue-router', () => ({
  useRoute: vi.fn(() => ({
    params: mockRouteParams.value,
    path: '/',
  })),
  useRouter: vi.fn(() => ({
    push: mockRouterPush,
  })),
  RouterLink: {
    name: 'RouterLink',
    template: '<a :href="to" @click="$emit(\'click\')"><slot /></a>',
    props: ['to'],
    emits: ['click'],
  },
}))

vi.mock('../../composables/useTeams', () => ({
  useTeams: vi.fn(() => ({
    teams: mockTeams,
    loading: mockLoading,
    fetchTeams: mockFetchTeams,
    createTeam: mockCreateTeam,
    deleteTeam: mockDeleteTeam,
  })),
}))

vi.mock('../../composables/useBatchImport', () => ({
  useBatchImport: vi.fn(() => ({
    loading: mockImportLoading,
    error: mockImportError,
    importResult: mockImportResult,
    importFromCSV: mockImportFromCSV,
    importFromJSON: mockImportFromJSON,
  })),
}))

import { useRoute } from 'vue-router'

// ── Helpers ────────────────────────────────────────────────────────────────────
const RouterLinkStub = {
  name: 'RouterLink',
  template: '<a :href="to" @click="$emit(\'click\')"><slot /></a>',
  props: ['to'],
  emits: ['click'],
}

function mountPanel() {
  return mount(TeamListPanel, {
    global: {
      stubs: { RouterLink: RouterLinkStub },
    },
  })
}

// ── Tests ──────────────────────────────────────────────────────────────────────
describe('TeamListPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockTeams.value = []
    mockLoading.value = false
    mockImportLoading.value = false
    mockImportError.value = null
    mockImportResult.value = null
    mockRouteParams.value = {}
    useRoute.mockReturnValue({ params: mockRouteParams.value, path: '/' })
    mockFetchTeams.mockResolvedValue(undefined)
    mockCreateTeam.mockResolvedValue({ id: 'new1', name: 'New Team' })
    mockDeleteTeam.mockResolvedValue(undefined)
  })

  it('renders team list when teams are loaded', async () => {
    mockTeams.value = [
      { id: '1', name: 'Alpha', players: [] },
      { id: '2', name: 'Beta', players: [{ id: 'p1' }] },
    ]
    const wrapper = mountPanel()
    expect(wrapper.text()).toContain('Alpha')
    expect(wrapper.text()).toContain('Beta')
  })

  it('shows loading spinner when loading', () => {
    mockLoading.value = true
    const wrapper = mountPanel()
    // The spinner is an animate-spin div
    expect(wrapper.find('.animate-spin').exists()).toBe(true)
  })

  it('does not show spinner when not loading', () => {
    mockLoading.value = false
    const wrapper = mountPanel()
    expect(wrapper.find('.animate-spin').exists()).toBe(false)
  })

  it('shows empty state when no teams', () => {
    mockTeams.value = []
    const wrapper = mountPanel()
    expect(wrapper.text()).toContain('暂无队伍')
  })

  it('opens create modal when 创建队伍 button is clicked', async () => {
    const wrapper = mountPanel()
    expect(wrapper.text()).not.toContain('创建新队伍')
    await wrapper.find('button').trigger('click') // first button is 创建队伍
    expect(wrapper.text()).toContain('创建新队伍')
  })

  it('opens import modal when 导入 button is clicked', async () => {
    const wrapper = mountPanel()
    const buttons = wrapper.findAll('button')
    // Second button is 导入
    const importBtn = buttons.find(b => b.text().includes('导入'))
    await importBtn.trigger('click')
    expect(wrapper.text()).toContain('批量导入')
  })

  it('submits create form and navigates to new team', async () => {
    const wrapper = mountPanel()
    // Open modal
    await wrapper.find('button').trigger('click')
    // Fill in team name
    const input = wrapper.find('input[placeholder="请输入队名"]')
    await input.setValue('Test Team')
    // Submit form
    await wrapper.find('form').trigger('submit')
    expect(mockCreateTeam).toHaveBeenCalledWith('Test Team')
    // Wait for async
    await vi.waitFor(() => {
      expect(mockRouterPush).toHaveBeenCalledWith('/teams/new1')
    })
  })

  it('highlights active team based on route param', () => {
    mockTeams.value = [
      { id: 'team1', name: 'Active Team', players: [] },
      { id: 'team2', name: 'Other Team', players: [] },
    ]
    useRoute.mockReturnValue({ params: { id: 'team1' }, path: '/teams/team1' })
    const wrapper = mountPanel()
    const links = wrapper.findAll('a[href="/teams/team1"]')
    const activeLink = links[0]
    expect(activeLink.classes()).toContain('bg-green-50')
    expect(activeLink.classes()).toContain('text-green-700')
  })

  it('calls confirmDelete when delete button is clicked', async () => {
    mockTeams.value = [{ id: '1', name: 'Team One', players: [] }]
    vi.spyOn(window, 'confirm').mockReturnValue(false)
    const wrapper = mountPanel()
    // Delete button is inside the router-link item; trigger click.prevent
    const deleteBtn = wrapper.find('button[class*="opacity-0"]')
    await deleteBtn.trigger('click')
    expect(window.confirm).toHaveBeenCalled()
  })

  it('deletes team and redirects when confirmed and team is active', async () => {
    const team = { id: 'active1', name: 'Active Team', players: [] }
    mockTeams.value = [team]
    useRoute.mockReturnValue({ params: { id: 'active1' }, path: '/teams/active1' })
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    const wrapper = mountPanel()
    const deleteBtn = wrapper.find('button[class*="opacity-0"]')
    await deleteBtn.trigger('click')
    await vi.waitFor(() => {
      expect(mockDeleteTeam).toHaveBeenCalledWith('active1')
    })
  })

  it('calls fetchTeams on mount', () => {
    mountPanel()
    expect(mockFetchTeams).toHaveBeenCalled()
  })
})
