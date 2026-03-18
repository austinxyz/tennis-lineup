import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import StrategySelector from '../StrategySelector.vue'

describe('StrategySelector', () => {
  describe('初始渲染', () => {
    it('默认显示预设策略模式', () => {
      const wrapper = mount(StrategySelector)
      expect(wrapper.text()).toContain('预设策略')
      expect(wrapper.text()).toContain('均衡')
      expect(wrapper.text()).toContain('集中火力')
    })

    it('默认不显示自然语言输入框', () => {
      const wrapper = mount(StrategySelector)
      expect(wrapper.find('textarea').exists()).toBe(false)
    })

    it('初始化时 emit balanced preset strategy', () => {
      const wrapper = mount(StrategySelector)
      const emitted = wrapper.emitted('update:strategy')
      expect(emitted).toBeTruthy()
      expect(emitted[0][0]).toEqual({
        strategyType: 'preset',
        preset: 'balanced',
        naturalLanguage: null,
      })
    })
  })

  describe('模式切换', () => {
    it('点击自定义策略按钮切换到自定义模式', async () => {
      const wrapper = mount(StrategySelector)
      await wrapper.find('button:nth-child(2)').trigger('click')
      expect(wrapper.find('textarea').exists()).toBe(true)
    })

    it('切换到自定义模式后 emit custom strategy', async () => {
      const wrapper = mount(StrategySelector)
      await wrapper.find('button:nth-child(2)').trigger('click')
      const emitted = wrapper.emitted('update:strategy')
      const lastEmit = emitted[emitted.length - 1][0]
      expect(lastEmit.strategyType).toBe('custom')
      expect(lastEmit.preset).toBeNull()
    })

    it('从自定义切换回预设模式后 textarea 消失', async () => {
      const wrapper = mount(StrategySelector)
      await wrapper.find('button:nth-child(2)').trigger('click')
      expect(wrapper.find('textarea').exists()).toBe(true)
      await wrapper.find('button:nth-child(1)').trigger('click')
      expect(wrapper.find('textarea').exists()).toBe(false)
    })
  })

  describe('预设策略选择', () => {
    it('点击集中火力 emit aggressive preset', async () => {
      const wrapper = mount(StrategySelector)
      const presetButtons = wrapper.findAll('.flex.gap-3 button')
      await presetButtons[1].trigger('click')
      const emitted = wrapper.emitted('update:strategy')
      const lastEmit = emitted[emitted.length - 1][0]
      expect(lastEmit).toEqual({
        strategyType: 'preset',
        preset: 'aggressive',
        naturalLanguage: null,
      })
    })

    it('点击均衡 emit balanced preset', async () => {
      const wrapper = mount(StrategySelector)
      const presetButtons = wrapper.findAll('.flex.gap-3 button')
      await presetButtons[0].trigger('click')
      const emitted = wrapper.emitted('update:strategy')
      const lastEmit = emitted[emitted.length - 1][0]
      expect(lastEmit.preset).toBe('balanced')
    })
  })
})
