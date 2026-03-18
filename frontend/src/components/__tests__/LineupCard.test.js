import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import LineupCard from '../LineupCard.vue'

function buildLineup(overrides = {}) {
  return {
    id: 'lineup-001',
    strategy: 'balanced',
    aiUsed: false,
    totalUtr: 30.5,
    valid: true,
    violationMessages: [],
    pairs: [
      { position: 'D1', player1Name: '张三', player2Name: '李四', combinedUtr: 15.5 },
      { position: 'D2', player1Name: '王五', player2Name: '赵六', combinedUtr: 8.0 },
      { position: 'D3', player1Name: '孙七', player2Name: '周八', combinedUtr: 4.5 },
      { position: 'D4', player1Name: '吴九', player2Name: '郑十', combinedUtr: 2.5 },
    ],
    ...overrides,
  }
}

describe('LineupCard', () => {
  describe('渲染', () => {
    it('显示所有4个位置', () => {
      const wrapper = mount(LineupCard, { props: { lineup: buildLineup() } })
      expect(wrapper.text()).toContain('D1')
      expect(wrapper.text()).toContain('D2')
      expect(wrapper.text()).toContain('D3')
      expect(wrapper.text()).toContain('D4')
    })

    it('显示球员名', () => {
      const wrapper = mount(LineupCard, { props: { lineup: buildLineup() } })
      expect(wrapper.text()).toContain('张三')
      expect(wrapper.text()).toContain('李四')
    })

    it('显示组合 UTR，格式为两位小数', () => {
      const wrapper = mount(LineupCard, { props: { lineup: buildLineup() } })
      expect(wrapper.text()).toContain('15.50')
    })

    it('显示总 UTR', () => {
      const wrapper = mount(LineupCard, { props: { lineup: buildLineup() } })
      expect(wrapper.text()).toContain('30.50')
    })
  })

  describe('AI 标志', () => {
    it('aiUsed=true 时显示 AI 优选标签', () => {
      const wrapper = mount(LineupCard, { props: { lineup: buildLineup({ aiUsed: true }) } })
      expect(wrapper.text()).toContain('AI 优选')
    })

    it('aiUsed=false 时显示启发式标签', () => {
      const wrapper = mount(LineupCard, { props: { lineup: buildLineup({ aiUsed: false }) } })
      expect(wrapper.text()).toContain('启发式')
    })

    it('aiUsed=false 时显示降级提示', () => {
      const wrapper = mount(LineupCard, { props: { lineup: buildLineup({ aiUsed: false }) } })
      expect(wrapper.text()).toContain('AI 不可用')
    })
  })

  describe('位置排序', () => {
    it('pairs 按 D1 D2 D3 D4 顺序渲染，不管传入顺序', () => {
      const reversedPairs = [
        { position: 'D4', player1Name: '吴九', player2Name: '郑十', combinedUtr: 2.5 },
        { position: 'D3', player1Name: '孙七', player2Name: '周八', combinedUtr: 4.5 },
        { position: 'D2', player1Name: '王五', player2Name: '赵六', combinedUtr: 8.0 },
        { position: 'D1', player1Name: '张三', player2Name: '李四', combinedUtr: 15.5 },
      ]
      const wrapper = mount(LineupCard, { props: { lineup: buildLineup({ pairs: reversedPairs }) } })
      const positions = wrapper.findAll('[class*="font-bold"]').map(el => el.text())
      expect(positions[0]).toBe('D1')
    })
  })
})
