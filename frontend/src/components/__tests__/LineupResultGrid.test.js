import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import LineupResultGrid from '../LineupResultGrid.vue'

const mockSaveLineup = vi.fn()

vi.mock('../../composables/useLineup', () => ({
  useLineup: () => ({
    lineups: { value: [] },
    loading: { value: false },
    generateLineup: vi.fn(),
    saveLineup: mockSaveLineup,
    fetchLineupHistory: vi.fn(),
    deleteLineup: vi.fn(),
  }),
}))

const makePair = (pos) => ({
  position: pos,
  player1Id: 'p1', player1Name: '张三', player1Utr: 6.0,
  player2Id: 'p2', player2Name: '李四', player2Utr: 5.5,
  combinedUtr: 11.5,
})

const makeLineup = (id) => ({
  id,
  strategy: 'balanced',
  aiUsed: false,
  totalUtr: 30.0,
  valid: true,
  violationMessages: [],
  pairs: ['D1', 'D2', 'D3', 'D4'].map(makePair),
})

const stubs = { LineupCard: true, LineupSwapPanel: true }

beforeEach(() => {
  mockSaveLineup.mockReset()
  vi.spyOn(console, 'error').mockImplementation(() => {})
})

afterEach(() => {
  vi.restoreAllMocks()
})

describe('LineupResultGrid', () => {
  describe('空状态', () => {
    it('无 lineups 时显示空状态占位', () => {
      const wrapper = mount(LineupResultGrid, { props: { lineups: [] }, global: { stubs } })
      expect(wrapper.text()).toContain('生成排阵后结果将显示在此处')
    })
  })

  describe('方案展示', () => {
    it('所有方案同时可见，无需点击 tab', () => {
      const lineups = [makeLineup('l1'), makeLineup('l2'), makeLineup('l3')]
      const wrapper = mount(LineupResultGrid, { props: { lineups }, global: { stubs } })
      expect(wrapper.text()).toContain('方案 1')
      expect(wrapper.text()).toContain('方案 2')
      expect(wrapper.text()).toContain('方案 3')
    })

    it('方案 1 显示"最佳"标签', () => {
      const lineups = [makeLineup('l1'), makeLineup('l2')]
      const wrapper = mount(LineupResultGrid, { props: { lineups }, global: { stubs } })
      expect(wrapper.text()).toContain('最佳')
    })

    it('方案 2 不显示"最佳"标签', () => {
      const lineups = [makeLineup('l1'), makeLineup('l2')]
      const wrapper = mount(LineupResultGrid, { props: { lineups }, global: { stubs } })
      const planHeaders = wrapper.findAll('.flex.items-center.gap-2')
      expect(planHeaders[1].text()).not.toContain('最佳')
    })

    it('方案 1 有绿色边框高亮', () => {
      const lineups = [makeLineup('l1'), makeLineup('l2')]
      const wrapper = mount(LineupResultGrid, { props: { lineups }, global: { stubs } })
      const firstCard = wrapper.find('.ring-2')
      expect(firstCard.exists()).toBe(true)
      expect(firstCard.classes()).toContain('ring-green-400')
    })

    it('最多6个方案全部显示', () => {
      const lineups = Array.from({ length: 6 }, (_, i) => makeLineup(`l${i}`))
      const wrapper = mount(LineupResultGrid, { props: { lineups }, global: { stubs } })
      for (let i = 1; i <= 6; i++) {
        expect(wrapper.text()).toContain(`方案 ${i}`)
      }
    })
  })

  describe('保留排阵', () => {
    it('每个排阵卡片显示"保留此排阵"按钮', () => {
      const lineups = [makeLineup('l1'), makeLineup('l2')]
      const wrapper = mount(LineupResultGrid, { props: { lineups, teamId: 'team-1' }, global: { stubs } })
      const saveButtons = wrapper.findAll('button').filter(b => b.text().includes('保留此排阵'))
      expect(saveButtons).toHaveLength(2)
    })

    it('点击"保留此排阵"后按钮变为"已保留 ✓"并消失', async () => {
      mockSaveLineup.mockResolvedValue({ id: 'lineup-saved' })
      const lineups = [makeLineup('l1')]
      const wrapper = mount(LineupResultGrid, { props: { lineups, teamId: 'team-1' }, global: { stubs } })

      const saveBtn = wrapper.findAll('button').find(b => b.text().includes('保留此排阵'))
      await saveBtn.trigger('click')
      await flushPromises()

      expect(wrapper.text()).toContain('已保留 ✓')
      const remainingSaveBtns = wrapper.findAll('button').filter(b => b.text().includes('保留此排阵'))
      expect(remainingSaveBtns).toHaveLength(0)
    })

    it('保留失败时显示错误信息且按钮仍可用', async () => {
      mockSaveLineup.mockRejectedValue(new Error('保存失败'))
      const lineups = [makeLineup('l1')]
      const wrapper = mount(LineupResultGrid, { props: { lineups, teamId: 'team-1' }, global: { stubs } })

      const saveBtn = wrapper.findAll('button').find(b => b.text().includes('保留此排阵'))
      await saveBtn.trigger('click')
      await flushPromises()

      expect(wrapper.text()).toContain('保存失败')
      // Button still present
      const remainingSaveBtns = wrapper.findAll('button').filter(b => b.text().includes('保留此排阵'))
      expect(remainingSaveBtns).toHaveLength(1)
    })
  })
})
