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
const mockLineups = ref([])
const mockLoading = ref(false)

vi.mock('../../composables/useLineup', () => ({
  useLineup: () => ({
    lineups: mockLineups,
    loading: mockLoading,
    generateLineup: mockGenerateLineup,
  }),
}))

const mockFetchPlayers = vi.fn().mockResolvedValue(undefined)
const mockPlayers = ref([])

vi.mock('../../composables/usePlayers', () => ({
  usePlayers: () => ({
    players: mockPlayers,
    fetchPlayers: mockFetchPlayers,
  }),
}))

const mockFetchPresets = vi.fn().mockResolvedValue(undefined)
const mockSavePreset = vi.fn().mockResolvedValue({})
const mockDeletePreset = vi.fn().mockResolvedValue(undefined)
const mockPresets = ref([])

vi.mock('../../composables/useConstraintPresets', () => ({
  useConstraintPresets: () => ({
    presets: mockPresets,
    loading: ref(false),
    error: ref(null),
    fetchPresets: mockFetchPresets,
    savePreset: mockSavePreset,
    deletePreset: mockDeletePreset,
  }),
}))

const stubs = {
  StrategySelector: true,
  PlayerConstraintSelector: true,
  ConstraintPresetSelector: true,
  LineupResultGrid: true,
}

beforeEach(() => {
  mockLineups.value = []
  mockLoading.value = false
  mockPlayers.value = []
  mockPresets.value = []
  mockGenerateLineup.mockReset()
  mockFetchPlayers.mockReset().mockResolvedValue(undefined)
  mockFetchPresets.mockReset().mockResolvedValue(undefined)
  vi.spyOn(console, 'error').mockImplementation(() => {})
})

afterEach(() => {
  vi.restoreAllMocks()
})

describe('LineupGenerator', () => {
  describe('两栏布局', () => {
    it('显示页面标题', () => {
      const wrapper = mount(LineupGenerator, { global: { stubs } })
      expect(wrapper.text()).toContain('排阵生成')
    })

    it('左栏包含队伍选择', () => {
      const wrapper = mount(LineupGenerator, { global: { stubs } })
      expect(wrapper.find('select').exists()).toBe(true)
    })

    it('左栏包含 StrategySelector', () => {
      const wrapper = mount(LineupGenerator, { global: { stubs } })
      expect(wrapper.findComponent({ name: 'StrategySelector' }).exists()).toBe(true)
    })

    it('左栏包含 PlayerConstraintSelector', () => {
      const wrapper = mount(LineupGenerator, { global: { stubs } })
      expect(wrapper.findComponent({ name: 'PlayerConstraintSelector' }).exists()).toBe(true)
    })

    it('右栏包含 LineupResultGrid', () => {
      const wrapper = mount(LineupGenerator, { global: { stubs } })
      expect(wrapper.findComponent({ name: 'LineupResultGrid' }).exists()).toBe(true)
    })

    it('左栏 lg:w-2/5 右栏 lg:w-3/5', () => {
      const wrapper = mount(LineupGenerator, { global: { stubs } })
      const html = wrapper.html()
      expect(html).toContain('lg:w-2/5')
      expect(html).toContain('lg:w-3/5')
    })
  })

  describe('初始渲染', () => {
    it('队伍列表填充了 teams', () => {
      const wrapper = mount(LineupGenerator, { global: { stubs } })
      const options = wrapper.findAll('option')
      expect(options.some(o => o.text().includes('测试队伍A'))).toBe(true)
      expect(options.some(o => o.text().includes('测试队伍B'))).toBe(true)
    })

    it('未选择队伍时生成按钮禁用', () => {
      const wrapper = mount(LineupGenerator, { global: { stubs } })
      const btn = wrapper.find('button[disabled]')
      expect(btn.exists()).toBe(true)
    })

    it('LineupResultGrid 接收空 lineups 数组', () => {
      const wrapper = mount(LineupGenerator, { global: { stubs } })
      const grid = wrapper.findComponent({ name: 'LineupResultGrid' })
      expect(grid.props('lineups')).toEqual([])
    })
  })

  describe('生成流程', () => {
    it('选择队伍后生成按钮启用', async () => {
      const wrapper = mount(LineupGenerator, { global: { stubs } })
      await wrapper.find('select').setValue('team-1')
      await nextTick()
      const btn = wrapper.find('button')
      expect(btn.attributes('disabled')).toBeUndefined()
    })

    it('点击生成按钮调用 generateLineup 并传递 constraints', async () => {
      mockGenerateLineup.mockResolvedValue([{ id: 'lineup-1', pairs: [] }])
      const wrapper = mount(LineupGenerator, { global: { stubs } })
      await wrapper.find('select').setValue('team-1')
      await nextTick()
      await wrapper.find('button').trigger('click')
      expect(mockGenerateLineup).toHaveBeenCalledWith(expect.objectContaining({
        teamId: 'team-1',
        pinPlayers: {},
        includePlayers: [],
        excludePlayers: [],
      }))
    })

    it('loading 状态时按钮显示"生成中..."', async () => {
      const wrapper = mount(LineupGenerator, { global: { stubs } })
      await wrapper.find('select').setValue('team-1')
      mockLoading.value = true
      await nextTick()
      expect(wrapper.text()).toContain('生成中...')
    })
  })

  describe('错误处理', () => {
    it('生成失败时显示错误消息', async () => {
      mockGenerateLineup.mockRejectedValue(new Error('队伍球员不足8人，无法生成排阵'))
      const wrapper = mount(LineupGenerator, { global: { stubs } })
      await wrapper.find('select').setValue('team-1')
      await flushPromises()
      await wrapper.find('button').trigger('click')
      await flushPromises()
      expect(wrapper.text()).toContain('队伍球员不足8人')
    })

    it('重新生成前清除之前的错误消息', async () => {
      mockGenerateLineup
        .mockRejectedValueOnce(new Error('第一次失败'))
        .mockResolvedValueOnce([{ id: 'lineup-1', pairs: [] }])
      const wrapper = mount(LineupGenerator, { global: { stubs } })
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
