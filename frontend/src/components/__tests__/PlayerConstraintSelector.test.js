import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import PlayerConstraintSelector from '../PlayerConstraintSelector.vue'

const samplePlayers = [
  { id: 'p1', name: '张三', utr: 6.0 },
  { id: 'p2', name: '李四', utr: 5.5 },
  { id: 'p3', name: '王五', utr: 5.0 },
]

describe('PlayerConstraintSelector', () => {
  describe('空状态', () => {
    it('无 players 时显示占位文字', () => {
      const wrapper = mount(PlayerConstraintSelector, { props: { players: [] } })
      expect(wrapper.text()).toContain('请先选择队伍')
    })

    it('players 为 undefined 时显示占位文字', () => {
      const wrapper = mount(PlayerConstraintSelector)
      expect(wrapper.text()).toContain('请先选择队伍')
    })
  })

  describe('球员列表渲染', () => {
    it('显示所有球员姓名和 UTR', () => {
      const wrapper = mount(PlayerConstraintSelector, { props: { players: samplePlayers } })
      expect(wrapper.text()).toContain('张三')
      expect(wrapper.text()).toContain('6')
      expect(wrapper.text()).toContain('李四')
    })

    it('显示约束摘要行', () => {
      const wrapper = mount(PlayerConstraintSelector, { props: { players: samplePlayers } })
      expect(wrapper.text()).toContain('必须上场')
      expect(wrapper.text()).toContain('排除')
    })

    it('初始状态摘要显示 0 人', () => {
      const wrapper = mount(PlayerConstraintSelector, { props: { players: samplePlayers } })
      // 必须上场: 0 人 / 排除: 0 人
      const text = wrapper.text()
      expect(text).toContain('必须上场')
      expect(text).toContain('0 人')
    })
  })

  describe('状态切换', () => {
    it('初始按钮标签为"中立"', () => {
      const wrapper = mount(PlayerConstraintSelector, { props: { players: samplePlayers } })
      const buttons = wrapper.findAll('button')
      expect(buttons[0].text()).toBe('中立')
    })

    it('点击一次切换为"必须上场"', async () => {
      const wrapper = mount(PlayerConstraintSelector, { props: { players: samplePlayers } })
      const btn = wrapper.findAll('button')[0]
      await btn.trigger('click')
      expect(btn.text()).toBe('必须上场')
    })

    it('点击两次切换为"排除"', async () => {
      const wrapper = mount(PlayerConstraintSelector, { props: { players: samplePlayers } })
      const btn = wrapper.findAll('button')[0]
      await btn.trigger('click')
      await btn.trigger('click')
      expect(btn.text()).toBe('排除')
    })

    it('点击三次回到"中立"', async () => {
      const wrapper = mount(PlayerConstraintSelector, { props: { players: samplePlayers } })
      const btn = wrapper.findAll('button')[0]
      await btn.trigger('click')
      await btn.trigger('click')
      await btn.trigger('click')
      expect(btn.text()).toBe('中立')
    })
  })

  describe('emit update:constraints', () => {
    it('切换为必须上场时 emit includePlayers', async () => {
      const wrapper = mount(PlayerConstraintSelector, { props: { players: samplePlayers } })
      await wrapper.findAll('button')[0].trigger('click')
      const emitted = wrapper.emitted('update:constraints')
      expect(emitted).toBeTruthy()
      expect(emitted[emitted.length - 1][0].includePlayers).toContain('p1')
      expect(emitted[emitted.length - 1][0].excludePlayers).toEqual([])
    })

    it('切换为排除时 emit excludePlayers', async () => {
      const wrapper = mount(PlayerConstraintSelector, { props: { players: samplePlayers } })
      const btn = wrapper.findAll('button')[0]
      await btn.trigger('click')
      await btn.trigger('click')
      const emitted = wrapper.emitted('update:constraints')
      const last = emitted[emitted.length - 1][0]
      expect(last.includePlayers).toEqual([])
      expect(last.excludePlayers).toContain('p1')
    })

    it('摘要行更新计数', async () => {
      const wrapper = mount(PlayerConstraintSelector, { props: { players: samplePlayers } })
      await wrapper.findAll('button')[0].trigger('click')
      await nextTick()
      // 必须上场: 1 人
      expect(wrapper.text()).toContain('1 人')
    })
  })
})
