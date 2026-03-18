import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick, ref } from 'vue'
import LineupGenerator from '../LineupGenerator.vue'

const mockTeams = ref([
  { id: 'team-1', name: '测试队伍A' },
  { id: 'team-2', name: '测试队伍B' },
])
const mockFetchTeams = vi.fn().mockResolvedValue(undefined)

vi.mock('../../composables/useTeams', () => ({
  useTeams: () => ({
    teams: mockTeams,
    fetchTeams: mockFetchTeams,
  }),
}))

const mockGenerateLineup = vi.fn()
const mockLineup = ref(null)
const mockLoading = ref(false)
const mockError = ref(null)

vi.mock('../../composables/useLineup', () => ({
  useLineup: () => ({
    lineup: mockLineup,
    loading: mockLoading,
    error: mockError,
    generateLineup: mockGenerateLineup,
  }),
}))

beforeEach(() => {
  mockLineup.value = null
  mockLoading.value = false
  mockError.value = null
  mockGenerateLineup.mockReset()
  vi.spyOn(console, 'error').mockImplementation(() => {})
})

afterEach(() => {
  vi.restoreAllMocks()
})

describe('LineupGenerator', () => {
  describe('初始渲染', () => {
    it('显示页面标题', () => {
      const wrapper = mount(LineupGenerator, { global: { stubs: { StrategySelector: true, LineupCard: true } } })
      expect(wrapper.text()).toContain('排阵生成')
    })

    it('显示队伍下拉选择', () => {
      const wrapper = mount(LineupGenerator, { global: { stubs: { StrategySelector: true, LineupCard: true } } })
      const select = wrapper.find('select')
      expect(select.exists()).toBe(true)
    })

    it('队伍列表填充了 teams', () => {
      const wrapper = mount(LineupGenerator, { global: { stubs: { StrategySelector: true, LineupCard: true } } })
      const options = wrapper.findAll('option')
      expect(options.some(o => o.text().includes('测试队伍A'))).toBe(true)
      expect(options.some(o => o.text().includes('测试队伍B'))).toBe(true)
    })

    it('未选择队伍时生成按钮禁用', () => {
      const wrapper = mount(LineupGenerator, { global: { stubs: { StrategySelector: true, LineupCard: true } } })
      const btn = wrapper.find('button[disabled]')
      expect(btn.exists()).toBe(true)
    })

    it('初始状态不显示 LineupCard', () => {
      const wrapper = mount(LineupGenerator, { global: { stubs: { StrategySelector: true, LineupCard: true } } })
      expect(wrapper.findComponent({ name: 'LineupCard' }).exists()).toBe(false)
    })
  })

  describe('生成流程', () => {
    it('选择队伍后生成按钮启用', async () => {
      const wrapper = mount(LineupGenerator, { global: { stubs: { StrategySelector: true, LineupCard: true } } })
      await wrapper.find('select').setValue('team-1')
      await nextTick()
      const btn = wrapper.find('button')
      expect(btn.attributes('disabled')).toBeUndefined()
    })

    it('点击生成按钮调用 generateLineup', async () => {
      mockGenerateLineup.mockResolvedValue({ id: 'lineup-1', pairs: [] })
      const wrapper = mount(LineupGenerator, { global: { stubs: { StrategySelector: true, LineupCard: true } } })
      await wrapper.find('select').setValue('team-1')
      await nextTick()
      await wrapper.find('button').trigger('click')
      expect(mockGenerateLineup).toHaveBeenCalledWith(expect.objectContaining({
        teamId: 'team-1',
      }))
    })

    it('生成成功后显示 LineupCard', async () => {
      mockGenerateLineup.mockImplementation(() => {
        mockLineup.value = { id: 'lineup-1', strategy: 'balanced', pairs: [], totalUtr: 30, aiUsed: false, violationMessages: [] }
        return Promise.resolve(mockLineup.value)
      })
      const wrapper = mount(LineupGenerator, { global: { stubs: { StrategySelector: true, LineupCard: true } } })
      await wrapper.find('select').setValue('team-1')
      await flushPromises()
      await wrapper.find('button').trigger('click')
      await flushPromises()
      expect(wrapper.findComponent({ name: 'LineupCard' }).exists()).toBe(true)
    })

    it('loading 状态时按钮显示"生成中..."', async () => {
      const wrapper = mount(LineupGenerator, { global: { stubs: { StrategySelector: true, LineupCard: true } } })
      await wrapper.find('select').setValue('team-1')
      mockLoading.value = true
      await nextTick()
      expect(wrapper.text()).toContain('生成中...')
    })
  })

  describe('错误处理', () => {
    it('生成失败时显示错误消息', async () => {
      mockGenerateLineup.mockRejectedValue(new Error('队伍球员不足8人，无法生成排阵'))
      const wrapper = mount(LineupGenerator, { global: { stubs: { StrategySelector: true, LineupCard: true } } })
      await wrapper.find('select').setValue('team-1')
      await flushPromises()
      await wrapper.find('button').trigger('click')
      await flushPromises()
      expect(wrapper.text()).toContain('队伍球员不足8人')
    })

    it('重新生成前清除之前的错误消息', async () => {
      mockGenerateLineup
        .mockRejectedValueOnce(new Error('第一次失败'))
        .mockResolvedValueOnce({ id: 'lineup-1', pairs: [] })
      const wrapper = mount(LineupGenerator, { global: { stubs: { StrategySelector: true, LineupCard: true } } })
      await wrapper.find('select').setValue('team-1')
      await flushPromises()
      await wrapper.find('button').trigger('click')
      await flushPromises()
      expect(wrapper.text()).toContain('第一次失败')
      await wrapper.find('button').trigger('click')
      await flushPromises()
      expect(wrapper.text()).not.toContain('第一次失败')
    })
  })
})
