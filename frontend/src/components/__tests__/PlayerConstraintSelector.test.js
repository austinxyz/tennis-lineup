import { describe, it, expect } from 'vitest'
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
      expect(rows[0].text()).toContain('赵梅') // female first
    })

    it('同性别内 UTR 高的排在前面', () => {
      const wrapper = mount(PlayerConstraintSelector, { props: { players: samplePlayers } })
      const rows = wrapper.findAll('.space-y-1 > div')
      const names = rows.map(r => r.text())
      const zhangIdx = names.findIndex(t => t.includes('张三'))
      const liIdx = names.findIndex(t => t.includes('李四'))
      expect(zhangIdx).toBeLessThan(liIdx)
    })
  })

  describe('性别徽章', () => {
    it('female 球员显示 F 徽章', () => {
      const wrapper = mount(PlayerConstraintSelector, { props: { players: samplePlayers } })
      const text = wrapper.text()
      expect(text).toContain('F')
    })

    it('male 球员显示 M 徽章', () => {
      const wrapper = mount(PlayerConstraintSelector, { props: { players: samplePlayers } })
      const text = wrapper.text()
      expect(text).toContain('M')
    })
  })

  describe('认证徽章', () => {
    it('verified=true 时显示\"认证\"徽章', () => {
      const wrapper = mount(PlayerConstraintSelector, { props: { players: samplePlayers } })
      const badges = wrapper.findAll('.bg-green-100')
      expect(badges.length).toBeGreaterThanOrEqual(2) // p1 和 p3 都 verified
    })

    it('verified=false 时不显示认证徽章（李四）', () => {
      const wrapper = mount(PlayerConstraintSelector, {
        props: { players: [{ id: 'p2', name: '李四', utr: 5.5, gender: 'male', verified: false }] }
      })
      expect(wrapper.find('.bg-green-100').exists()).toBe(false)
    })
  })

  describe('下拉选择器', () => {
    it('每个球员有一个 select 下拉', () => {
      const wrapper = mount(PlayerConstraintSelector, { props: { players: samplePlayers } })
      const selects = wrapper.findAll('select')
      expect(selects.length).toBe(samplePlayers.length)
    })

    it('下拉选项包含 7 个值', () => {
      const wrapper = mount(PlayerConstraintSelector, { props: { players: samplePlayers } })
      const options = wrapper.findAll('select')[0].findAll('option')
      expect(options.length).toBe(7)
      const values = options.map(o => o.element.value)
      expect(values).toContain('neutral')
      expect(values).toContain('exclude')
      expect(values).toContain('include')
      expect(values).toContain('D1')
      expect(values).toContain('D2')
      expect(values).toContain('D3')
      expect(values).toContain('D4')
    })

    it('默认值为 中立 (neutral)', () => {
      const wrapper = mount(PlayerConstraintSelector, { props: { players: samplePlayers } })
      const select = wrapper.findAll('select')[0]
      expect(select.element.value).toBe('neutral')
    })
  })

  describe('emit update:constraints', () => {
    it('选 D1 时 emit pinPlayers 和 includePlayers', async () => {
      const wrapper = mount(PlayerConstraintSelector, { props: { players: samplePlayers } })
      // First select is for sorted first player (赵梅, female)
      const select = wrapper.findAll('select')[0]
      await select.setValue('D1')
      const emitted = wrapper.emitted('update:constraints')
      expect(emitted).toBeTruthy()
      const last = emitted[emitted.length - 1][0]
      expect(Object.values(last.pinPlayers)).toContain('D1')
      expect(last.includePlayers).toContain('p3') // 赵梅 is p3
    })

    it('选 一定上 时 emit includePlayers，不包含在 pinPlayers', async () => {
      const wrapper = mount(PlayerConstraintSelector, { props: { players: samplePlayers } })
      const select = wrapper.findAll('select')[0]
      await select.setValue('include')
      const emitted = wrapper.emitted('update:constraints')
      const last = emitted[emitted.length - 1][0]
      expect(last.includePlayers).toContain('p3')
      expect(Object.keys(last.pinPlayers)).not.toContain('p3')
    })

    it('选 不上 时 emit excludePlayers', async () => {
      const wrapper = mount(PlayerConstraintSelector, { props: { players: samplePlayers } })
      const select = wrapper.findAll('select')[0]
      await select.setValue('exclude')
      const emitted = wrapper.emitted('update:constraints')
      const last = emitted[emitted.length - 1][0]
      expect(last.excludePlayers).toContain('p3')
      expect(last.pinPlayers).toEqual({})
    })

    it('摘要行更新固定位置、一定上场、排除计数', async () => {
      const wrapper = mount(PlayerConstraintSelector, { props: { players: samplePlayers } })
      const selects = wrapper.findAll('select')
      await selects[0].setValue('D1')
      await selects[1].setValue('include')
      await selects[2].setValue('exclude')
      await nextTick()
      expect(wrapper.text()).toContain('1 人') // pin count
      // at least the counts are visible
      const text = wrapper.text()
      expect(text).toContain('固定位置')
      expect(text).toContain('一定上场')
      expect(text).toContain('排除')
    })
  })
})
