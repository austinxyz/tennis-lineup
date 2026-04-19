import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { ref } from 'vue'
import AppHeader from '../AppHeader.vue'

// Mock vue-router
const mockRouterPush = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: vi.fn(() => ({ push: mockRouterPush })),
}))

beforeEach(() => {
  mockRouterPush.mockReset()
})

/**
 * Helper: mount AppHeader with optional provide and props
 */
function mountHeader(props = {}, sidebarOpen = null) {
  const options = {
    props: { title: '测试标题', ...props },
    global: {},
  }
  if (sidebarOpen !== null) {
    options.global.provide = { sidebarOpen }
  }
  return mount(AppHeader, options)
}

describe('AppHeader', () => {
  // ── Rendering ────────────────────────────────────────────────────────────────

  it('renders the title prop', () => {
    const wrapper = mountHeader({ title: '排阵生成' })
    expect(wrapper.text()).toContain('排阵生成')
  })

  it('renders hamburger button with data-testid="hamburger"', () => {
    const wrapper = mountHeader()
    expect(wrapper.find('[data-testid="hamburger"]').exists()).toBe(true)
  })

  it('hamburger aria-label is "打开导航" when sidebar closed', () => {
    const wrapper = mountHeader({}, ref(false))
    const btn = wrapper.find('[data-testid="hamburger"]')
    expect(btn.attributes('aria-label')).toBe('打开导航')
    expect(btn.attributes('aria-expanded')).toBe('false')
  })

  it('hamburger aria-label is "关闭导航" when sidebar open', () => {
    const wrapper = mountHeader({}, ref(true))
    const btn = wrapper.find('[data-testid="hamburger"]')
    expect(btn.attributes('aria-label')).toBe('关闭导航')
    expect(btn.attributes('aria-expanded')).toBe('true')
  })

  it('hamburger and back buttons have type="button" (safe inside forms)', () => {
    const wrapper = mountHeader({ backTo: '/' })
    expect(wrapper.find('[data-testid="hamburger"]').attributes('type')).toBe('button')
    expect(wrapper.find('[data-testid="back-btn"]').attributes('type')).toBe('button')
  })

  it('back button has aria-label set to backLabel', () => {
    const wrapper = mountHeader({ backTo: '/', backLabel: '队伍列表' })
    expect(wrapper.find('[data-testid="back-btn"]').attributes('aria-label')).toBe('队伍列表')
  })

  it('header element has lg:hidden class', () => {
    const wrapper = mountHeader()
    const header = wrapper.find('header')
    expect(header.classes()).toContain('lg:hidden')
  })

  // ── Back button ──────────────────────────────────────────────────────────────

  it('does not render back button when backTo prop is not set', () => {
    const wrapper = mountHeader({ title: '首页' })
    expect(wrapper.find('[data-testid="back-btn"]').exists()).toBe(false)
  })

  it('renders back button when backTo prop is set', () => {
    const wrapper = mountHeader({ title: '详情', backTo: '/teams' })
    expect(wrapper.find('[data-testid="back-btn"]').exists()).toBe(true)
  })

  it('back button shows default backLabel "返回"', () => {
    const wrapper = mountHeader({ title: '详情', backTo: '/teams' })
    expect(wrapper.find('[data-testid="back-btn"]').text()).toContain('返回')
  })

  it('back button shows custom backLabel when provided', () => {
    const wrapper = mountHeader({ title: '详情', backTo: '/teams', backLabel: '队伍列表' })
    expect(wrapper.find('[data-testid="back-btn"]').text()).toContain('队伍列表')
  })

  it('clicking back button calls router.push with backTo value', async () => {
    const wrapper = mountHeader({ title: '详情', backTo: '/teams/123' })
    await wrapper.find('[data-testid="back-btn"]').trigger('click')
    expect(mockRouterPush).toHaveBeenCalledWith('/teams/123')
  })

  // ── Hamburger toggles sidebarOpen ────────────────────────────────────────────

  it('clicking hamburger toggles sidebarOpen ref from false to true', async () => {
    const sidebarOpen = ref(false)
    const wrapper = mountHeader({}, sidebarOpen)
    await wrapper.find('[data-testid="hamburger"]').trigger('click')
    expect(sidebarOpen.value).toBe(true)
  })

  it('clicking hamburger toggles sidebarOpen ref from true to false', async () => {
    const sidebarOpen = ref(true)
    const wrapper = mountHeader({}, sidebarOpen)
    await wrapper.find('[data-testid="hamburger"]').trigger('click')
    expect(sidebarOpen.value).toBe(false)
  })

  // ── Actions slot ─────────────────────────────────────────────────────────────

  it('renders content placed in the actions slot', () => {
    const wrapper = mount(AppHeader, {
      props: { title: '首页' },
      slots: {
        actions: '<button data-testid="custom-action">操作</button>',
      },
    })
    expect(wrapper.find('[data-testid="custom-action"]').exists()).toBe(true)
  })

  // ── Fallback safety ─────────────────────────────────────────────────────────

  it('hamburger click does not throw when sidebarOpen is NOT provided (fallback ref)', async () => {
    const wrapper = mount(AppHeader, { props: { title: 'X' } })
    // Should not throw; the fallback is a local ref
    await wrapper.find('[data-testid="hamburger"]').trigger('click')
    // If it didn't throw, aria-expanded should flip
    expect(wrapper.find('[data-testid="hamburger"]').attributes('aria-expanded')).toBe('true')
  })
})
