import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { ref } from 'vue'
import OpponentAnalysis from '../OpponentAnalysis.vue'

const mockTeams = ref([
  { id: 'team-1', name: '己方队伍' },
  { id: 'team-2', name: '对手队伍' },
])
const mockFetchTeams = vi.fn().mockResolvedValue(undefined)

vi.mock('../../composables/useTeams', () => ({
  useTeams: () => ({
    teams: mockTeams,
    fetchTeams: mockFetchTeams,
  }),
}))

const mockRunBestThree = vi.fn()
const mockRunHeadToHead = vi.fn()
const mockRunAiAnalysis = vi.fn()
const mockRunCommentary = vi.fn()
const mockMatchupLoading = ref(false)
const mockMatchupError = ref('')

vi.mock('../../composables/useOpponentMatchup', () => ({
  useOpponentMatchup: () => ({
    loading: mockMatchupLoading,
    error: mockMatchupError,
    runBestThree: mockRunBestThree,
    runHeadToHead: mockRunHeadToHead,
    runAiAnalysis: mockRunAiAnalysis,
    runCommentary: mockRunCommentary,
  }),
}))

const mockGet = vi.fn()

vi.mock('../../composables/useApi', () => ({
  useApi: () => ({
    get: mockGet,
    post: vi.fn(),
    loading: ref(false),
    error: ref(null),
  }),
}))

const mockPairs = [
  { position: 'D1', player1Name: '甲一', player2Name: '甲二', player1Utr: 6.0, player2Utr: 5.5, combinedUtr: 11.5 },
  { position: 'D2', player1Name: '乙一', player2Name: '乙二', player1Utr: 5.5, player2Utr: 5.0, combinedUtr: 10.5 },
  { position: 'D3', player1Name: '丙一', player2Name: '丙二', player1Utr: 5.0, player2Utr: 4.5, combinedUtr: 9.5 },
  { position: 'D4', player1Name: '丁一', player2Name: '丁二', player1Utr: 4.5, player2Utr: 4.0, combinedUtr: 8.5 },
]

const mockOpponentLineups = [
  { id: 'opp-lineup-1', createdAt: '2026-01-01T00:00:00Z', totalUtr: 33.0, pairs: mockPairs },
]

const mockOwnLineups = [
  { id: 'own-lineup-1', createdAt: '2026-01-01T00:00:00Z', totalUtr: 34.0, pairs: mockPairs },
  { id: 'own-lineup-2', createdAt: '2026-01-02T00:00:00Z', totalUtr: 37.0, pairs: [] },
]

const mockLineAnalysis = [
  { position: 'D1', ownCombinedUtr: 10.0, opponentCombinedUtr: 9.0, delta: 1.0, winProbability: 0.8, label: '80% 赢' },
  { position: 'D2', ownCombinedUtr: 8.5, opponentCombinedUtr: 8.5, delta: 0.0, winProbability: 0.5, label: '对等' },
  { position: 'D3', ownCombinedUtr: 8.0, opponentCombinedUtr: 8.5, delta: -0.5, winProbability: 0.5, label: '对等' },
  { position: 'D4', ownCombinedUtr: 7.5, opponentCombinedUtr: 7.5, delta: 0.0, winProbability: 0.5, label: '对等' },
]

const mockBestThreeResults = [
  { lineup: { id: 'own-2', pairs: [] }, lineAnalysis: mockLineAnalysis, expectedScore: 7.2, opponentExpectedScore: 2.8, verdict: '能赢' },
  { lineup: { id: 'own-1', pairs: [] }, lineAnalysis: mockLineAnalysis, expectedScore: 5.5, opponentExpectedScore: 4.5, verdict: '势均力敌' },
  { lineup: { id: 'own-3', pairs: [] }, lineAnalysis: mockLineAnalysis, expectedScore: 4.5, opponentExpectedScore: 5.5, verdict: '劣势' },
]

const mockHeadToHeadResult = {
  lineup: { id: 'own-1', pairs: [] },
  lineAnalysis: mockLineAnalysis,
  expectedScore: 5.0,
  opponentExpectedScore: 5.0,
  verdict: '势均力敌',
}

const mockAiResult = {
  aiUsed: true,
  lineup: { id: 'own-2', pairs: [] },
  lineAnalysis: mockLineAnalysis,
  explanation: 'D1组合UTR优势明显',
  expectedScore: 7.0,
  opponentExpectedScore: 3.0,
  opponentLineup: { id: 'opp-lineup-1', pairs: [] },
}

const mockCommentaryResult = {
  aiUsed: true,
  lines: [
    { position: 'D1', commentary: '己方UTR优势，建议主动进攻' },
    { position: 'D2', commentary: '势均力敌，注重稳定发挥' },
    { position: 'D3', commentary: '处于劣势，多以防守反击为主' },
    { position: 'D4', commentary: '势均力敌，注重稳定发挥' },
  ],
}

function setupMockGet({ ownLineups = mockOwnLineups, opponentLineups = mockOpponentLineups } = {}) {
  mockGet.mockImplementation((url) => {
    if (url.includes('team-1')) return Promise.resolve(ownLineups)
    if (url.includes('team-2')) return Promise.resolve(opponentLineups)
    return Promise.resolve([])
  })
}

beforeEach(() => {
  mockMatchupLoading.value = false
  mockMatchupError.value = ''
  mockRunBestThree.mockResolvedValue(mockBestThreeResults)
  mockRunHeadToHead.mockResolvedValue(mockHeadToHeadResult)
  mockRunAiAnalysis.mockResolvedValue(mockAiResult)
  mockRunCommentary.mockResolvedValue(mockCommentaryResult)
  mockGet.mockResolvedValue([])
  vi.spyOn(console, 'error').mockImplementation(() => {})
})

afterEach(() => {
  vi.restoreAllMocks()
})

function mountComponent() {
  return mount(OpponentAnalysis, { global: { stubs: { RouterLink: true } } })
}

describe('OpponentAnalysis', () => {
  it('renders mode toggle buttons', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    expect(wrapper.text()).toContain('最佳三阵')
    expect(wrapper.text()).toContain('逐线对比')
  })

  it('defaults to 最佳三阵 mode', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    const buttons = wrapper.findAll('button')
    const bestThreeBtn = buttons.find(b => b.text() === '最佳三阵')
    expect(bestThreeBtn.classes()).toContain('bg-white')
  })

  it('renders team dropdowns', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    expect(wrapper.text()).toContain('己方队伍')
    expect(wrapper.text()).toContain('对手队伍')
    expect(wrapper.text()).toContain('对手排阵')
  })

  it('shows empty opponent lineup message when team has no lineups', async () => {
    setupMockGet({ opponentLineups: [] })
    const wrapper = mountComponent()
    await flushPromises()

    const opponentSelect = wrapper.findAll('select')[1]
    await opponentSelect.setValue('team-2')
    await opponentSelect.trigger('change')
    await flushPromises()

    expect(wrapper.text()).toContain('对手队伍暂无保存排阵')
  })

  it('populates opponent lineup dropdown when lineups exist', async () => {
    setupMockGet()
    const wrapper = mountComponent()
    await flushPromises()

    const opponentSelect = wrapper.findAll('select')[1]
    await opponentSelect.setValue('team-2')
    await opponentSelect.trigger('change')
    await flushPromises()

    const allSelects = wrapper.findAll('select')
    expect(allSelects.length).toBeGreaterThanOrEqual(3)
  })

  describe('排阵预览', () => {
    it('shows opponent lineup preview after selecting opponent lineup', async () => {
      setupMockGet()
      const wrapper = mountComponent()
      await flushPromises()

      await wrapper.findAll('select')[1].setValue('team-2')
      await wrapper.findAll('select')[1].trigger('change')
      await flushPromises()

      await wrapper.findAll('select')[2].setValue('opp-lineup-1')
      await flushPromises()

      expect(wrapper.text()).toContain('D1: 甲一 + 甲二')
    })

    it('shows own lineup preview in 逐线对比 mode after selecting own lineup', async () => {
      setupMockGet()
      const wrapper = mountComponent()
      await flushPromises()

      // Switch to head-to-head
      await wrapper.findAll('button').find(b => b.text() === '逐线对比').trigger('click')
      await flushPromises()

      await wrapper.findAll('select')[0].setValue('team-1')
      await wrapper.findAll('select')[0].trigger('change')
      await flushPromises()

      // Select own lineup
      const ownLineupSelect = wrapper.findAll('select').find(s =>
        s.findAll('option').some(o => o.element.value === 'own-lineup-1')
      )
      await ownLineupSelect.setValue('own-lineup-1')
      await flushPromises()

      expect(wrapper.text()).toContain('D1: 甲一 + 甲二')
    })
  })

  describe('最佳三阵 mode', () => {
    it('shows 查找最佳三阵 button', async () => {
      const wrapper = mountComponent()
      await flushPromises()
      const buttons = wrapper.findAll('button')
      const btn = buttons.find(b => b.text().includes('查找最佳三阵'))
      expect(btn.exists()).toBe(true)
    })

    it('查找最佳三阵 button is disabled when no team/lineup selected', async () => {
      const wrapper = mountComponent()
      await flushPromises()
      const btn = wrapper.findAll('button').find(b => b.text().includes('查找最佳三阵'))
      expect(btn.attributes('disabled')).toBeDefined()
    })

    async function setupAndRunBestThree(wrapper) {
      setupMockGet()
      await wrapper.findAll('select')[0].setValue('team-1')
      const opponentSelect = wrapper.findAll('select')[1]
      await opponentSelect.setValue('team-2')
      await opponentSelect.trigger('change')
      await flushPromises()

      await wrapper.findAll('select')[2].setValue('opp-lineup-1')
      await wrapper.findAll('button').find(b => b.text().includes('查找最佳三阵')).trigger('click')
      await flushPromises()
    }

    it('calls runBestThree when button clicked', async () => {
      const wrapper = mountComponent()
      await flushPromises()
      await setupAndRunBestThree(wrapper)

      expect(mockRunBestThree).toHaveBeenCalledWith('team-1', 'team-2', 'opp-lineup-1')
    })

    it('displays up to 3 result cards after successful call', async () => {
      const wrapper = mountComponent()
      await flushPromises()
      await setupAndRunBestThree(wrapper)

      expect(wrapper.text()).toContain('能赢')
      expect(wrapper.text()).toContain('势均力敌')
      expect(wrapper.text()).toContain('劣势')
    })

    it('shows per-line comparison in results', async () => {
      const wrapper = mountComponent()
      await flushPromises()
      await setupAndRunBestThree(wrapper)

      expect(wrapper.text()).toContain('D1')
      expect(wrapper.text()).toContain('80% 赢')
      expect(wrapper.text()).toContain('对等')
    })

    it('shows AI 推荐 button after best-three results are displayed', async () => {
      const wrapper = mountComponent()
      await flushPromises()
      await setupAndRunBestThree(wrapper)

      const aiBtn = wrapper.findAll('button').find(b => b.text() === '获取 AI 推荐')
      expect(aiBtn.exists()).toBe(true)
    })

    it('AI 推荐 button not shown before results', async () => {
      const wrapper = mountComponent()
      await flushPromises()

      const aiBtn = wrapper.findAll('button').find(b => b.text() === '获取 AI 推荐')
      expect(aiBtn).toBeUndefined()
    })

    it('clicking AI 推荐 shows AI recommendation card', async () => {
      const wrapper = mountComponent()
      await flushPromises()
      await setupAndRunBestThree(wrapper)

      const aiBtn = wrapper.findAll('button').find(b => b.text() === '获取 AI 推荐')
      await aiBtn.trigger('click')
      await flushPromises()

      expect(mockRunAiAnalysis).toHaveBeenCalledWith('team-1', 'team-2', 'opp-lineup-1')
      expect(wrapper.text()).toContain('AI 推荐排阵')
      expect(wrapper.text()).toContain('D1组合UTR优势明显')
    })
  })

  describe('逐线对比 mode', () => {
    async function switchToHeadToHead(wrapper) {
      const btn = wrapper.findAll('button').find(b => b.text() === '逐线对比')
      await btn.trigger('click')
      await flushPromises()
    }

    it('switching to 逐线对比 shows own lineup selector', async () => {
      const wrapper = mountComponent()
      await flushPromises()
      await switchToHeadToHead(wrapper)
      expect(wrapper.text()).toContain('己方排阵')
    })

    it('shows 对比分析 button in 逐线对比 mode', async () => {
      const wrapper = mountComponent()
      await flushPromises()
      await switchToHeadToHead(wrapper)
      const btn = wrapper.findAll('button').find(b => b.text().includes('对比分析'))
      expect(btn.exists()).toBe(true)
    })

    async function setupAndRunHeadToHead(wrapper) {
      setupMockGet()
      await switchToHeadToHead(wrapper)

      await wrapper.findAll('select')[0].setValue('team-1')
      await wrapper.findAll('select')[0].trigger('change')
      await wrapper.findAll('select')[1].setValue('team-2')
      await wrapper.findAll('select')[1].trigger('change')
      await flushPromises()

      // After fetching: selects[0]=ownTeam, [1]=oppTeam, [2]=oppLineup, [3]=ownLineup
      await wrapper.findAll('select')[2].setValue('opp-lineup-1')
      await wrapper.findAll('select')[3].setValue('own-lineup-1')

      const btn = wrapper.findAll('button').find(b => b.text().includes('对比分析'))
      await btn.trigger('click')
      await flushPromises()
    }

    it('calls runHeadToHead with own lineup id', async () => {
      const wrapper = mountComponent()
      await flushPromises()
      await setupAndRunHeadToHead(wrapper)

      expect(mockRunHeadToHead).toHaveBeenCalledWith('team-1', 'own-lineup-1', 'team-2', 'opp-lineup-1')
    })

    it('shows UTR comparison result card with verdict', async () => {
      const wrapper = mountComponent()
      await flushPromises()
      await setupAndRunHeadToHead(wrapper)

      expect(wrapper.text()).toContain('UTR 比较分析')
      expect(wrapper.text()).toContain('势均力敌')
    })

    it('shows AI 逐线评析 button after head-to-head result', async () => {
      const wrapper = mountComponent()
      await flushPromises()
      await setupAndRunHeadToHead(wrapper)

      const aiBtn = wrapper.findAll('button').find(b => b.text().includes('AI 逐线评析'))
      expect(aiBtn.exists()).toBe(true)
    })

    it('calls runCommentary and displays commentary card', async () => {
      const wrapper = mountComponent()
      await flushPromises()
      await setupAndRunHeadToHead(wrapper)

      const aiBtn = wrapper.findAll('button').find(b => b.text().includes('AI 逐线评析'))
      await aiBtn.trigger('click')
      await flushPromises()

      expect(mockRunCommentary).toHaveBeenCalledWith('team-1', 'own-lineup-1', 'team-2', 'opp-lineup-1')
      expect(wrapper.text()).toContain('AI 逐线评析')
      expect(wrapper.text()).toContain('己方UTR优势，建议主动进攻')
    })

    it('shows AI 不可用 badge when aiUsed is false in commentary', async () => {
      mockRunCommentary.mockResolvedValue({ ...mockCommentaryResult, aiUsed: false })
      const wrapper = mountComponent()
      await flushPromises()
      await setupAndRunHeadToHead(wrapper)

      const aiBtn = wrapper.findAll('button').find(b => b.text().includes('AI 逐线评析'))
      await aiBtn.trigger('click')
      await flushPromises()

      expect(wrapper.text()).toContain('AI 不可用')
    })

    it('clears previous result when switching mode', async () => {
      const wrapper = mountComponent()
      await flushPromises()
      await switchToHeadToHead(wrapper)

      // switch back to 最佳三阵
      const bestThreeBtn = wrapper.findAll('button').find(b => b.text() === '最佳三阵')
      await bestThreeBtn.trigger('click')
      await flushPromises()

      expect(wrapper.text()).not.toContain('UTR 比较分析')
    })

    it('shows empty own lineups warning when own team has no saved lineups', async () => {
      setupMockGet({ ownLineups: [] })
      const wrapper = mountComponent()
      await flushPromises()
      await switchToHeadToHead(wrapper)

      await wrapper.findAll('select')[0].setValue('team-1')
      await wrapper.findAll('select')[0].trigger('change')
      await flushPromises()

      expect(wrapper.text()).toContain('己方队伍暂无保存排阵')
    })
  })
})
