import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { ref } from 'vue'
import OpponentAnalysis from '../OpponentAnalysis.vue'

// --- Mocks ---

vi.mock('../../components/AppHeader.vue', () => ({
  default: {
    name: 'AppHeader',
    props: ['title', 'backTo', 'backLabel'],
    template: '<header data-testid="app-header" :data-title="title"><slot name="actions"/></header>',
  },
}))

const mockTeams = ref([
  { id: 'team-1', name: '浙江队' },
  { id: 'team-2', name: '上海队' },
])
const mockFetchTeams = vi.fn().mockResolvedValue(undefined)

vi.mock('../../composables/useTeams', () => ({
  useTeams: () => ({
    teams: mockTeams,
    fetchTeams: mockFetchTeams,
  }),
}))

const mockGet = vi.fn()
const mockPost = vi.fn()

vi.mock('../../composables/useApi', () => ({
  useApi: () => ({
    loading: ref(false),
    error: ref(null),
    get: mockGet,
    post: mockPost,
    put: vi.fn(),
    del: vi.fn(),
    patch: vi.fn(),
  }),
}))

// --- Fixture data ---

const samplePairs = [
  { position: 'D1', player1Name: 'A', player1Gender: 'female', player2Name: 'B', player2Gender: 'male', player1Utr: 6.0, player2Utr: 5.5 },
  { position: 'D2', player1Name: 'C', player1Gender: 'male', player2Name: 'D', player2Gender: 'female', player1Utr: 5.0, player2Utr: 5.0 },
]

const myLineup = { id: 'ml1', label: '我方排阵', strategy: 'balanced', pairs: samplePairs, totalUtr: 22.0 }
const oppLineup = { id: 'ol1', label: '对手排阵', strategy: 'aggressive', pairs: samplePairs, totalUtr: 20.0 }

const mockLineAnalysis = [
  { position: 'D1', ownCombinedUtr: 11.5, opponentCombinedUtr: 10.5, ownCombinedRegularUtr: 11.5, opponentCombinedActualUtr: 10.5, delta: 1.0, winProbability: 0.8, label: '80% 赢' },
  { position: 'D2', ownCombinedUtr: 10.0, opponentCombinedUtr: 10.0, ownCombinedRegularUtr: 10.0, opponentCombinedActualUtr: 10.0, delta: 0.0, winProbability: 0.5, label: '对等' },
]

const mockMatchupResult = {
  results: [
    {
      lineup: { id: 'ml1', pairs: samplePairs },
      lineAnalysis: mockLineAnalysis,
      expectedScore: 7.2,
      opponentExpectedScore: 2.8,
      verdict: '能赢',
    },
  ],
  aiRecommendation: {
    aiUsed: true,
    lineup: { id: 'ml1', pairs: samplePairs },
    opponentLineup: { id: 'ol1', pairs: samplePairs },
    lineAnalysis: mockLineAnalysis,
    explanation: 'D1组合UTR优势明显',
    expectedScore: 7.0,
  },
}

// --- Setup helpers ---

beforeEach(() => {
  mockGet.mockReset()
  mockPost.mockReset()
  mockFetchTeams.mockResolvedValue(undefined)
  vi.spyOn(console, 'error').mockImplementation(() => {})
})

afterEach(() => {
  vi.restoreAllMocks()
})

function mountComponent() {
  return mount(OpponentAnalysis, {
    global: {
      stubs: {
        RouterLink: true,
        PartnerNotesEditor: { template: '<div data-testid="partner-notes-editor"/>' },
      },
    },
  })
}

// Helper: select my team and load my lineups
async function selectMyTeam(wrapper, lineups = [myLineup]) {
  mockGet.mockResolvedValueOnce(lineups)
  await wrapper.find('[data-testid="select-my-team"]').setValue('team-1')
  await flushPromises()
}

// Helper: select opp team and load opp lineups
async function selectOppTeam(wrapper, lineups = [oppLineup]) {
  mockGet.mockResolvedValueOnce(lineups)
  await wrapper.find('[data-testid="select-opp-team"]').setValue('team-2')
  await flushPromises()
}

// Helper: fully select all 4 dropdowns
async function selectAll(wrapper) {
  await selectMyTeam(wrapper)
  await wrapper.find('[data-testid="select-my-lineup"]').setValue('ml1')
  await flushPromises()
  await selectOppTeam(wrapper)
  await wrapper.find('[data-testid="select-opp-lineup"]').setValue('ol1')
  await flushPromises()
}

// ============================================================
// TESTS
// ============================================================

describe('OpponentAnalysis dropdown flow', () => {
  // --- Structure ---

  it('renders AppHeader with title 对手分析', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    const header = wrapper.find('[data-testid="app-header"]')
    expect(header.exists()).toBe(true)
    expect(header.attributes('data-title')).toBe('对手分析')
  })

  it('renders 4 select dropdowns', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    expect(wrapper.find('[data-testid="select-my-team"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="select-my-lineup"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="select-opp-team"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="select-opp-lineup"]').exists()).toBe(true)
  })

  it('calls fetchTeams on mount', async () => {
    mountComponent()
    await flushPromises()
    expect(mockFetchTeams).toHaveBeenCalled()
  })

  it('populates my-team dropdown with teams from useTeams', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    const options = wrapper.find('[data-testid="select-my-team"]').findAll('option')
    expect(options.some(o => o.text().includes('浙江队'))).toBe(true)
    expect(options.some(o => o.text().includes('上海队'))).toBe(true)
  })

  it('populates opp-team dropdown with teams from useTeams', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    const options = wrapper.find('[data-testid="select-opp-team"]').findAll('option')
    expect(options.some(o => o.text().includes('浙江队'))).toBe(true)
    expect(options.some(o => o.text().includes('上海队'))).toBe(true)
  })

  // --- Analyze button state ---

  it('analyze button is disabled when no team is selected', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    expect(wrapper.find('[data-testid="analyze-btn"]').attributes('disabled')).toBeDefined()
  })

  it('analyze button is disabled when only my team is selected', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    await selectMyTeam(wrapper)
    expect(wrapper.find('[data-testid="analyze-btn"]').attributes('disabled')).toBeDefined()
  })

  it('analyze button is disabled when only 3 of 4 dropdowns are selected', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    await selectMyTeam(wrapper)
    await wrapper.find('[data-testid="select-my-lineup"]').setValue('ml1')
    await flushPromises()
    await selectOppTeam(wrapper)
    // oppLineupId still empty
    expect(wrapper.find('[data-testid="analyze-btn"]').attributes('disabled')).toBeDefined()
  })

  it('analyze button is enabled when all 4 selections are made', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    await selectAll(wrapper)
    expect(wrapper.find('[data-testid="analyze-btn"]').attributes('disabled')).toBeUndefined()
  })

  // --- Team change triggers lineup load ---

  it('selecting my team triggers GET /api/teams/{id}/lineups', async () => {
    mockGet.mockResolvedValueOnce([])
    const wrapper = mountComponent()
    await flushPromises()
    await wrapper.find('[data-testid="select-my-team"]').setValue('team-1')
    await flushPromises()
    expect(mockGet).toHaveBeenCalledWith('/api/teams/team-1/lineups')
  })

  it('selecting opp team triggers GET /api/teams/{id}/lineups', async () => {
    mockGet.mockResolvedValueOnce([])
    const wrapper = mountComponent()
    await flushPromises()
    await wrapper.find('[data-testid="select-opp-team"]').setValue('team-2')
    await flushPromises()
    expect(mockGet).toHaveBeenCalledWith('/api/teams/team-2/lineups')
  })

  it('changing my team resets my lineup selection', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    await selectMyTeam(wrapper)
    await wrapper.find('[data-testid="select-my-lineup"]').setValue('ml1')
    await flushPromises()

    // Now change team
    mockGet.mockResolvedValueOnce([])
    await wrapper.find('[data-testid="select-my-team"]').setValue('team-2')
    await flushPromises()

    const myLineupSel = wrapper.find('[data-testid="select-my-lineup"]')
    expect(myLineupSel.element.value).toBe('')
  })

  it('changing opp team resets opp lineup selection', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    await selectOppTeam(wrapper)
    await wrapper.find('[data-testid="select-opp-lineup"]').setValue('ol1')
    await flushPromises()

    // Now change team
    mockGet.mockResolvedValueOnce([])
    await wrapper.find('[data-testid="select-opp-team"]').setValue('team-1')
    await flushPromises()

    const oppLineupSel = wrapper.find('[data-testid="select-opp-lineup"]')
    expect(oppLineupSel.element.value).toBe('')
  })

  // --- Lineup selects disabled/enabled state ---

  it('my-lineup select is disabled when no my team selected', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    expect(wrapper.find('[data-testid="select-my-lineup"]').attributes('disabled')).toBeDefined()
  })

  it('my-lineup select is disabled when my team has no lineups', async () => {
    mockGet.mockResolvedValueOnce([])
    const wrapper = mountComponent()
    await flushPromises()
    await wrapper.find('[data-testid="select-my-team"]').setValue('team-1')
    await flushPromises()
    expect(wrapper.find('[data-testid="select-my-lineup"]').attributes('disabled')).toBeDefined()
  })

  it('my-lineup select is enabled when my team has lineups', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    await selectMyTeam(wrapper, [myLineup])
    expect(wrapper.find('[data-testid="select-my-lineup"]').attributes('disabled')).toBeUndefined()
  })

  it('opp-lineup select is disabled when no opp team selected', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    expect(wrapper.find('[data-testid="select-opp-lineup"]').attributes('disabled')).toBeDefined()
  })

  it('opponent lineup select is disabled when opp team has no lineups', async () => {
    mockGet.mockResolvedValueOnce([])
    const wrapper = mountComponent()
    await flushPromises()
    await wrapper.find('[data-testid="select-opp-team"]').setValue('team-2')
    await flushPromises()
    const sel = wrapper.find('[data-testid="select-opp-lineup"]')
    expect(sel.attributes('disabled')).toBeDefined()
    expect(wrapper.text()).toContain('该队伍暂无排阵')
  })

  it('opp-lineup select is enabled when opp team has lineups', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    await selectOppTeam(wrapper, [oppLineup])
    expect(wrapper.find('[data-testid="select-opp-lineup"]').attributes('disabled')).toBeUndefined()
  })

  // --- 暂无排阵 placeholder text ---

  it('shows 请先选队伍 in my-lineup placeholder before team is selected', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    expect(wrapper.find('[data-testid="select-my-lineup"]').text()).toContain('请先选队伍')
  })

  it('shows 该队伍暂无排阵 in my-lineup when team has no lineups', async () => {
    mockGet.mockResolvedValueOnce([])
    const wrapper = mountComponent()
    await flushPromises()
    await wrapper.find('[data-testid="select-my-team"]').setValue('team-1')
    await flushPromises()
    expect(wrapper.find('[data-testid="select-my-lineup"]').text()).toContain('该队伍暂无排阵')
  })

  it('shows 请先选对手 in opp-lineup placeholder before opp team is selected', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    expect(wrapper.find('[data-testid="select-opp-lineup"]').text()).toContain('请先选对手')
  })

  // --- Preview cards ---

  it('does not show my-preview before lineup is selected', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    expect(wrapper.find('[data-testid="my-preview"]').exists()).toBe(false)
  })

  it('does not show opp-preview before lineup is selected', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    expect(wrapper.find('[data-testid="opp-preview"]').exists()).toBe(false)
  })

  it('shows my-preview once my lineup is selected', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    await selectMyTeam(wrapper)
    await wrapper.find('[data-testid="select-my-lineup"]').setValue('ml1')
    await flushPromises()
    expect(wrapper.find('[data-testid="my-preview"]').exists()).toBe(true)
  })

  it('shows opp-preview once opp lineup is selected', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    await selectOppTeam(wrapper)
    await wrapper.find('[data-testid="select-opp-lineup"]').setValue('ol1')
    await flushPromises()
    expect(wrapper.find('[data-testid="opp-preview"]').exists()).toBe(true)
  })

  it('shows my and opponent previews once both lineups are selected', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    await selectAll(wrapper)
    expect(wrapper.find('[data-testid="my-preview"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="opp-preview"]').exists()).toBe(true)
  })

  it('preview shows player names from lineup pairs', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    await selectMyTeam(wrapper)
    await wrapper.find('[data-testid="select-my-lineup"]').setValue('ml1')
    await flushPromises()
    const preview = wrapper.find('[data-testid="my-preview"]')
    expect(preview.text()).toContain('A')
    expect(preview.text()).toContain('B')
  })

  it('my-preview shows D1 position label', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    await selectMyTeam(wrapper)
    await wrapper.find('[data-testid="select-my-lineup"]').setValue('ml1')
    await flushPromises()
    expect(wrapper.find('[data-testid="my-preview"]').text()).toContain('D1')
  })

  it('opp-preview has red-tinted styling (bg-red-50 border-red-200)', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    await selectOppTeam(wrapper)
    await wrapper.find('[data-testid="select-opp-lineup"]').setValue('ol1')
    await flushPromises()
    const preview = wrapper.find('[data-testid="opp-preview"]')
    expect(preview.classes()).toContain('bg-red-50')
  })

  // --- Analyze button click ---

  it('analyze button click posts correct payload', async () => {
    mockPost.mockResolvedValue({ results: [], aiRecommendation: null })
    const wrapper = mountComponent()
    await flushPromises()
    await selectAll(wrapper)
    await wrapper.find('[data-testid="analyze-btn"]').trigger('click')
    await flushPromises()
    expect(mockPost).toHaveBeenCalledWith('/api/lineups/matchup', expect.objectContaining({
      teamId: 'team-1',
      ownLineupId: 'ml1',
      opponentTeamId: 'team-2',
      opponentLineupId: 'ol1',
    }))
  })

  it('analyze payload includes ownPartnerNotes and opponentPartnerNotes from pair notes', async () => {
    const lineupWithNotes = {
      id: 'ml1',
      label: '我方',
      strategy: 'balanced',
      totalUtr: 22,
      pairs: [
        { position: 'D1', player1Name: 'A', player2Name: 'B', player1Notes: '正手强', player2Notes: null },
        { position: 'D2', player1Name: 'C', player2Name: 'D', player1Notes: '底线稳', player2Notes: '发球好' },
      ],
    }
    const oppLineupWithNotes = {
      id: 'ol1',
      label: '对手',
      strategy: 'balanced',
      totalUtr: 20,
      pairs: [
        { position: 'D1', player1Name: 'X', player2Name: 'Y', player1Notes: '反手弱', player2Notes: null },
      ],
    }
    mockGet
      .mockResolvedValueOnce([lineupWithNotes])
      .mockResolvedValueOnce([oppLineupWithNotes])
    mockPost.mockResolvedValue({ results: [], aiRecommendation: null })

    const wrapper = mountComponent()
    await flushPromises()
    await wrapper.find('[data-testid="select-my-team"]').setValue('team-1')
    await flushPromises()
    await wrapper.find('[data-testid="select-my-lineup"]').setValue('ml1')
    await flushPromises()
    await wrapper.find('[data-testid="select-opp-team"]').setValue('team-2')
    await flushPromises()
    await wrapper.find('[data-testid="select-opp-lineup"]').setValue('ol1')
    await flushPromises()
    await wrapper.find('[data-testid="analyze-btn"]').trigger('click')
    await flushPromises()

    expect(mockPost).toHaveBeenCalledWith('/api/lineups/matchup', expect.objectContaining({
      ownPartnerNotes: [
        { player1Name: 'A', player2Name: 'B', note: '正手强' },
        { player1Name: 'C', player2Name: 'D', note: '底线稳 | 发球好' },
      ],
      opponentPartnerNotes: [
        { player1Name: 'X', player2Name: 'Y', note: '反手弱' },
      ],
    }))
  })

  it('analyze button includes includeAi: true in payload', async () => {
    mockPost.mockResolvedValue({ results: [], aiRecommendation: null })
    const wrapper = mountComponent()
    await flushPromises()
    await selectAll(wrapper)
    await wrapper.find('[data-testid="analyze-btn"]').trigger('click')
    await flushPromises()
    expect(mockPost).toHaveBeenCalledWith('/api/lineups/matchup', expect.objectContaining({
      includeAi: true,
    }))
  })

  it('shows 分析中... while analyzing', async () => {
    let resolvePost
    mockPost.mockReturnValue(new Promise(r => { resolvePost = r }))
    const wrapper = mountComponent()
    await flushPromises()
    await selectAll(wrapper)
    wrapper.find('[data-testid="analyze-btn"]').trigger('click')
    await flushPromises()
    expect(wrapper.find('[data-testid="analyze-btn"]').text()).toContain('分析中')
    resolvePost({ results: [], aiRecommendation: null })
    await flushPromises()
  })

  it('analyze button is disabled while analyzing', async () => {
    let resolvePost
    mockPost.mockReturnValue(new Promise(r => { resolvePost = r }))
    const wrapper = mountComponent()
    await flushPromises()
    await selectAll(wrapper)
    wrapper.find('[data-testid="analyze-btn"]').trigger('click')
    await flushPromises()
    expect(wrapper.find('[data-testid="analyze-btn"]').attributes('disabled')).toBeDefined()
    resolvePost({ results: [], aiRecommendation: null })
    await flushPromises()
  })

  // --- Results section ---

  it('does not show analysis-result before analyze is clicked', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    expect(wrapper.find('[data-testid="analysis-result"]').exists()).toBe(false)
  })

  it('shows analysis-result after successful analyze', async () => {
    mockPost.mockResolvedValue(mockMatchupResult)
    const wrapper = mountComponent()
    await flushPromises()
    await selectAll(wrapper)
    await wrapper.find('[data-testid="analyze-btn"]').trigger('click')
    await flushPromises()
    expect(wrapper.find('[data-testid="analysis-result"]').exists()).toBe(true)
  })

  it('displays verdict from matchup results', async () => {
    mockPost.mockResolvedValue(mockMatchupResult)
    const wrapper = mountComponent()
    await flushPromises()
    await selectAll(wrapper)
    await wrapper.find('[data-testid="analyze-btn"]').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('能赢')
  })

  it('displays line analysis positions in results', async () => {
    mockPost.mockResolvedValue(mockMatchupResult)
    const wrapper = mountComponent()
    await flushPromises()
    await selectAll(wrapper)
    await wrapper.find('[data-testid="analyze-btn"]').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('D1')
    expect(wrapper.text()).toContain('80% 赢')
  })

  it('displays AI recommendation explanation when aiRecommendation is present', async () => {
    mockPost.mockResolvedValue(mockMatchupResult)
    const wrapper = mountComponent()
    await flushPromises()
    await selectAll(wrapper)
    await wrapper.find('[data-testid="analyze-btn"]').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('D1组合UTR优势明显')
  })

  it('does not show AI recommendation section when aiRecommendation is null', async () => {
    mockPost.mockResolvedValue({ results: mockMatchupResult.results, aiRecommendation: null })
    const wrapper = mountComponent()
    await flushPromises()
    await selectAll(wrapper)
    await wrapper.find('[data-testid="analyze-btn"]').trigger('click')
    await flushPromises()
    expect(wrapper.text()).not.toContain('D1组合UTR优势明显')
  })

  // --- Edge cases ---

  it('does not POST when analyze is called without canAnalyze (defensive)', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    // Directly trigger without selecting
    await wrapper.find('[data-testid="analyze-btn"]').trigger('click')
    await flushPromises()
    expect(mockPost).not.toHaveBeenCalled()
  })

  it('handles empty results array gracefully', async () => {
    mockPost.mockResolvedValue({ results: [], aiRecommendation: null })
    const wrapper = mountComponent()
    await flushPromises()
    await selectAll(wrapper)
    await wrapper.find('[data-testid="analyze-btn"]').trigger('click')
    await flushPromises()
    // Should render result container without crashing
    expect(wrapper.find('[data-testid="analysis-result"]').exists()).toBe(true)
  })

  it('shows 开始分析 label on button by default', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    expect(wrapper.find('[data-testid="analyze-btn"]').text()).toContain('开始分析')
  })

  it('lineup options display label and totalUtr', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    await selectMyTeam(wrapper, [{ id: 'ml1', label: '我方排阵', totalUtr: 22.0, pairs: [] }])
    const options = wrapper.find('[data-testid="select-my-lineup"]').findAll('option')
    const lineupOption = options.find(o => o.element.value === 'ml1')
    expect(lineupOption).toBeTruthy()
    expect(lineupOption.text()).toContain('22.00')
  })

  it('opp-preview shows player names from opp lineup pairs', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    await selectOppTeam(wrapper, [{ id: 'ol1', label: '对手', pairs: samplePairs, totalUtr: 20 }])
    await wrapper.find('[data-testid="select-opp-lineup"]').setValue('ol1')
    await flushPromises()
    const preview = wrapper.find('[data-testid="opp-preview"]')
    expect(preview.text()).toContain('A')
    expect(preview.text()).toContain('B')
  })
})
