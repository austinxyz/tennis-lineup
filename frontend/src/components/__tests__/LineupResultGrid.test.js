import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import LineupResultGrid from '../LineupResultGrid.vue'

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
})
