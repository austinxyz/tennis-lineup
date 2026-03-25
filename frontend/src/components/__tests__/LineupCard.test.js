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
      { position: 'D1', player1Name: '张三', player1Utr: 8.0, player1Gender: 'male', player2Name: '李四', player2Utr: 7.5, player2Gender: 'female', combinedUtr: 15.5 },
      { position: 'D2', player1Name: '王五', player1Utr: 4.5, player1Gender: 'male', player2Name: '赵六', player2Utr: 3.5, player2Gender: 'male', combinedUtr: 8.0 },
      { position: 'D3', player1Name: '孙七', player1Utr: 2.5, player1Gender: 'female', player2Name: '周八', player2Utr: 2.0, player2Gender: 'male', combinedUtr: 4.5 },
      { position: 'D4', player1Name: '吴九', player1Utr: 1.5, player1Gender: 'male', player2Name: '郑十', player2Utr: 1.0, player2Gender: 'male', combinedUtr: 2.5 },
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

  describe('per-player UTR 显示', () => {
    it('默认显示球员 UTR（showPlayerUtr=true）', () => {
      const wrapper = mount(LineupCard, { props: { lineup: buildLineup() } })
      expect(wrapper.text()).toContain('8')   // player1Utr
      expect(wrapper.text()).toContain('7.5') // player2Utr
    })

    it('showPlayerUtr=false 时不显示括号中的 UTR', () => {
      const wrapper = mount(LineupCard, { props: { lineup: buildLineup(), showPlayerUtr: false } })
      // player names still shown
      expect(wrapper.text()).toContain('张三')
      expect(wrapper.text()).toContain('李四')
      // UTR values in parens not shown - the text should just be "张三 / 李四"
      expect(wrapper.text()).not.toContain('(8')
    })
  })

  describe('性别显示', () => {
    it('male 球员显示 男 文字', () => {
      const wrapper = mount(LineupCard, { props: { lineup: buildLineup() } })
      expect(wrapper.text()).toContain('男')
    })

    it('female 球员显示 女 文字', () => {
      const wrapper = mount(LineupCard, { props: { lineup: buildLineup() } })
      expect(wrapper.text()).toContain('女')
    })

    it('性别显示与 UTR 共存', () => {
      const wrapper = mount(LineupCard, { props: { lineup: buildLineup() } })
      const text = wrapper.text()
      // D1 pair: male 张三 (8) / female 李四 (7.5)
      expect(text).toContain('男')
      expect(text).toContain('女')
      expect(text).toContain('8')
      expect(text).toContain('7.5')
    })

    it('player1Gender 为 null 时不显示性别文字', () => {
      const pairsWithNullGender = [
        { position: 'D1', player1Name: '张三', player1Utr: 8.0, player1Gender: null, player2Name: '李四', player2Utr: 7.5, player2Gender: null, combinedUtr: 15.5 },
        { position: 'D2', player1Name: '王五', player1Utr: 4.5, player1Gender: 'male', player2Name: '赵六', player2Utr: 3.5, player2Gender: 'male', combinedUtr: 8.0 },
        { position: 'D3', player1Name: '孙七', player1Utr: 2.5, player1Gender: null, player2Name: '周八', player2Utr: 2.0, player2Gender: 'male', combinedUtr: 4.5 },
        { position: 'D4', player1Name: '吴九', player1Utr: 1.5, player1Gender: 'male', player2Name: '郑十', player2Utr: 1.0, player2Gender: 'male', combinedUtr: 2.5 },
      ]
      const wrapper = mount(LineupCard, { props: { lineup: buildLineup({ pairs: pairsWithNullGender }) } })
      // 张三 and 李四 have null gender — should not show 男 or 女 for them
      // 王五/赵六 are male — will show 男, so we can't assert "no 男 at all"
      // But we can assert the component does NOT default null to "男"
      // The number of gender spans should equal the number of non-null genders
      const spans = wrapper.findAll('span.text-xs.font-semibold')
      // D1: both null (0 spans), D2: male+male (2), D3: null+male (1), D4: male+male (2) = 5 total
      expect(spans.length).toBe(5)
    })
  })

  describe('actualUtr 显示', () => {
    it('shows actualUtr per player when non-null', () => {
      const pairsWithActualUtr = [
        { position: 'D1', player1Name: '张三', player1Utr: 8.0, player1Gender: 'male', player1ActualUtr: 7.0, player2Name: '李四', player2Utr: 7.5, player2Gender: 'female', player2ActualUtr: null, combinedUtr: 15.5 },
        { position: 'D2', player1Name: '王五', player1Utr: 4.5, player1Gender: 'male', player1ActualUtr: null, player2Name: '赵六', player2Utr: 3.5, player2Gender: 'male', player2ActualUtr: null, combinedUtr: 8.0 },
        { position: 'D3', player1Name: '孙七', player1Utr: 2.5, player1Gender: 'female', player1ActualUtr: null, player2Name: '周八', player2Utr: 2.0, player2Gender: 'male', player2ActualUtr: null, combinedUtr: 4.5 },
        { position: 'D4', player1Name: '吴九', player1Utr: 1.5, player1Gender: 'male', player1ActualUtr: null, player2Name: '郑十', player2Utr: 1.0, player2Gender: 'male', player2ActualUtr: null, combinedUtr: 2.5 },
      ]
      const wrapper = mount(LineupCard, { props: { lineup: buildLineup({ pairs: pairsWithActualUtr }) } })
      expect(wrapper.text()).toContain('实:7.00')
    })

    it('shows actualUtrSum in header when differs from totalUtr', () => {
      const wrapper = mount(LineupCard, { props: { lineup: buildLineup({ totalUtr: 38.5, actualUtrSum: 41.0 }) } })
      expect(wrapper.text()).toContain('实际 UTR: 41.00')
    })

    it('always shows actualUtr per player using official UTR as fallback when no override set', () => {
      const wrapper = mount(LineupCard, { props: { lineup: buildLineup() } })
      // No actualUtr overrides — official UTR is used as effective actual
      expect(wrapper.text()).toContain('实:8.00')
      expect(wrapper.text()).toContain('实:7.50')
    })

    it('always shows actualUtrSum in header', () => {
      const wrapper = mount(LineupCard, { props: { lineup: buildLineup({ totalUtr: 30.5 }) } })
      expect(wrapper.text()).toContain('实际 UTR:')
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
