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
      { position: 'D4', player1Id: 'p7', player1Name: 'G', player1Utr: 4.5, player2Id: 'p8', player2Name: 'H', player2Utr: 4.0, combinedUtr: 8.5 },
    ],
  }
}

// D1=11.0, D2=10.0, D3=9.0, D4=8.5 — valid ordering
// Swapping B(D1) with C(D2): new D1=A+C=11.5, new D2=B+D=9.5 → D1>D2 OK
// Swapping A(D1,6.0) with H(D4,4.0): new D1=H+B=9.0, new D4=A+G=10.5 → triggers auto-sort → D4 becomes D1

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
      const bBtn = allBtns.find(b => b.text().includes('B'))
      const cBtn = allBtns.find(b => b.text().includes('C'))
      await bBtn.trigger('click')
      await cBtn.trigger('click')
      const swapBtn = allBtns.find(b => b.text() === '互换')
      await swapBtn.trigger('click')
      const emitted = wrapper.emitted('update:lineup')
      expect(emitted).toBeTruthy()
    })
  })

  describe('自动重排（顺序违反时）', () => {
    it('互换导致顺序违反时自动重排并 emit，不显示错误', async () => {
      const wrapper = mount(LineupSwapPanel, { props: { lineup: buildLineup() } })
      const allBtns = wrapper.findAll('button')
      // Swap A (D1, player1, utr=6.0) with H (D4, player2, utr=4.0)
      // After swap: D1 has H+B=9.0, D4 has G+A=10.5 → ordering violated → auto-sort
      const aBtn = allBtns.find(b => b.text().includes('A (6'))
      const hBtn = allBtns.find(b => b.text().includes('H (4'))
      await aBtn.trigger('click')
      await hBtn.trigger('click')
      const swapBtn = allBtns.find(b => b.text() === '互换')
      await swapBtn.trigger('click')
      // Should emit (auto-sort happened, no error)
      const emitted = wrapper.emitted('update:lineup')
      expect(emitted).toBeTruthy()
      // No error text should appear
      expect(wrapper.text()).not.toContain('UTR排序约束')
    })
  })

  describe('重置', () => {
    it('重置后恢复原始排阵并 emit', async () => {
      const wrapper = mount(LineupSwapPanel, { props: { lineup: buildLineup() } })
      const resetBtn = wrapper.findAll('button').find(b => b.text() === '重置')
      await resetBtn.trigger('click')
      const emitted = wrapper.emitted('update:lineup')
      expect(emitted).toBeTruthy()
      expect(wrapper.text()).toContain('A')
    })
  })
})
