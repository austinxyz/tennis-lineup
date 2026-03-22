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

const mockAnalyzeOpponent = vi.fn()
const mockAnalysisLoading = ref(false)
const mockAnalysisResult = ref(null)

vi.mock('../../composables/useOpponentAnalysis', () => ({
  useOpponentAnalysis: () => ({
    loading: mockAnalysisLoading,
    error: ref(null),
    result: mockAnalysisResult,
    analyzeOpponent: mockAnalyzeOpponent,
  }),
}))

const mockRunMatchup = vi.fn()
const mockMatchupLoading = ref(false)
const mockMatchupResults = ref([])

vi.mock('../../composables/useSavedLineupMatchup', () => ({
  useSavedLineupMatchup: () => ({
    loading: mockMatchupLoading,
    error: ref(null),
    matchupResults: mockMatchupResults,
    runMatchup: mockRunMatchup,
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

const stubs = {
  LineupCard: true,
  RouterLink: true,
}

const mockOpponentLineups = [
  { id: 'opp-lineup-1', createdAt: '2026-01-01T00:00:00Z', totalUtr: 33.0, pairs: [] },
]

const mockAnalysisResponse = {
  utrRecommendation: {
    lineup: { id: 'lineup-1', pairs: [] },
    lineAnalysis: [
      { position: 'D1', ownCombinedUtr: 9.5, opponentCombinedUtr: 8.5, delta: 1.0, winProbability: 0.6, label: '60% 赢' },
      { position: 'D2', ownCombinedUtr: 8.5, opponentCombinedUtr: 8.5, delta: 0.0, winProbability: 0.5, label: '对等' },
      { position: 'D3', ownCombinedUtr: 8.0, opponentCombinedUtr: 8.5, delta: -0.5, winProbability: 0.5, label: '对等' },
      { position: 'D4', ownCombinedUtr: 7.0, opponentCombinedUtr: 8.0, delta: -1.0, winProbability: 0.4, label: '60% 输' },
    ],
    expectedScore: 5.0,
    opponentExpectedScore: 5.0,
  },
  aiRecommendation: {
    lineup: { id: 'lineup-1', pairs: [] },
    explanation: 'AI 根据对手排阵选择最优方案',
    aiUsed: true,
  },
}

const mockMatchupResponse = [
  {
    lineup: { id: 'own-lineup-2', createdAt: '2026-01-02T00:00:00Z', totalUtr: 37.0, pairs: [] },
    lineAnalysis: [
      { position: 'D1', ownCombinedUtr: 10.0, opponentCombinedUtr: 9.0, delta: 1.0, winProbability: 0.8, label: '80% 赢' },
    ],
    expectedScore: 7.2,
    opponentExpectedScore: 2.8,
    verdict: '能赢',
  },
  {
    lineup: { id: 'own-lineup-1', createdAt: '2026-01-01T00:00:00Z', totalUtr: 33.0, pairs: [] },
    lineAnalysis: [
      { position: 'D1', ownCombinedUtr: 8.0, opponentCombinedUtr: 9.0, delta: -1.0, winProbability: 0.4, label: '60% 输' },
    ],
    expectedScore: 4.0,
    opponentExpectedScore: 6.0,
    verdict: '势均力敌',
  },
]

beforeEach(() => {
  mockAnalysisResult.value = null
  mockAnalysisLoading.value = false
  mockMatchupResults.value = []
  mockMatchupLoading.value = false
  mockAnalyzeOpponent.mockResolvedValue(mockAnalysisResponse)
  mockRunMatchup.mockResolvedValue(mockMatchupResponse)
  mockGet.mockResolvedValue([])
  vi.spyOn(console, 'error').mockImplementation(() => {})
})

afterEach(() => {
  vi.restoreAllMocks()
})

function mountComponent() {
  return mount(OpponentAnalysis, {
    global: { stubs },
  })
}

describe('OpponentAnalysis', () => {
  describe('排阵生成 mode (default)', () => {
    it('renders team dropdowns with teams from useTeams', async () => {
      const wrapper = mountComponent()
      await flushPromises()
      const selects = wrapper.findAll('select')
      expect(selects.length).toBeGreaterThanOrEqual(2)
      expect(wrapper.text()).toContain('己方队伍')
      expect(wrapper.text()).toContain('对手队伍')
    })

    it('analyze button is disabled when no own team selected', () => {
      const wrapper = mountComponent()
      const button = wrapper.find('button[disabled]')
      expect(button.exists()).toBe(true)
    })

    it('shows empty opponent lineups message when team has no lineups', async () => {
      mockGet.mockResolvedValue([])
      const wrapper = mountComponent()
      await flushPromises()

      const opponentSelect = wrapper.findAll('select')[1]
      await opponentSelect.setValue('team-2')
      await opponentSelect.trigger('change')
      await flushPromises()

      expect(wrapper.text()).toContain('对手队伍暂无保存排阵')
    })

    it('populates opponent lineup dropdown when lineups exist', async () => {
      mockGet.mockResolvedValue(mockOpponentLineups)
      const wrapper = mountComponent()
      await flushPromises()

      const opponentSelect = wrapper.findAll('select')[1]
      await opponentSelect.setValue('team-2')
      await opponentSelect.trigger('change')
      await flushPromises()

      const allSelects = wrapper.findAll('select')
      expect(allSelects.length).toBe(3)
    })

    it('calls analyzeOpponent when analyze button clicked', async () => {
      mockGet.mockResolvedValue(mockOpponentLineups)
      const wrapper = mountComponent()
      await flushPromises()

      await wrapper.findAll('select')[0].setValue('team-1')
      const opponentSelect = wrapper.findAll('select')[1]
      await opponentSelect.setValue('team-2')
      await opponentSelect.trigger('change')
      await flushPromises()

      await wrapper.findAll('select')[2].setValue('opp-lineup-1')
      const analyzeBtn = wrapper.findAll('button').find(b => b.text() === '分析')
      await analyzeBtn.trigger('click')
      await flushPromises()

      expect(mockAnalyzeOpponent).toHaveBeenCalledWith('team-1', 'team-2', 'opp-lineup-1', {})
    })

    it('renders UTR panel and AI panel after analysis', async () => {
      mockAnalysisResult.value = mockAnalysisResponse
      const wrapper = mountComponent()
      await flushPromises()

      expect(wrapper.text()).toContain('UTR 比较推荐')
      expect(wrapper.text()).toContain('AI 建议')
      expect(wrapper.text()).toContain('预期得分')
      expect(wrapper.text()).toContain('60% 赢')
      expect(wrapper.text()).toContain('对等')
      expect(wrapper.text()).toContain('60% 输')
    })

    it('shows AI not available warning when aiUsed is false', async () => {
      mockAnalysisResult.value = {
        ...mockAnalysisResponse,
        aiRecommendation: {
          lineup: { id: 'lineup-1', pairs: [] },
          explanation: 'AI 不可用，已用UTR分析代替',
          aiUsed: false,
        },
      }
      const wrapper = mountComponent()
      await flushPromises()

      expect(wrapper.text()).toContain('AI 不可用')
      expect(wrapper.text()).toContain('AI 不可用，已用UTR分析代替')
    })

    it('shows error message on API failure', async () => {
      mockAnalyzeOpponent.mockRejectedValue(new Error('服务器错误'))
      mockAnalysisResult.value = null
      mockGet.mockResolvedValue(mockOpponentLineups)

      const wrapper = mountComponent()
      await flushPromises()

      await wrapper.findAll('select')[0].setValue('team-1')
      const opponentSelect = wrapper.findAll('select')[1]
      await opponentSelect.setValue('team-2')
      await opponentSelect.trigger('change')
      await flushPromises()

      await wrapper.findAll('select')[2].setValue('opp-lineup-1')
      const analyzeBtn = wrapper.findAll('button').find(b => b.text() === '分析')
      await analyzeBtn.trigger('click')
      await flushPromises()

      expect(wrapper.text()).toContain('服务器错误')
    })
  })

  describe('已保存对比 mode', () => {
    async function switchToSavedMode(wrapper) {
      const tabs = wrapper.findAll('button')
      const savedTab = tabs.find(b => b.text() === '已保存对比')
      await savedTab.trigger('click')
      await flushPromises()
    }

    it('mode toggle switches to 已保存对比 tab', async () => {
      const wrapper = mountComponent()
      await flushPromises()
      await switchToSavedMode(wrapper)

      const tabs = wrapper.findAll('button')
      const savedTab = tabs.find(b => b.text() === '已保存对比')
      expect(savedTab.classes()).toContain('border-green-600')
    })

    it('shows 对比 button in 已保存对比 mode', async () => {
      const wrapper = mountComponent()
      await flushPromises()
      await switchToSavedMode(wrapper)

      const buttons = wrapper.findAll('button')
      const compareBtn = buttons.find(b => b.text().includes('对比'))
      expect(compareBtn.exists()).toBe(true)
    })

    it('对比 button is disabled when own team not selected', async () => {
      const wrapper = mountComponent()
      await flushPromises()
      await switchToSavedMode(wrapper)

      const buttons = wrapper.findAll('button')
      const compareBtn = buttons.find(b => b.text() === '对比')
      expect(compareBtn.attributes('disabled')).toBeDefined()
    })

    it('calls runMatchup when 对比 button clicked', async () => {
      mockGet.mockResolvedValue(mockOpponentLineups)
      const wrapper = mountComponent()
      await flushPromises()
      await switchToSavedMode(wrapper)

      await wrapper.findAll('select')[0].setValue('team-1')
      await wrapper.findAll('select')[0].trigger('change')
      const opponentSelect = wrapper.findAll('select')[1]
      await opponentSelect.setValue('team-2')
      await opponentSelect.trigger('change')
      await flushPromises()

      await wrapper.findAll('select')[2].setValue('opp-lineup-1')
      const compareBtn = wrapper.findAll('button').find(b => b.text() === '对比')
      await compareBtn.trigger('click')
      await flushPromises()

      expect(mockRunMatchup).toHaveBeenCalledWith('team-1', 'team-2', 'opp-lineup-1')
    })

    it('shows empty own lineups warning when own team has no saved lineups', async () => {
      mockGet.mockResolvedValue([])
      const wrapper = mountComponent()
      await flushPromises()
      await switchToSavedMode(wrapper)

      await wrapper.findAll('select')[0].setValue('team-1')
      await wrapper.findAll('select')[0].trigger('change')
      await flushPromises()

      expect(wrapper.text()).toContain('己方队伍暂无保存排阵')
    })

    it('renders matchup results with verdicts ranked by score', async () => {
      mockMatchupResults.value = mockMatchupResponse
      const wrapper = mountComponent()
      await flushPromises()
      await switchToSavedMode(wrapper)

      expect(wrapper.text()).toContain('能赢')
      expect(wrapper.text()).toContain('势均力敌')
      expect(wrapper.text()).toContain('预期得分')
      // First result should appear before second
      const text = wrapper.text()
      expect(text.indexOf('能赢')).toBeLessThan(text.indexOf('势均力敌'))
    })
  })
})
