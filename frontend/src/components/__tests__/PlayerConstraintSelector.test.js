import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import PlayerConstraintSelector from '../PlayerConstraintSelector.vue'

const samplePlayers = [
  { id: 'p1', name: '张三', utr: 6.0, gender: 'male', verified: true },
  { id: 'p2', name: '李四', utr: 5.5, gender: 'male', verified: false },
  { id: 'p3', name: '赵梅', utr: 5.0, gender: 'female', verified: true },
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

  describe('排序', () => {
    it('女性球员排在男性球员前面', () => {
      const wrapper = mount(PlayerConstraintSelector, { props: { players: samplePlayers } })
      const rows = wrapper.findAll('.space-y-1 > div')
      expect(rows[0].text()).toContain('赵梅') // female
    })

    it('同性别内 UTR 高的排在前面', () => {
      const wrapper = mount(PlayerConstraintSelector, { props: { players: samplePlayers } })
      const rows = wrapper.findAll('.space-y-1 > div')
      // Male players: 张三 UTR 6.0 before 李四 UTR 5.5
      const names = rows.map(r => r.text())
      const zhangIdx = names.findIndex(t => t.includes('张三'))
      const liIdx = names.findIndex(t => t.includes('李四'))
      expect(zhangIdx).toBeLessThan(liIdx)
    })
  })

  describe('认证徽章', () => {
    it('verified=true 时显示"认证"徽章', () => {
      const wrapper = mount(PlayerConstraintSelector, { props: { players: samplePlayers } })
      const text = wrapper.text()
      // p1 (张三 verified) and p3 (赵梅 verified) should show badge
      expect(text.match(/认证/g)?.length).toBeGreaterThanOrEqual(2)
    })

    it('verified=false 时不显示认证徽章（李四）', () => {
      const wrapper = mount(PlayerConstraintSelector, { props: { players: [{ id: 'p2', name: '李四', utr: 5.5, gender: 'male', verified: false }] } })
      expect(wrapper.find('.bg-green-100').exists()).toBe(false)
    })
  })

  describe('6 态状态切换', () => {
    it('初始按钮标签为"中立"', () => {
      const wrapper = mount(PlayerConstraintSelector, { props: { players: samplePlayers } })
      expect(wrapper.findAll('button')[0].text()).toBe('中立')
    })

    it('点击一次变为 D1', async () => {
      const wrapper = mount(PlayerConstraintSelector, { props: { players: samplePlayers } })
      await wrapper.findAll('button')[0].trigger('click')
      expect(wrapper.findAll('button')[0].text()).toBe('D1')
    })

    it('点击两次变为 D2', async () => {
      const wrapper = mount(PlayerConstraintSelector, { props: { players: samplePlayers } })
      const btn = wrapper.findAll('button')[0]
      await btn.trigger('click')
      await btn.trigger('click')
      expect(btn.text()).toBe('D2')
    })

    it('点击五次变为排除', async () => {
      const wrapper = mount(PlayerConstraintSelector, { props: { players: samplePlayers } })
      const btn = wrapper.findAll('button')[0]
      for (let i = 0; i < 5; i++) await btn.trigger('click')
      expect(btn.text()).toBe('排除')
    })

    it('点击六次回到中立', async () => {
      const wrapper = mount(PlayerConstraintSelector, { props: { players: samplePlayers } })
      const btn = wrapper.findAll('button')[0]
      for (let i = 0; i < 6; i++) await btn.trigger('click')
      expect(btn.text()).toBe('中立')
    })
  })

  describe('emit update:constraints', () => {
    it('固定到 D1 时 emit pinPlayers', async () => {
      const wrapper = mount(PlayerConstraintSelector, { props: { players: samplePlayers } })
      // First button is for the first sorted player (赵梅 female) — click once → D1
      await wrapper.findAll('button')[0].trigger('click')
      const emitted = wrapper.emitted('update:constraints')
      expect(emitted).toBeTruthy()
      const last = emitted[emitted.length - 1][0]
      expect(Object.values(last.pinPlayers)).toContain('D1')
    })

    it('排除时 emit excludePlayers', async () => {
      const wrapper = mount(PlayerConstraintSelector, { props: { players: samplePlayers } })
      const btn = wrapper.findAll('button')[0]
      for (let i = 0; i < 5; i++) await btn.trigger('click')
      const emitted = wrapper.emitted('update:constraints')
      const last = emitted[emitted.length - 1][0]
      expect(last.excludePlayers.length).toBe(1)
      expect(last.pinPlayers).toEqual({})
    })

    it('摘要行更新固定位置计数', async () => {
      const wrapper = mount(PlayerConstraintSelector, { props: { players: samplePlayers } })
      await wrapper.findAll('button')[0].trigger('click') // → D1
      await nextTick()
      expect(wrapper.text()).toContain('1 人')
    })
  })
})
