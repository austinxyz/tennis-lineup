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
      // The number of gender tag spans should equal the number of non-null genders
      // New layout: gender tags use data-testid="gender-tag"
      const genderTags = wrapper.findAll('[data-testid="gender-tag"]')
      // D1: both null (0 tags), D2: male+male (2), D3: null+male (1), D4: male+male (2) = 5 total
      expect(genderTags.length).toBe(5)
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

    it('hides per-player 实: when no actualUtr override set', () => {
      const wrapper = mount(LineupCard, { props: { lineup: buildLineup() } })
      // No actualUtr overrides — 实: should not appear per player
      expect(wrapper.text()).not.toContain('实:')
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

  // ─── pair-player-row 2-row refactor ──────────────────────────────────────

  describe('pair-player-row 双行布局 (showPlayerUtr=true)', () => {
    it('每个 pair 渲染恰好 2 个 pair-player-row 元素', () => {
      const wrapper = mount(LineupCard, { props: { lineup: buildLineup() } })
      const rows = wrapper.findAll('[data-testid="pair-player-row"]')
      // 4 pairs × 2 rows each = 8 rows total
      expect(rows.length).toBe(8)
    })

    it('第一行显示 player1 信息，第二行显示 player2 信息', () => {
      const wrapper = mount(LineupCard, { props: { lineup: buildLineup() } })
      // D1 is the first pair rendered (sorted). Get its two rows.
      const rows = wrapper.findAll('[data-testid="pair-player-row"]')
      expect(rows[0].text()).toContain('张三') // D1 player1
      expect(rows[1].text()).toContain('李四') // D1 player2
    })

    it('female 性别标签有 pink 样式类', () => {
      const wrapper = mount(LineupCard, { props: { lineup: buildLineup() } })
      // D1 player2 李四 is female
      const rows = wrapper.findAll('[data-testid="pair-player-row"]')
      const femaleRow = rows[1] // D1 player2 李四 (female)
      const genderTag = femaleRow.find('[data-testid="gender-tag"]')
      expect(genderTag.classes()).toContain('bg-pink-100')
      expect(genderTag.classes()).toContain('text-pink-700')
      expect(genderTag.text()).toBe('女')
    })

    it('male 性别标签有 blue 样式类', () => {
      const wrapper = mount(LineupCard, { props: { lineup: buildLineup() } })
      // D1 player1 张三 is male
      const rows = wrapper.findAll('[data-testid="pair-player-row"]')
      const maleRow = rows[0] // D1 player1 张三 (male)
      const genderTag = maleRow.find('[data-testid="gender-tag"]')
      expect(genderTag.classes()).toContain('bg-blue-100')
      expect(genderTag.classes()).toContain('text-blue-700')
      expect(genderTag.text()).toBe('男')
    })

    it('actualUtr 与 utr 不同时显示 实:{value}，与 utr 相同时不显示', () => {
      const pairsWithMixedActualUtr = [
        {
          position: 'D1',
          player1Name: 'Alice', player1Utr: 6.0, player1Gender: 'female', player1ActualUtr: 6.80,
          player2Name: 'Bob',   player2Utr: 5.5, player2Gender: 'male',   player2ActualUtr: 5.5, // same → no 实:
          combinedUtr: 11.5,
        },
        { position: 'D2', player1Name: '王五', player1Utr: 4.5, player1Gender: 'male', player2Name: '赵六', player2Utr: 3.5, player2Gender: 'male', combinedUtr: 8.0 },
        { position: 'D3', player1Name: '孙七', player1Utr: 2.5, player1Gender: 'female', player2Name: '周八', player2Utr: 2.0, player2Gender: 'male', combinedUtr: 4.5 },
        { position: 'D4', player1Name: '吴九', player1Utr: 1.5, player1Gender: 'male', player2Name: '郑十', player2Utr: 1.0, player2Gender: 'male', combinedUtr: 2.5 },
      ]
      const wrapper = mount(LineupCard, { props: { lineup: buildLineup({ pairs: pairsWithMixedActualUtr }) } })
      // Alice has actualUtr 6.80 ≠ utr 6.0 → should show 实:6.80
      expect(wrapper.text()).toContain('实:6.80')
      // Bob has actualUtr 5.5 === utr 5.5 → should NOT show 实: for Bob's row
      const rows = wrapper.findAll('[data-testid="pair-player-row"]')
      const bobRow = rows[1] // D1 player2
      expect(bobRow.text()).not.toContain('实:')
    })

    it('combined UTR 在右列正确显示', () => {
      const wrapper = mount(LineupCard, { props: { lineup: buildLineup() } })
      // D1 combinedUtr = 15.5
      expect(wrapper.text()).toContain('15.50')
    })

    it('当两名球员都无 actualUtr 时，右列不显示 "实:"', () => {
      const lineup = {
        pairs: [{
          position: 'D1',
          player1Name: '张三', player1Utr: 6.0, player1Gender: 'male', player1ActualUtr: null,
          player2Name: '李四', player2Utr: 5.5, player2Gender: 'male', player2ActualUtr: null,
          combinedUtr: 11.5,
        }],
        totalUtr: 11.5, strategy: 'balanced', aiUsed: false,
      }
      const wrapper = mount(LineupCard, { props: { lineup } })
      // No "实:" inside any pair row or its right column
      const pairBlock = wrapper.find('.grid.grid-cols-\\[36px_1fr_auto\\]')
      expect(pairBlock.text()).not.toContain('实:')
    })

    it('只有一名球员有 actualUtr 时，右列只求和非 null 值（不混入 utr 回退）', () => {
      const lineup = {
        pairs: [{
          position: 'D1',
          player1Name: '张三', player1Utr: 8.0, player1Gender: 'male', player1ActualUtr: 7.0,
          player2Name: '李四', player2Utr: 6.0, player2Gender: 'female', player2ActualUtr: null,
          combinedUtr: 14.0,
        }],
        totalUtr: 14.0, strategy: 'balanced', aiUsed: false,
      }
      const wrapper = mount(LineupCard, { props: { lineup } })
      // Right column should show 实:7.00 (only player1's actual, not 7.0 + 6.0 = 13.0)
      const pairBlock = wrapper.find('.grid.grid-cols-\\[36px_1fr_auto\\]')
      expect(pairBlock.text()).toContain('实:7.00')
      expect(pairBlock.text()).not.toContain('实:13.00')
    })
  })

  describe('showPlayerUtr=false 时退回简单布局', () => {
    it('不渲染 pair-player-row 元素', () => {
      const wrapper = mount(LineupCard, { props: { lineup: buildLineup(), showPlayerUtr: false } })
      const rows = wrapper.findAll('[data-testid="pair-player-row"]')
      expect(rows.length).toBe(0)
    })

    it('以 player1Name / player2Name 格式显示球员名', () => {
      const wrapper = mount(LineupCard, { props: { lineup: buildLineup(), showPlayerUtr: false } })
      expect(wrapper.text()).toContain('张三')
      expect(wrapper.text()).toContain('李四')
    })

    it('combined UTR 仍然显示', () => {
      const wrapper = mount(LineupCard, { props: { lineup: buildLineup(), showPlayerUtr: false } })
      expect(wrapper.text()).toContain('15.50')
    })

    it('不渲染性别标签', () => {
      const wrapper = mount(LineupCard, { props: { lineup: buildLineup(), showPlayerUtr: false } })
      const genderTags = wrapper.findAll('[data-testid="gender-tag"]')
      expect(genderTags.length).toBe(0)
    })
  })
})
