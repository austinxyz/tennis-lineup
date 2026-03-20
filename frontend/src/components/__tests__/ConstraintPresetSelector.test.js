import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import ConstraintPresetSelector from '../ConstraintPresetSelector.vue'

const samplePlayers = [
  { id: 'p1', name: 'Alice', utr: 6.0, gender: 'female' },
  { id: 'p2', name: 'Bob', utr: 5.5, gender: 'male' },
  { id: 'p3', name: 'Carol', utr: 5.0, gender: 'female' },
]

const samplePresets = [
  {
    id: 'preset-1',
    name: '全攻',
    excludePlayers: [],
    includePlayers: ['p1'],
    pinPlayers: { p2: 'D1' },
  },
  {
    id: 'preset-2',
    name: '稳守',
    excludePlayers: ['p3'],
    includePlayers: [],
    pinPlayers: {},
  },
]

function makeWrapper(props = {}) {
  return mount(ConstraintPresetSelector, {
    props: {
      presets: samplePresets,
      players: samplePlayers,
      currentConstraints: { excludePlayers: [], includePlayers: [], pinPlayers: {} },
      ...props,
    },
  })
}

describe('ConstraintPresetSelector', () => {
  describe('空预设占位符', () => {
    it('无预设时显示"暂无预设"', () => {
      const wrapper = mount(ConstraintPresetSelector, {
        props: { presets: [], players: [], currentConstraints: {} },
      })
      expect(wrapper.text()).toContain('暂无预设')
    })

    it('有预设时 select 包含预设名称', () => {
      const wrapper = makeWrapper()
      expect(wrapper.text()).toContain('全攻')
      expect(wrapper.text()).toContain('稳守')
    })
  })

  describe('加载预设', () => {
    it('选择预设后点击加载触发 load-preset 事件', async () => {
      const wrapper = makeWrapper()
      await wrapper.find('select').setValue('preset-1')
      await nextTick()
      await wrapper.findAll('button')[0].trigger('click') // 加载 button
      const events = wrapper.emitted('load-preset')
      expect(events).toBeTruthy()
      expect(events[0][0]).toMatchObject({
        includePlayers: ['p1'],
        pinPlayers: { p2: 'D1' },
      })
    })

    it('未选择预设时加载按钮禁用', () => {
      const wrapper = makeWrapper()
      const loadBtn = wrapper.findAll('button')[0]
      expect(loadBtn.attributes('disabled')).toBeDefined()
    })

    it('加载预设时过滤不在队伍中的球员', async () => {
      const presetWithMissingPlayer = [{
        id: 'preset-missing',
        name: '含离队球员',
        excludePlayers: ['p-gone'],
        includePlayers: ['p1', 'p-gone'],
        pinPlayers: { 'p-gone': 'D1', p2: 'D2' },
      }]
      const wrapper = mount(ConstraintPresetSelector, {
        props: {
          presets: presetWithMissingPlayer,
          players: samplePlayers,
          currentConstraints: {},
        },
      })
      await wrapper.find('select').setValue('preset-missing')
      await nextTick()
      await wrapper.findAll('button')[0].trigger('click')
      const event = wrapper.emitted('load-preset')[0][0]
      // p-gone should be filtered out
      expect(event.includePlayers).toEqual(['p1'])
      expect(event.excludePlayers).toEqual([])
      expect(event.pinPlayers).toEqual({ p2: 'D2' })
    })

    it('有球员被过滤时显示警告提示', async () => {
      const presetWithMissingPlayer = [{
        id: 'preset-missing',
        name: '含离队球员',
        excludePlayers: ['p-gone'],
        includePlayers: [],
        pinPlayers: {},
      }]
      const wrapper = mount(ConstraintPresetSelector, {
        props: {
          presets: presetWithMissingPlayer,
          players: samplePlayers,
          currentConstraints: {},
        },
      })
      await wrapper.find('select').setValue('preset-missing')
      await nextTick()
      await wrapper.findAll('button')[0].trigger('click')
      await nextTick()
      expect(wrapper.text()).toContain('部分球员已不在队伍中')
    })
  })

  describe('保存预设', () => {
    it('输入名称并点击保存触发 save-preset 事件', async () => {
      const wrapper = makeWrapper()
      const input = wrapper.find('input[type="text"]')
      await input.setValue('新预设')
      await nextTick()
      // 3rd button: 加载, 删除, 保存
      const saveBtn = wrapper.findAll('button')[2]
      await saveBtn.trigger('click')
      const events = wrapper.emitted('save-preset')
      expect(events).toBeTruthy()
      expect(events[0][0]).toBe('新预设')
    })

    it('名称为空时保存按钮禁用', () => {
      const wrapper = makeWrapper()
      const saveBtn = wrapper.findAll('button')[2]
      expect(saveBtn.attributes('disabled')).toBeDefined()
    })

    it('按 Enter 键也触发保存', async () => {
      const wrapper = makeWrapper()
      const input = wrapper.find('input[type="text"]')
      await input.setValue('快捷保存')
      await input.trigger('keyup.enter')
      const events = wrapper.emitted('save-preset')
      expect(events).toBeTruthy()
      expect(events[0][0]).toBe('快捷保存')
    })
  })

  describe('删除预设', () => {
    it('选择预设后点击删除触发 delete-preset 事件', async () => {
      const wrapper = makeWrapper()
      await wrapper.find('select').setValue('preset-2')
      await nextTick()
      const deleteBtn = wrapper.findAll('button')[1] // delete button
      await deleteBtn.trigger('click')
      const events = wrapper.emitted('delete-preset')
      expect(events).toBeTruthy()
      expect(events[0][0]).toBe('preset-2')
    })

    it('删除后清空已选预设', async () => {
      const wrapper = makeWrapper()
      await wrapper.find('select').setValue('preset-1')
      await nextTick()
      const deleteBtn = wrapper.findAll('button')[1]
      await deleteBtn.trigger('click')
      await nextTick()
      // After delete, selected preset should be cleared
      expect(wrapper.find('select').element.value).toBe('')
    })

    it('未选择预设时删除按钮禁用', () => {
      const wrapper = makeWrapper()
      const deleteBtn = wrapper.findAll('button')[1]
      expect(deleteBtn.attributes('disabled')).toBeDefined()
    })
  })
})
