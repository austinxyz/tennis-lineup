import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import LineupResultTabs from '../LineupResultTabs.vue'

const makePair = (pos) => ({
  position: pos,
  player1Id: 'p1',
  player1Name: '张三',
  player2Id: 'p2',
  player2Name: '李四',
  combinedUtr: 10.0,
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

describe('LineupResultTabs', () => {
  describe('空状态', () => {
    it('无 lineups 时显示空状态占位', () => {
      const wrapper = mount(LineupResultTabs, {
        props: { lineups: [] },
        global: { stubs: { LineupCard: true } },
      })
      expect(wrapper.text()).toContain('生成排阵后结果将显示在此处')
    })
  })

  describe('Tab 渲染', () => {
    it('为每个 lineup 渲染一个 tab 按钮', () => {
      const lineups = [makeLineup('l1'), makeLineup('l2'), makeLineup('l3')]
      const wrapper = mount(LineupResultTabs, {
        props: { lineups },
        global: { stubs: { LineupCard: true } },
      })
      const buttons = wrapper.findAll('button')
      expect(buttons).toHaveLength(3)
      expect(buttons[0].text()).toBe('方案 1')
      expect(buttons[1].text()).toBe('方案 2')
      expect(buttons[2].text()).toBe('方案 3')
    })

    it('最多 6 个 tab（3×2 网格）', () => {
      const lineups = Array.from({ length: 6 }, (_, i) => makeLineup(`l${i}`))
      const wrapper = mount(LineupResultTabs, {
        props: { lineups },
        global: { stubs: { LineupCard: true } },
      })
      expect(wrapper.findAll('button')).toHaveLength(6)
    })

    it('第一个 tab 默认激活（绿色背景）', () => {
      const lineups = [makeLineup('l1'), makeLineup('l2')]
      const wrapper = mount(LineupResultTabs, {
        props: { lineups },
        global: { stubs: { LineupCard: true } },
      })
      const firstBtn = wrapper.findAll('button')[0]
      expect(firstBtn.classes()).toContain('bg-green-600')
    })
  })

  describe('Tab 切换', () => {
    it('点击第二个 tab 后激活第二个', async () => {
      const lineups = [makeLineup('l1'), makeLineup('l2')]
      const wrapper = mount(LineupResultTabs, {
        props: { lineups },
        global: { stubs: { LineupCard: true } },
      })
      const buttons = wrapper.findAll('button')
      await buttons[1].trigger('click')
      expect(buttons[1].classes()).toContain('bg-green-600')
      expect(buttons[0].classes()).not.toContain('bg-green-600')
    })

    it('lineups 更新后重置为第一个 tab', async () => {
      const lineups = [makeLineup('l1'), makeLineup('l2')]
      const wrapper = mount(LineupResultTabs, {
        props: { lineups },
        global: { stubs: { LineupCard: true } },
      })
      // click second tab
      await wrapper.findAll('button')[1].trigger('click')
      // update lineups prop
      await wrapper.setProps({ lineups: [makeLineup('l3'), makeLineup('l4')] })
      await nextTick()
      const buttons = wrapper.findAll('button')
      expect(buttons[0].classes()).toContain('bg-green-600')
    })
  })
})
