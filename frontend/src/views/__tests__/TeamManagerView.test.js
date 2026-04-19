import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { ref } from 'vue'

// ── Module mocks ───────────────────────────────────────────────────────────────
const mockRouteParams = ref({})

vi.mock('vue-router', () => ({
  useRoute: vi.fn(() => ({
    get params() { return mockRouteParams.value },
  })),
  RouterView: { name: 'RouterView', template: '<div data-testid="router-view-outlet" />' },
}))

vi.mock('../../components/TeamListPanel.vue', () => ({
  default: {
    name: 'TeamListPanel',
    template: '<div data-testid="team-list-panel" />',
  },
}))

// ── Lazy import (after mocks) ──────────────────────────────────────────────────
let TeamManagerView

beforeEach(async () => {
  mockRouteParams.value = {}
  vi.resetModules()
  TeamManagerView = (await import('../TeamManagerView.vue')).default
})

function mountView() {
  return mount(TeamManagerView, {
    global: {
      stubs: { RouterView: { name: 'RouterView', template: '<div data-testid="router-view-outlet" />' } },
    },
  })
}

// ── Tests ──────────────────────────────────────────────────────────────────────
describe('TeamManagerView mobile responsive behavior', () => {
  it('shows team list panel wrapper and hides detail on mobile when no team selected', () => {
    mockRouteParams.value = {}
    const wrapper = mountView()

    const listWrapper = wrapper.find('[data-testid="team-list-panel-wrapper"]')
    const detailWrapper = wrapper.find('[data-testid="team-detail-wrapper"]')

    // No team selected: list visible (NOT hidden), detail hidden on mobile
    expect(listWrapper.classes()).not.toContain('hidden')
    expect(detailWrapper.classes()).toContain('hidden')
  })

  it('hides team list panel on mobile when a team is selected', () => {
    mockRouteParams.value = { id: 'team-1' }
    const wrapper = mountView()

    const listWrapper = wrapper.find('[data-testid="team-list-panel-wrapper"]')
    const detailWrapper = wrapper.find('[data-testid="team-detail-wrapper"]')

    // Team selected: list hidden on mobile, detail visible
    expect(listWrapper.classes()).toContain('hidden')
    expect(detailWrapper.classes()).not.toContain('hidden')
  })

  it('desktop always shows both panels: list wrapper contains lg:block', () => {
    mockRouteParams.value = {}
    const wrapper = mountView()

    const listWrapper = wrapper.find('[data-testid="team-list-panel-wrapper"]')
    expect(listWrapper.classes()).toContain('lg:block')
  })

  it('desktop always shows both panels: detail wrapper contains lg:block when no team selected', () => {
    mockRouteParams.value = {}
    const wrapper = mountView()

    const detailWrapper = wrapper.find('[data-testid="team-detail-wrapper"]')
    expect(detailWrapper.classes()).toContain('lg:block')
  })

  it('detail wrapper has lg:block when team is selected', () => {
    mockRouteParams.value = { id: 'team-1' }
    const wrapper = mountView()

    const detailWrapper = wrapper.find('[data-testid="team-detail-wrapper"]')
    expect(detailWrapper.classes()).toContain('lg:block')
  })

  it('list wrapper has lg:block when team is selected', () => {
    mockRouteParams.value = { id: 'team-1' }
    const wrapper = mountView()

    const listWrapper = wrapper.find('[data-testid="team-list-panel-wrapper"]')
    // Should have lg:block so it still appears on desktop even when hidden on mobile
    expect(listWrapper.classes()).toContain('lg:block')
  })
})
