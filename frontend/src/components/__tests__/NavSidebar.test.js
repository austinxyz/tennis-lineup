import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import NavSidebar from '../NavSidebar.vue'

// ── vue-router mock ────────────────────────────────────────────────────────────
vi.mock('vue-router', () => ({
  useRoute: vi.fn(),
  RouterLink: {
    name: 'RouterLink',
    // Emitting 'click' from the stub allows the parent @click handler to fire
    template: '<a :href="to" v-bind="$attrs"><slot /></a>',
    props: ['to'],
    inheritAttrs: false,
  },
}))

import { useRoute } from 'vue-router'

// RouterLink stub used in mount global options
const RouterLinkStub = {
  name: 'RouterLink',
  template: '<a :href="to" v-bind="$attrs"><slot /></a>',
  props: ['to'],
  inheritAttrs: false,
}

function mountSidebar(path = '/') {
  useRoute.mockReturnValue({ path, params: {} })
  return mount(NavSidebar, {
    global: { stubs: { RouterLink: RouterLinkStub } },
  })
}

describe('NavSidebar', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders the app name 网球排阵管理', () => {
    const wrapper = mountSidebar('/')
    expect(wrapper.text()).toContain('网球排阵管理')
  })

  it('renders 队伍管理 nav link', () => {
    const wrapper = mountSidebar('/')
    expect(wrapper.text()).toContain('队伍管理')
  })

  it('applies active class when route is "/"', () => {
    const wrapper = mountSidebar('/')
    const link = wrapper.find('a[href="/"]')
    expect(link.classes()).toContain('bg-green-50')
    expect(link.classes()).toContain('text-green-700')
  })

  it('applies active class when route starts with /teams', () => {
    const wrapper = mountSidebar('/teams/123')
    const link = wrapper.find('a[href="/"]')
    expect(link.classes()).toContain('bg-green-50')
    expect(link.classes()).toContain('text-green-700')
  })

  it('does not apply active class on unrelated route', () => {
    const wrapper = mountSidebar('/settings')
    const link = wrapper.find('a[href="/"]')
    expect(link.classes()).not.toContain('bg-green-50')
    expect(link.classes()).toContain('text-gray-600')
  })

  it('emits "navigate" event when the nav link is clicked', async () => {
    const wrapper = mountSidebar('/')
    // The @click="$emit('navigate')" on <router-link> binds to the stub root <a>
    // because the stub uses inheritAttrs: false but v-bind="$attrs" on <a>
    const link = wrapper.find('a[href="/"]')
    await link.trigger('click')
    expect(wrapper.emitted('navigate')).toBeTruthy()
    expect(wrapper.emitted('navigate').length).toBe(1)
  })
})
