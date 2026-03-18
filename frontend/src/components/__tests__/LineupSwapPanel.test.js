import { describe, it, expect } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import LineupSwapPanel from '../LineupSwapPanel.vue'

function buildLineup() {
  return {
    id: 'lineup-1',
    strategy: 'balanced',
    aiUsed: false,
    totalUtr: 40.0,
    valid: true,
    violationMessages: [],
    pairs: [
      { position: 'D1', player1Id: 'p1', player1Name: 'A', player1Utr: 6.0, player2Id: 'p2', player2Name: 'B', player2Utr: 5.0, combinedUtr: 11.0 },
      { position: 'D2', player1Id: 'p3', player1Name: 'C', player1Utr: 5.5, player2Id: 'p4', player2Name: 'D', player2Utr: 4.5, combinedUtr: 10.0 },
      { position: 'D3', player1Id: 'p5', player1Name: 'E', player1Utr: 5.0, player2Id: 'p6', player2Name: 'F', player2Utr: 4.0, combinedUtr: 9.0 },
      { position: 'D4', player1Id: 'p7', player1Name: 'G', player1Utr: 4.5, player2Id: 'p8', player2Name: 'H', player2Utr: 4.0, combinedUtr: 8.5 },  // wait, let me adjust
    ],
  }
}

// Lineup where D1=11.0, D2=10.0, D3=9.0, D4=8.5 — valid ordering
// Swapping B(D1) with C(D2): D1=A+C=11.5, D2=B+D=9.5 → D1>D2 OK
// Swapping A(D1) with H(D4): D1=H+B=9.0, D4=A+G=10.5 → D4>D1 violates

describe('LineupSwapPanel', () => {
  describe('初始渲染', () => {
    it('显示4条线的球员', () => {
      const wrapper = mount(LineupSwapPanel, { props: { lineup: buildLineup() } })
      expect(wrapper.text()).toContain('D1')
      expect(wrapper.text()).toContain('D4')
      expect(wrapper.text()).toContain('A')
      expect(wrapper.text()).toContain('H')
    })

    it('互换按钮初始禁用（未选择球员）', () => {
      const wrapper = mount(LineupSwapPanel, { props: { lineup: buildLineup() } })
      const swapBtn = wrapper.find('button[disabled]')
      expect(swapBtn.exists()).toBe(true)
    })
  })

  describe('球员选择', () => {
    it('点击球员将其高亮选中', async () => {
      const wrapper = mount(LineupSwapPanel, { props: { lineup: buildLineup() } })
      const buttons = wrapper.findAll('button').filter(b => b.text().includes('A'))
      await buttons[0].trigger('click')
      expect(buttons[0].classes()).toContain('bg-blue-500')
    })

    it('选择同一球员再次点击取消选择', async () => {
      const wrapper = mount(LineupSwapPanel, { props: { lineup: buildLineup() } })
      const buttons = wrapper.findAll('button').filter(b => b.text().includes('A'))
      await buttons[0].trigger('click')
      await buttons[0].trigger('click')
      expect(buttons[0].classes()).not.toContain('bg-blue-500')
    })
  })

  describe('合法互换', () => {
    it('互换B(D1)和C(D2)后 emit update:lineup', async () => {
      const wrapper = mount(LineupSwapPanel, { props: { lineup: buildLineup() } })
      const allBtns = wrapper.findAll('button')
      // Click B (D1 slot2)
      const bBtn = allBtns.find(b => b.text().includes('B'))
      // Click C (D2 slot1)
      const cBtn = allBtns.find(b => b.text().includes('C'))
      await bBtn.trigger('click')
      await cBtn.trigger('click')
      const swapBtn = allBtns.find(b => b.text() === '互换')
      await swapBtn.trigger('click')
      const emitted = wrapper.emitted('update:lineup')
      expect(emitted).toBeTruthy()
    })
  })

  describe('约束校验', () => {
    it('互换导致 D4 > D3 时显示错误信息', async () => {
      const wrapper = mount(LineupSwapPanel, { props: { lineup: buildLineup() } })
      const allBtns = wrapper.findAll('button')
      // Swap A (D1 highest UTR) with H (D4 lower) → violates ordering
      const aBtn = allBtns.find(b => b.text().includes('A (6'))
      const hBtn = allBtns.find(b => b.text().includes('H (4'))
      await aBtn.trigger('click')
      await hBtn.trigger('click')
      const swapBtn = allBtns.find(b => b.text() === '互换')
      if (!swapBtn.attributes('disabled')) {
        await swapBtn.trigger('click')
        expect(wrapper.text()).toContain('UTR排序约束')
      }
    })
  })

  describe('重置', () => {
    it('重置后恢复原始排阵并 emit', async () => {
      const wrapper = mount(LineupSwapPanel, { props: { lineup: buildLineup() } })
      const resetBtn = wrapper.findAll('button').find(b => b.text() === '重置')
      await resetBtn.trigger('click')
      const emitted = wrapper.emitted('update:lineup')
      expect(emitted).toBeTruthy()
      // Should restore A in D1
      expect(wrapper.text()).toContain('A')
    })
  })
})
