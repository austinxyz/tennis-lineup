# Mobile UI Optimization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Adapt the tennis-lineup Vue 3 frontend to be mobile-friendly at `< lg` (1024px) — hamburger-toggled sidebar, single-column layouts, per-player lineup rows, and opponent analysis via dropdowns only. Desktop layout (`>= lg`) must remain pixel-perfect unchanged.

**Architecture:** Tailwind `lg` breakpoint as the mobile/desktop boundary. Shared `AppHeader.vue` component (mobile only, `lg:hidden`) handles hamburger + back button + page title. Global `sidebarOpen` state provided by `MainLayout.vue`, injected by `AppHeader`. URL-driven list/detail toggle in team management (`/` = list, `/teams/:id` = detail). `LineupCard.vue` refactored to show each player on their own row with gender tag + name + UTR + actual UTR. Opponent analysis replaces text input with 4 dropdowns + live preview.

**Tech Stack:** Vue 3 Composition API (`<script setup>`), Vue Router 4, Tailwind CSS, Vitest + @vue/test-utils + jsdom (tests), Playwright (E2E), `@vueuse/core` not required — use native `provide`/`inject`.

---

## Pre-flight

- [ ] **Step 0.1: Ensure working tree clean**

Run: `git status`
Expected: `nothing to commit, working tree clean`. If dirty, commit or stash first.

- [ ] **Step 0.2: Run baseline tests**

Run:
```bash
cd /c/Users/lorra/projects/tennis/frontend && npm test 2>&1 | tail -5
```
Expected: `Tests N passed (N)` with N around 365+. Record the number; it's the baseline.

---

## Task 1: AppHeader component (Mobile-only top bar)

**Files:**
- Create: `frontend/src/components/AppHeader.vue`
- Create: `frontend/src/components/__tests__/AppHeader.test.js`

**Why:** Unified mobile top bar shared by all pages. Contains hamburger button (toggles sidebar), optional back button, page title, and `actions` slot on the right. Hidden on desktop via `lg:hidden`.

- [ ] **Step 1.1: Write the failing test**

File: `frontend/src/components/__tests__/AppHeader.test.js`

```js
import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import AppHeader from '../AppHeader.vue'

// Mock vue-router's useRouter
const mockRouterPush = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mockRouterPush, back: vi.fn() }),
}))

function mountHeader(props = {}, slots = {}) {
  return mount(AppHeader, {
    props,
    slots,
    global: {
      provide: { sidebarOpen: { value: false } },
    },
  })
}

describe('AppHeader', () => {
  it('renders the page title', () => {
    const wrapper = mountHeader({ title: '队伍列表' })
    expect(wrapper.text()).toContain('队伍列表')
  })

  it('renders the hamburger button', () => {
    const wrapper = mountHeader({ title: 'X' })
    expect(wrapper.find('[data-testid="hamburger"]').exists()).toBe(true)
  })

  it('does not render back button when backTo is not set', () => {
    const wrapper = mountHeader({ title: 'X' })
    expect(wrapper.find('[data-testid="back-btn"]').exists()).toBe(false)
  })

  it('renders back button when backTo is set', () => {
    const wrapper = mountHeader({ title: 'X', backTo: '/' })
    expect(wrapper.find('[data-testid="back-btn"]').exists()).toBe(true)
  })

  it('clicking back button navigates to backTo', async () => {
    const wrapper = mountHeader({ title: 'X', backTo: '/' })
    await wrapper.find('[data-testid="back-btn"]').trigger('click')
    expect(mockRouterPush).toHaveBeenCalledWith('/')
  })

  it('hamburger click toggles injected sidebarOpen', async () => {
    const sidebarOpen = { value: false }
    const wrapper = mount(AppHeader, {
      props: { title: 'X' },
      global: { provide: { sidebarOpen } },
    })
    await wrapper.find('[data-testid="hamburger"]').trigger('click')
    expect(sidebarOpen.value).toBe(true)
  })

  it('renders actions slot on the right', () => {
    const wrapper = mountHeader({ title: 'X' }, {
      actions: '<button data-testid="custom-action">X</button>',
    })
    expect(wrapper.find('[data-testid="custom-action"]').exists()).toBe(true)
  })

  it('has lg:hidden class so it does not show on desktop', () => {
    const wrapper = mountHeader({ title: 'X' })
    expect(wrapper.find('header').classes()).toContain('lg:hidden')
  })
})
```

- [ ] **Step 1.2: Run test to verify it fails**

Run: `cd frontend && npx vitest run src/components/__tests__/AppHeader.test.js`
Expected: FAIL — `AppHeader.vue` doesn't exist.

- [ ] **Step 1.3: Write minimal implementation**

File: `frontend/src/components/AppHeader.vue`

```vue
<template>
  <header class="lg:hidden fixed top-0 left-0 right-0 z-50 bg-white border-b border-gray-200 h-14 flex items-center px-3 gap-2">
    <button
      data-testid="hamburger"
      @click="onHamburger"
      class="p-2 rounded-lg hover:bg-gray-100 text-gray-600"
      aria-label="打开导航"
    >
      <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16"/>
      </svg>
    </button>

    <button
      v-if="backTo"
      data-testid="back-btn"
      @click="onBack"
      class="text-sm text-blue-600 font-medium px-2 py-1 rounded hover:bg-blue-50"
    >← {{ backLabel }}</button>

    <span class="flex-1 font-semibold text-gray-900 truncate">{{ title }}</span>

    <div class="flex items-center gap-1">
      <slot name="actions" />
    </div>
  </header>
</template>

<script setup>
import { inject } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps({
  title: { type: String, required: true },
  backTo: { type: String, default: null },
  backLabel: { type: String, default: '返回' },
})

const sidebarOpen = inject('sidebarOpen', { value: false })
const router = useRouter()

function onHamburger() { sidebarOpen.value = !sidebarOpen.value }
function onBack() { if (props.backTo) router.push(props.backTo) }
</script>
```

- [ ] **Step 1.4: Run test to verify it passes**

Run: `cd frontend && npx vitest run src/components/__tests__/AppHeader.test.js`
Expected: PASS all 7 tests.

- [ ] **Step 1.5: Commit**

```bash
cd /c/Users/lorra/projects/tennis
git add frontend/src/components/AppHeader.vue frontend/src/components/__tests__/AppHeader.test.js
git commit -m "feat(mobile): add AppHeader component with hamburger + back button"
```

---

## Task 2: MainLayout refactor — provide sidebarOpen, delegate to AppHeader

**Files:**
- Modify: `frontend/src/layouts/MainLayout.vue`

**Why:** The existing `MainLayout.vue` has an inline mobile header. Replace with `AppHeader` and use `provide` to share `sidebarOpen` state. Individual pages will mount their own `AppHeader` (with correct title + backTo) inside their template.

**Important:** MainLayout's built-in top bar stays as a fallback default for pages that don't have their own AppHeader yet. But it will be phased out as pages are migrated. For simplicity in this task, we keep MainLayout's header generic (no title/back) and let pages add their own AppHeader. Actually — cleaner approach: **remove MainLayout's header entirely**, require each top-level page view to render its own AppHeader.

- [ ] **Step 2.1: Read current MainLayout to confirm structure**

Run: `cat frontend/src/layouts/MainLayout.vue`
Expected: Current file has inline header + overlay + aside + router-view.

- [ ] **Step 2.2: Rewrite MainLayout to remove inline header, provide sidebarOpen**

File: `frontend/src/layouts/MainLayout.vue`

```vue
<template>
  <div class="flex min-h-screen bg-gray-100">

    <!-- Mobile overlay (only when drawer open) -->
    <div
      v-if="sidebarOpen"
      @click="sidebarOpen = false"
      class="lg:hidden fixed inset-0 bg-black/40 z-40"
    />

    <!-- Nav Sidebar (drawer on mobile, fixed on desktop) -->
    <aside :class="[
      'fixed top-0 bottom-0 z-50 transition-transform duration-300 lg:static lg:translate-x-0 lg:z-auto',
      sidebarOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'
    ]">
      <NavSidebar @navigate="sidebarOpen = false"/>
    </aside>

    <!-- Main content -->
    <div class="flex-1 flex flex-col min-w-0">
      <router-view/>
    </div>
  </div>
</template>

<script setup>
import { ref, provide } from 'vue'
import NavSidebar from '../components/NavSidebar.vue'

const sidebarOpen = ref(false)
provide('sidebarOpen', sidebarOpen)
</script>
```

- [ ] **Step 2.3: Run all existing tests — expect some failures**

Run: `cd frontend && npm test 2>&1 | tail -10`
Expected: Existing tests that relied on MainLayout's inline header (if any) will fail. Record what fails.

- [ ] **Step 2.4: Fix any failing tests**

If `MainLayout.test.js` exists and has header-related assertions, update them:
- Remove assertions about header rendering inside MainLayout
- Add assertion that `sidebarOpen` is `false` by default (via provide)

Most page tests don't mount MainLayout, so they should be fine.

- [ ] **Step 2.5: Run tests to verify green**

Run: `cd frontend && npm test 2>&1 | tail -5`
Expected: All tests pass (same count as baseline).

- [ ] **Step 2.6: Commit**

```bash
git add frontend/src/layouts/MainLayout.vue
git commit -m "refactor(mobile): MainLayout provides sidebarOpen, removes inline header"
```

---

## Task 3: TeamManagerView — mobile single-view switch

**Files:**
- Modify: `frontend/src/views/TeamManagerView.vue`
- Modify (or verify): `frontend/src/views/__tests__/TeamManagerView.test.js`

**Why:** Current `TeamManagerView.vue` renders `TeamListPanel` + nested `<router-view/>` side-by-side. On mobile, show only one at a time based on `route.params.id`.

- [ ] **Step 3.1: Read current TeamManagerView**

Run: `cat frontend/src/views/TeamManagerView.vue`
Note the current template structure.

- [ ] **Step 3.2: Write the failing test (or extend existing)**

File: `frontend/src/views/__tests__/TeamManagerView.test.js` — add tests:

```js
// Add these tests to the existing describe block
import { ref } from 'vue'

const mockRoute = ref({ params: {}, path: '/' })
vi.mock('vue-router', async () => {
  const actual = await vi.importActual('vue-router')
  return {
    ...actual,
    useRoute: () => mockRoute.value,
  }
})

describe('TeamManagerView mobile behavior', () => {
  it('shows team list panel when no team selected', async () => {
    mockRoute.value = { params: {}, path: '/' }
    const wrapper = mount(TeamManagerView, { global: { stubs: { RouterLink: true, RouterView: true } } })
    await flushPromises()
    expect(wrapper.find('[data-testid="team-list-panel"]').exists()).toBe(true)
  })

  it('hides team list on mobile when team is selected', async () => {
    mockRoute.value = { params: { id: 'team-1' }, path: '/teams/team-1' }
    const wrapper = mount(TeamManagerView, { global: { stubs: { RouterLink: true, RouterView: true } } })
    await flushPromises()
    // On mobile, list should have lg:block (hidden on mobile)
    const panel = wrapper.find('[data-testid="team-list-panel"]')
    expect(panel.classes()).toContain('hidden')
    expect(panel.classes()).toContain('lg:block')
  })
})
```

- [ ] **Step 3.3: Run test to verify it fails**

Run: `cd frontend && npx vitest run src/views/__tests__/TeamManagerView.test.js`
Expected: FAIL — missing `data-testid` or wrong class.

- [ ] **Step 3.4: Modify TeamManagerView template**

File: `frontend/src/views/TeamManagerView.vue`

Update template (preserve existing script):

```vue
<template>
  <div class="flex flex-1 min-h-0">
    <!-- Team list panel:
         - Always visible on desktop (lg:block)
         - On mobile: visible only when no team selected -->
    <div
      data-testid="team-list-panel"
      :class="[
        'lg:block',
        teamSelected ? 'hidden' : 'block w-full lg:w-auto'
      ]"
    >
      <TeamListPanel />
    </div>

    <!-- Detail area:
         - Always visible on desktop
         - On mobile: visible only when a team is selected -->
    <div
      :class="[
        'flex-1 min-w-0',
        teamSelected ? 'block' : 'hidden lg:block'
      ]"
    >
      <router-view />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import TeamListPanel from '../components/TeamListPanel.vue'

const route = useRoute()
const teamSelected = computed(() => Boolean(route.params.id))
</script>
```

- [ ] **Step 3.5: Run tests to verify they pass**

Run: `cd frontend && npx vitest run src/views/__tests__/TeamManagerView.test.js`
Expected: PASS.

- [ ] **Step 3.6: Run full test suite**

Run: `cd frontend && npm test 2>&1 | tail -5`
Expected: All tests pass.

- [ ] **Step 3.7: Commit**

```bash
git add frontend/src/views/TeamManagerView.vue frontend/src/views/__tests__/TeamManagerView.test.js
git commit -m "feat(mobile): TeamManagerView shows list XOR detail on mobile"
```

---

## Task 4: TeamDetail — add AppHeader + Mobile player cards

**Files:**
- Modify: `frontend/src/views/TeamDetail.vue`
- Modify: `frontend/src/views/__tests__/TeamDetail.test.js`

**Why:** (1) Show `AppHeader` on mobile with back button to `/`. (2) Add mobile player card view (`lg:hidden`) with collapsible rows showing name/gender/UTR/actualUtr. Desktop table (`hidden lg:block`) unchanged.

- [ ] **Step 4.1: Read current TeamDetail to find player list section**

Run: `grep -n "players\|player-row\|球员" frontend/src/views/TeamDetail.vue | head -20`
Locate the player table block to wrap with `hidden lg:block`.

- [ ] **Step 4.2: Write failing test for mobile player card**

Add to `frontend/src/views/__tests__/TeamDetail.test.js`:

```js
describe('TeamDetail mobile player cards', () => {
  it('renders mobile player card for each player', async () => {
    // Use existing mock setup; ensure a player exists in mocked team state
    // (Adjust based on existing test fixture patterns in this file)
    const wrapper = mountTeamDetail({
      players: [
        { id: 'p1', name: 'Alice', gender: 'female', utr: 6.0, verified: true, actualUtr: 6.8 },
      ],
    })
    await flushPromises()
    const cards = wrapper.findAll('[data-testid="player-card-mobile"]')
    expect(cards.length).toBeGreaterThan(0)
    expect(cards[0].text()).toContain('Alice')
    expect(cards[0].text()).toContain('6.0')
  })

  it('clicking a mobile player card expands details', async () => {
    const wrapper = mountTeamDetail({
      players: [{ id: 'p1', name: 'Alice', gender: 'female', utr: 6.0, verified: true, actualUtr: 6.8, notes: 'strong' }],
    })
    await flushPromises()
    const card = wrapper.find('[data-testid="player-card-mobile"]')
    await card.trigger('click')
    const detail = wrapper.find('[data-testid="player-card-detail"]')
    expect(detail.exists()).toBe(true)
    expect(detail.text()).toContain('strong')
  })

  it('shows female gender tag in pink', async () => {
    const wrapper = mountTeamDetail({
      players: [{ id: 'p1', name: 'Alice', gender: 'female', utr: 6.0, verified: true }],
    })
    await flushPromises()
    const tag = wrapper.find('[data-testid="player-card-mobile"] [data-testid="gender-tag"]')
    expect(tag.classes()).toContain('bg-pink-100')
    expect(tag.text()).toBe('女')
  })
})
```

**Note:** `mountTeamDetail` helper signature depends on the existing test file. Adapt to match existing fixture pattern. If unclear, inline a fresh `mount(TeamDetail, ...)` call with full mocks.

- [ ] **Step 4.3: Run test to verify it fails**

Run: `cd frontend && npx vitest run src/views/__tests__/TeamDetail.test.js`
Expected: FAIL — no `player-card-mobile` element yet.

- [ ] **Step 4.4: Add mobile card view + AppHeader in TeamDetail**

In `frontend/src/views/TeamDetail.vue`:

1. Import `AppHeader`: `import AppHeader from '../components/AppHeader.vue'`
2. At top of template, add:

```vue
<AppHeader :title="team?.name || '队伍详情'" back-to="/" back-label="队伍">
  <template #actions>
    <!-- optional: add player button or menu -->
  </template>
</AppHeader>
```

3. Wrap existing desktop player table with `<div class="hidden lg:block">...</div>`

4. Add mobile card list after the wrapped desktop table:

```vue
<!-- Mobile player cards -->
<div class="lg:hidden px-4 py-3 space-y-2">
  <div
    v-for="player in players"
    :key="player.id"
    data-testid="player-card-mobile"
    @click="toggleExpand(player.id)"
    class="bg-white border border-gray-200 rounded-lg p-3 cursor-pointer transition-colors"
    :class="{ 'bg-green-50 border-green-200': expandedPlayerId === player.id }"
  >
    <div class="flex items-center gap-2">
      <span
        data-testid="gender-tag"
        class="text-xs font-bold px-1.5 py-0.5 rounded"
        :class="player.gender === 'female' ? 'bg-pink-100 text-pink-700' : 'bg-blue-100 text-blue-700'"
      >{{ player.gender === 'female' ? '女' : '男' }}</span>
      <span class="font-semibold flex-1 text-gray-900">{{ player.name }}</span>
      <div class="text-right text-xs">
        <div class="text-gray-600">
          UTR {{ player.utr }}
          <span v-if="player.verified" class="inline-block w-1.5 h-1.5 rounded-full bg-emerald-500 ml-0.5" title="Verified"></span>
        </div>
        <div
          v-if="player.actualUtr != null && player.actualUtr !== player.utr"
          class="text-amber-500 font-semibold"
        >实:{{ player.actualUtr.toFixed(2) }}</div>
      </div>
      <span class="text-gray-400 text-xs ml-1">{{ expandedPlayerId === player.id ? '▾' : '▸' }}</span>
    </div>

    <div v-if="expandedPlayerId === player.id" data-testid="player-card-detail" class="mt-2 pt-2 border-t border-green-200 text-xs text-gray-600 space-y-1" @click.stop>
      <div v-if="player.verifiedDoublesUtr != null"><span class="text-gray-400">Verified Doubles:</span> {{ player.verifiedDoublesUtr.toFixed(2) }} ✓</div>
      <div><span class="text-gray-400">性别:</span> {{ player.gender === 'female' ? '女' : '男' }}</div>
      <div v-if="player.notes" class="bg-white p-2 rounded">📝 {{ player.notes }}</div>
      <div class="flex gap-2 mt-2">
        <button @click="handleEdit(player)" class="px-2 py-1 text-xs border border-gray-300 rounded hover:bg-gray-100">编辑</button>
        <button @click="handleDelete(player.id)" class="px-2 py-1 text-xs border border-red-200 rounded text-red-600 hover:bg-red-50">删除</button>
      </div>
    </div>
  </div>
</div>
```

5. In the `<script setup>` section, add:

```js
const expandedPlayerId = ref(null)
function toggleExpand(id) {
  expandedPlayerId.value = expandedPlayerId.value === id ? null : id
}
```

(If `handleEdit`/`handleDelete` don't exist, reuse existing edit/delete handlers from the desktop table.)

- [ ] **Step 4.5: Run tests to verify green**

Run: `cd frontend && npx vitest run src/views/__tests__/TeamDetail.test.js`
Expected: PASS.

- [ ] **Step 4.6: Run full suite**

Run: `cd frontend && npm test 2>&1 | tail -5`

- [ ] **Step 4.7: Commit**

```bash
git add frontend/src/views/TeamDetail.vue frontend/src/views/__tests__/TeamDetail.test.js
git commit -m "feat(mobile): TeamDetail AppHeader + collapsible player cards"
```

---

## Task 5: LineupCard — refactor player row to 2-line layout with gender/UTR/actualUtr

**Files:**
- Modify: `frontend/src/components/LineupCard.vue`
- Modify: `frontend/src/components/__tests__/LineupCard.test.js`

**Why:** `LineupCard` is used by `LineupHistoryView`, `LineupGenerator`, and `LineupMatchup`. Standardize each pair to 2 rows (player1 top / player2 bottom) with `[gender-tag] name UTR 实:actualUtr`.

- [ ] **Step 5.1: Read current LineupCard pair rendering**

Run: `cat frontend/src/components/LineupCard.vue`
Note: current renders pair as single line "player1 / player2".

- [ ] **Step 5.2: Write failing test**

Add to `frontend/src/components/__tests__/LineupCard.test.js`:

```js
describe('LineupCard player row layout', () => {
  it('renders each player on its own row with gender tag', () => {
    const lineup = {
      pairs: [{
        position: 'D1',
        player1Name: 'Alice', player1Gender: 'female', player1Utr: 6.0, player1ActualUtr: 6.8,
        player2Name: 'Bob', player2Gender: 'male', player2Utr: 5.5, player2ActualUtr: null,
        combinedUtr: 11.5,
      }],
      totalUtr: 11.5,
      strategy: 'balanced', aiUsed: false,
    }
    const wrapper = mount(LineupCard, { props: { lineup } })
    const rows = wrapper.findAll('[data-testid="pair-player-row"]')
    expect(rows).toHaveLength(2)
    expect(rows[0].text()).toContain('Alice')
    expect(rows[0].text()).toContain('女')
    expect(rows[1].text()).toContain('Bob')
    expect(rows[1].text()).toContain('男')
  })

  it('shows actual UTR only when different from utr', () => {
    const lineup = {
      pairs: [{
        position: 'D1',
        player1Name: 'Alice', player1Gender: 'female', player1Utr: 6.0, player1ActualUtr: 6.8,
        player2Name: 'Bob', player2Gender: 'male', player2Utr: 5.5, player2ActualUtr: 5.5,
        combinedUtr: 11.5,
      }],
      totalUtr: 11.5, strategy: 'balanced', aiUsed: false,
    }
    const wrapper = mount(LineupCard, { props: { lineup } })
    const rows = wrapper.findAll('[data-testid="pair-player-row"]')
    expect(rows[0].text()).toContain('6.80') // actual UTR for Alice
    expect(rows[1].text()).not.toContain('实:') // no actual for Bob (equal to utr)
  })
})
```

- [ ] **Step 5.3: Run test to verify it fails**

Run: `cd frontend && npx vitest run src/components/__tests__/LineupCard.test.js`

- [ ] **Step 5.4: Refactor LineupCard pair rendering**

In `frontend/src/components/LineupCard.vue`, replace the pair row's inline player display with 2 rows:

Find the block like:
```vue
<div v-for="pair in sortedPairs" :key="pair.position" class="px-5 py-3 flex items-center gap-4">
  ...
  <div class="flex-1 text-sm text-gray-800">
    <template v-if="showPlayerUtr">
      ...
    </template>
  </div>
  ...
</div>
```

Replace the player display part with:

```vue
<div v-for="pair in sortedPairs" :key="pair.position" class="px-4 py-2.5 grid grid-cols-[36px_1fr_auto] gap-3 items-start border-t border-gray-50 first:border-t-0">
  <span class="text-xs font-bold text-green-600 pt-1">{{ pair.position }}</span>
  <div class="space-y-1 min-w-0">
    <div
      v-for="slot in [1, 2]"
      :key="slot"
      data-testid="pair-player-row"
      class="flex items-center gap-2 text-sm"
    >
      <span
        v-if="pair[`player${slot}Gender`]"
        class="text-xs font-bold px-1.5 py-0.5 rounded flex-shrink-0"
        :class="pair[`player${slot}Gender`] === 'female' ? 'bg-pink-100 text-pink-700' : 'bg-blue-100 text-blue-700'"
      >{{ pair[`player${slot}Gender`] === 'female' ? '女' : '男' }}</span>
      <span class="font-semibold text-gray-900 flex-1 truncate">{{ pair[`player${slot}Name`] }}</span>
      <span class="text-xs text-gray-500">{{ pair[`player${slot}Utr`] ?? '—' }}</span>
      <span
        v-if="pair[`player${slot}ActualUtr`] != null && pair[`player${slot}ActualUtr`] !== pair[`player${slot}Utr`]"
        class="text-xs text-amber-500 font-semibold"
      >实:{{ pair[`player${slot}ActualUtr`].toFixed(2) }}</span>
    </div>
  </div>
  <div class="text-right text-xs text-gray-500 pt-1 whitespace-nowrap">
    <div>{{ pair.combinedUtr.toFixed(2) }}</div>
  </div>
</div>
```

- [ ] **Step 5.5: Run LineupCard tests**

Run: `cd frontend && npx vitest run src/components/__tests__/LineupCard.test.js`
Expected: PASS all (existing tests may need minor adjustments if they check old single-line format — update them to match new 2-row format).

- [ ] **Step 5.6: Run full suite (LineupCard used by many pages)**

Run: `cd frontend && npm test 2>&1 | tail -5`
Expected: All pass. If any fail, check tests for `LineupHistoryView`, `LineupResultGrid` — update assertions to match new DOM structure.

- [ ] **Step 5.7: Commit**

```bash
git add frontend/src/components/LineupCard.vue frontend/src/components/__tests__/LineupCard.test.js
git commit -m "refactor(lineup-card): each player on its own row with gender/UTR/actualUtr"
```

---

## Task 6: LineupHistoryView — add AppHeader, verify single-column mobile

**Files:**
- Modify: `frontend/src/views/LineupHistoryView.vue`

**Why:** Add `AppHeader` with back button to `/teams/:id`. Existing `grid-cols-1 lg:grid-cols-2` already provides mobile single-column.

- [ ] **Step 6.1: Read current LineupHistoryView structure**

Run: `grep -n "template\|AppHeader\|已保存排阵" frontend/src/views/LineupHistoryView.vue | head -10`

- [ ] **Step 6.2: Add AppHeader at top of template**

In `frontend/src/views/LineupHistoryView.vue` template, at the very top replace:

```vue
<template>
  <div class="p-6">
    <!-- ... existing content ... -->
```

With:

```vue
<template>
  <div class="flex flex-col min-h-full">
    <AppHeader :title="`已保存排阵${currentTeam?.name ? ' · ' + currentTeam.name : ''}`" :back-to="`/teams/${teamId}`" back-label="队伍">
      <template #actions>
        <button @click="handleExport" class="text-xs text-green-700 px-2 py-1 rounded border border-green-300">导出</button>
        <button @click="$refs.importInput.click()" :disabled="importing" class="text-xs text-blue-600 px-2 py-1 rounded border border-blue-300 disabled:opacity-50">{{ importing ? '...' : '导入' }}</button>
        <input ref="importInput" type="file" accept=".json" class="hidden" @change="handleImport" />
      </template>
    </AppHeader>

    <div class="flex-1 p-4 lg:p-6 pt-16 lg:pt-6">
      <!-- Desktop also sees a header here (since AppHeader is lg:hidden) -->
      <div class="hidden lg:flex items-center justify-between mb-6">
        <h2 class="text-2xl font-bold text-gray-900">已保存排阵</h2>
        <div class="flex items-center gap-2">
          <button @click="handleExport" class="px-3 py-1.5 text-xs rounded-lg border border-green-300 text-green-700 hover:bg-green-50 transition-colors">导出排阵</button>
          <button @click="$refs.importInput2.click()" :disabled="importing" class="px-3 py-1.5 text-xs rounded-lg border border-blue-300 text-blue-600 hover:bg-blue-50 transition-colors disabled:opacity-50">{{ importing ? '导入中...' : '导入排阵' }}</button>
          <input ref="importInput2" type="file" accept=".json" class="hidden" @change="handleImport" />
        </div>
      </div>

      <!-- ... rest of existing content ... -->
```

(The desktop header inside `hidden lg:flex` duplicates the button set; if preferred, unify by putting both refs on the same input — use separate refs to avoid `$refs` collision.)

Add import: `import AppHeader from '../components/AppHeader.vue'`

Also add `pt-14 lg:pt-0` on the outer wrapper so content doesn't slide under the fixed AppHeader.

- [ ] **Step 6.3: Run tests**

Run: `cd frontend && npx vitest run src/views/__tests__/LineupHistoryView.test.js`
Expected: PASS (existing tests). If they mount with a full wrapper including AppHeader's injection, they may need `global.provide.sidebarOpen` and `global.mocks.$route`.

- [ ] **Step 6.4: Fix test mounts if needed**

If AppHeader fails to mount in tests due to missing inject, add to existing `mountView()` helper:

```js
function mountView() {
  return mount(LineupHistoryView, {
    global: {
      stubs: { RouterLink: true },
      provide: { sidebarOpen: ref(false) },
    },
  })
}
```

Or stub AppHeader itself:

```js
vi.mock('../../components/AppHeader.vue', () => ({
  default: { name: 'AppHeader', props: ['title', 'backTo', 'backLabel'], template: '<header><slot name="actions"/></header>' }
}))
```

- [ ] **Step 6.5: Run full suite**

Run: `cd frontend && npm test 2>&1 | tail -5`

- [ ] **Step 6.6: Commit**

```bash
git add frontend/src/views/LineupHistoryView.vue frontend/src/views/__tests__/LineupHistoryView.test.js
git commit -m "feat(mobile): LineupHistoryView adds AppHeader with export/import actions"
```

---

## Task 7: LineupGenerator — mobile form + AppHeader

**Files:**
- Modify: `frontend/src/views/LineupGenerator.vue`
- Modify: `frontend/src/views/__tests__/LineupGenerator.test.js`

**Why:** Stack form vertically on mobile. Collapse advanced options (pin/include/exclude) into `<details>`. Keep desktop layout untouched.

- [ ] **Step 7.1: Read current LineupGenerator structure**

Run: `wc -l frontend/src/views/LineupGenerator.vue`
Likely long — skim with `head -100` and `grep -n "固定位置\|包含球员\|排除"`.

- [ ] **Step 7.2: Add AppHeader at top**

In the template, prepend:

```vue
<AppHeader title="排阵生成">
  <template #actions>
    <button v-if="hasResults" @click="regenerate" class="text-xs text-gray-700 px-2 py-1 rounded border border-gray-300">重新</button>
  </template>
</AppHeader>
<div class="pt-14 lg:pt-0">
  <!-- existing content -->
</div>
```

Import `AppHeader` in script.

- [ ] **Step 7.3: Wrap advanced sections in `<details>` for mobile only**

Find the sections for pin players, include players, exclude players. Wrap each with:

```vue
<details class="lg:hidden bg-white border border-gray-200 rounded-lg mb-2">
  <summary class="px-4 py-3 cursor-pointer font-semibold text-sm flex items-center gap-2">
    <span>📌 固定位置</span>
    <span v-if="pinCount > 0" class="bg-indigo-100 text-indigo-700 text-xs px-1.5 py-0.5 rounded-full">{{ pinCount }}</span>
  </summary>
  <div class="px-4 pb-3">
    <!-- existing pin players UI -->
  </div>
</details>

<!-- Desktop retains original -->
<div class="hidden lg:block">
  <!-- original desktop layout -->
</div>
```

Apply same pattern for include and exclude sections. Compute `pinCount`, `includeCount`, `excludeCount` from existing state.

- [ ] **Step 7.4: Run tests and fix selector mismatches**

Run: `cd frontend && npx vitest run src/views/__tests__/LineupGenerator.test.js`
Tests that select elements by class may need adjustment due to new `lg:hidden` / `hidden lg:block` duplication. Update selectors to use `data-testid` instead of class if needed.

- [ ] **Step 7.5: Commit**

```bash
git add frontend/src/views/LineupGenerator.vue frontend/src/views/__tests__/LineupGenerator.test.js
git commit -m "feat(mobile): LineupGenerator AppHeader + collapsible advanced options"
```

---

## Task 8: OpponentAnalysis — full rewrite to dropdown-only flow

**Files:**
- Modify: `frontend/src/views/OpponentAnalysis.vue`
- Modify: `frontend/src/views/__tests__/OpponentAnalysis.test.js`

**Why:** Remove text input for opponent. Add 4 dropdowns: my team, my lineup, opponent team, opponent lineup. Show live previews. Result page with per-line matchup cards + risk badges + AI commentary.

- [ ] **Step 8.1: Read current OpponentAnalysis**

Run: `head -80 frontend/src/views/OpponentAnalysis.vue`
Understand existing state shape (my team, my lineup, opponent text, analysis result).

- [ ] **Step 8.2: Write failing tests**

Add/replace in `frontend/src/views/__tests__/OpponentAnalysis.test.js`:

```js
describe('OpponentAnalysis dropdown flow', () => {
  it('renders 4 select dropdowns: my team, my lineup, opponent team, opponent lineup', async () => {
    const wrapper = mountAnalysis()
    await flushPromises()
    expect(wrapper.find('[data-testid="select-my-team"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="select-my-lineup"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="select-opp-team"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="select-opp-lineup"]').exists()).toBe(true)
  })

  it('opponent lineup dropdown is disabled when opponent team has no lineups', async () => {
    // Mock opp team with empty lineups
    // ...
    const wrapper = mountAnalysis()
    await flushPromises()
    const sel = wrapper.find('[data-testid="select-opp-lineup"]')
    expect(sel.attributes('disabled')).toBeDefined()
    expect(wrapper.text()).toContain('该队伍暂无排阵')
  })

  it('analyze button is disabled until all 4 selections made', async () => {
    const wrapper = mountAnalysis()
    await flushPromises()
    expect(wrapper.find('[data-testid="analyze-btn"]').attributes('disabled')).toBeDefined()
  })

  it('shows my and opponent lineup previews once selected', async () => {
    // Select both lineups via v-model updates
    // ...
    expect(wrapper.find('[data-testid="my-preview"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="opp-preview"]').exists()).toBe(true)
  })
})
```

Provide any required mocks for `useTeams`, `useLineupHistory`, and the analysis endpoint.

- [ ] **Step 8.3: Rewrite the template**

Structure:

```vue
<template>
  <div class="flex flex-col min-h-full">
    <AppHeader title="对手分析" />
    <div class="flex-1 pt-14 lg:pt-0 p-4 lg:p-6 max-w-2xl mx-auto w-full">

      <!-- Our side -->
      <div class="text-center text-xs text-gray-500 font-semibold tracking-widest my-2">· 我 方 ·</div>

      <label class="block text-xs text-gray-500 font-semibold mb-1 mt-2 uppercase">队伍</label>
      <select data-testid="select-my-team" v-model="myTeamId" class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm">
        <option value="" disabled>选择队伍</option>
        <option v-for="t in teams" :key="t.id" :value="t.id">{{ t.name }} ({{ (t.players || []).length }} 名)</option>
      </select>

      <label class="block text-xs text-gray-500 font-semibold mb-1 mt-3 uppercase">排阵</label>
      <select data-testid="select-my-lineup" v-model="myLineupId" :disabled="!myTeamId || myLineups.length === 0" class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm disabled:bg-gray-100">
        <option value="" disabled>{{ !myTeamId ? '请先选队伍' : (myLineups.length ? '选择排阵' : '该队伍暂无排阵') }}</option>
        <option v-for="l in myLineups" :key="l.id" :value="l.id">{{ l.label || l.strategy }} (总{{ (l.totalUtr || 0).toFixed(2) }})</option>
      </select>

      <LineupPreviewCard v-if="myLineup" data-testid="my-preview" :lineup="myLineup" variant="ours" />

      <!-- Opponent side -->
      <div class="text-center text-xs text-gray-500 font-semibold tracking-widest my-4">· 对 手 ·</div>

      <label class="block text-xs text-gray-500 font-semibold mb-1 uppercase">队伍</label>
      <select data-testid="select-opp-team" v-model="oppTeamId" class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm">
        <option value="" disabled>选择对手队伍</option>
        <option v-for="t in teams" :key="t.id" :value="t.id" :disabled="t.id === myTeamId">{{ t.name }}</option>
      </select>

      <label class="block text-xs text-gray-500 font-semibold mb-1 mt-3 uppercase">对手排阵</label>
      <select data-testid="select-opp-lineup" v-model="oppLineupId" :disabled="!oppTeamId || oppLineups.length === 0" class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm disabled:bg-gray-100">
        <option value="" disabled>{{ !oppTeamId ? '请先选对手' : (oppLineups.length ? '选择排阵' : '该队伍暂无排阵') }}</option>
        <option v-for="l in oppLineups" :key="l.id" :value="l.id">{{ l.label || l.strategy }} (总{{ (l.totalUtr || 0).toFixed(2) }})</option>
      </select>

      <LineupPreviewCard v-if="oppLineup" data-testid="opp-preview" :lineup="oppLineup" variant="opponent" />

      <button
        data-testid="analyze-btn"
        @click="analyze"
        :disabled="!canAnalyze || analyzing"
        class="w-full mt-4 py-3 bg-blue-600 text-white rounded-lg font-semibold disabled:bg-gray-300 disabled:cursor-not-allowed"
      >{{ analyzing ? '分析中...' : '🔍 开始分析' }}</button>

      <OpponentAnalysisResult v-if="result" :result="result" class="mt-6" />
    </div>
  </div>
</template>
```

- [ ] **Step 8.4: Create `LineupPreviewCard.vue` (new component)**

File: `frontend/src/components/LineupPreviewCard.vue`

```vue
<template>
  <div :class="[
    'border rounded-lg p-3 mt-2 text-xs space-y-1',
    variant === 'opponent' ? 'bg-red-50 border-red-200' : 'bg-white border-gray-200',
  ]">
    <div class="text-xs font-semibold uppercase mb-1" :class="variant === 'opponent' ? 'text-red-700' : 'text-gray-500'">
      {{ variant === 'opponent' ? '对手预览' : '我方预览' }}
    </div>
    <div v-for="pair in sortedPairs" :key="pair.position" class="flex items-center gap-2 py-1">
      <span class="w-7 text-xs font-bold text-green-600">{{ pair.position }}</span>
      <div class="flex-1 flex flex-wrap gap-1 items-center">
        <span v-if="pair.player1Gender" class="text-xs font-bold px-1 rounded" :class="pair.player1Gender === 'female' ? 'bg-pink-100 text-pink-700' : 'bg-blue-100 text-blue-700'">{{ pair.player1Gender === 'female' ? '女' : '男' }}</span>
        <span class="font-medium">{{ pair.player1Name }}</span>
        <span class="text-gray-400">/</span>
        <span v-if="pair.player2Gender" class="text-xs font-bold px-1 rounded" :class="pair.player2Gender === 'female' ? 'bg-pink-100 text-pink-700' : 'bg-blue-100 text-blue-700'">{{ pair.player2Gender === 'female' ? '女' : '男' }}</span>
        <span class="font-medium">{{ pair.player2Name }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
const props = defineProps({
  lineup: { type: Object, required: true },
  variant: { type: String, default: 'ours' }, // 'ours' | 'opponent'
})
const order = ['D1','D2','D3','D4']
const sortedPairs = computed(() => [...(props.lineup.pairs || [])].sort((a,b) => order.indexOf(a.position) - order.indexOf(b.position)))
</script>
```

- [ ] **Step 8.5: Create `OpponentAnalysisResult.vue` (new component)**

File: `frontend/src/components/OpponentAnalysisResult.vue`

```vue
<template>
  <div>
    <!-- Overall summary -->
    <div class="bg-gradient-to-br from-emerald-50 to-emerald-100 border border-emerald-200 rounded-xl p-4 text-center mb-4">
      <div class="text-3xl font-extrabold text-emerald-700">{{ overallPercent }}%</div>
      <div class="text-sm text-emerald-600 mt-1">整体预测胜率</div>
      <div class="text-xs text-emerald-700 mt-2">
        <strong>我方 {{ winCount }} 胜 {{ loseCount }} 负</strong>
        <span v-if="riskCount > 0"> · {{ riskCount }} 条风险线</span>
      </div>
    </div>

    <!-- Per-line cards -->
    <div
      v-for="line in result.lines"
      :key="line.position"
      :class="[
        'bg-white border rounded-lg p-3 mb-2',
        line.winProb < 0.5 ? 'border-amber-400' : 'border-gray-200',
      ]"
    >
      <div class="flex justify-between items-center mb-2">
        <span class="font-bold" :class="line.winProb < 0.5 ? 'text-amber-700' : 'text-emerald-600'">
          {{ line.position }}{{ line.winProb < 0.5 ? ' ⚠️' : '' }}
        </span>
        <span class="text-lg font-bold" :class="rateColor(line.winProb)">{{ Math.round(line.winProb * 100) }}%</span>
      </div>
      <div class="text-xs text-gray-600 mb-2">{{ line.matchup }}</div>
      <div class="bg-gray-100 rounded-full h-3 overflow-hidden">
        <div
          :class="[
            'h-full rounded-l-full',
            line.winProb >= 0.6 ? 'bg-gradient-to-r from-emerald-400 to-emerald-600' :
            line.winProb >= 0.5 ? 'bg-gradient-to-r from-amber-400 to-amber-600' :
            'bg-gradient-to-r from-red-400 to-red-600',
          ]"
          :style="{ width: `${line.winProb * 100}%` }"
        />
      </div>
      <div v-if="line.winProb < 0.5 && line.note" class="mt-2 p-2 bg-amber-50 border border-amber-200 rounded text-xs text-amber-800">
        <strong>⚠️ 风险提示：</strong>{{ line.note }}
      </div>
    </div>

    <!-- AI commentary -->
    <div v-if="result.commentary" class="bg-blue-50 border border-blue-200 rounded-lg p-3 text-sm text-blue-900 leading-relaxed mt-3">
      <strong>💡 AI 综合点评</strong><br/>
      {{ result.commentary }}
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
const props = defineProps({ result: { type: Object, required: true } })
const winCount = computed(() => (props.result.lines || []).filter(l => l.winProb >= 0.5).length)
const loseCount = computed(() => (props.result.lines || []).filter(l => l.winProb < 0.5).length)
const riskCount = computed(() => (props.result.lines || []).filter(l => l.winProb < 0.5).length)
const overallPercent = computed(() => {
  const lines = props.result.lines || []
  if (!lines.length) return 0
  const avg = lines.reduce((s, l) => s + l.winProb, 0) / lines.length
  return Math.round(avg * 100)
})
function rateColor(p) {
  if (p >= 0.6) return 'text-emerald-600'
  if (p >= 0.5) return 'text-amber-600'
  return 'text-red-600'
}
</script>
```

- [ ] **Step 8.6: Wire up OpponentAnalysis.vue script**

Replace existing script with dropdown-based logic:

```js
<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import AppHeader from '../components/AppHeader.vue'
import LineupPreviewCard from '../components/LineupPreviewCard.vue'
import OpponentAnalysisResult from '../components/OpponentAnalysisResult.vue'
import { useTeams } from '../composables/useTeams'
import { useApi } from '../composables/useApi'

const { teams, fetchTeams } = useTeams()
const { post } = useApi()

const myTeamId = ref('')
const myLineupId = ref('')
const oppTeamId = ref('')
const oppLineupId = ref('')
const myLineups = ref([])
const oppLineups = ref([])
const analyzing = ref(false)
const result = ref(null)

onMounted(() => fetchTeams())

async function loadLineups(teamId) {
  if (!teamId) return []
  return await (await fetch(`/api/teams/${teamId}/lineups`)).json()
}

watch(myTeamId, async (id) => {
  myLineupId.value = ''
  myLineups.value = id ? await loadLineups(id) : []
})
watch(oppTeamId, async (id) => {
  oppLineupId.value = ''
  oppLineups.value = id ? await loadLineups(id) : []
})

const myLineup = computed(() => myLineups.value.find(l => l.id === myLineupId.value))
const oppLineup = computed(() => oppLineups.value.find(l => l.id === oppLineupId.value))

const canAnalyze = computed(() => myLineup.value && oppLineup.value)

async function analyze() {
  if (!canAnalyze.value) return
  analyzing.value = true
  try {
    result.value = await post('/api/lineups/matchup', {
      myTeamId: myTeamId.value,
      myLineupId: myLineupId.value,
      oppTeamId: oppTeamId.value,
      oppLineupId: oppLineupId.value,
    })
  } finally {
    analyzing.value = false
  }
}
</script>
```

**Important:** Verify the actual backend endpoint for matchup analysis. Existing routes: `POST /api/lineups/matchup` and `POST /api/lineups/analyze-opponent`. Use whichever matches the new input shape. If backend needs updating, that becomes a separate task — consult the LineupController to confirm.

- [ ] **Step 8.7: Check backend analyze endpoint compatibility**

Run: `grep -n "matchup\|analyzeOpponent" backend/src/main/java/com/tennis/controller/LineupController.java`
Record the existing request shape. If current `matchup` endpoint accepts `{myTeamId, myLineupId, oppTeamId, oppLineupId}` or similar, use it. If it expects a different shape (e.g. expects full lineup objects inline), adapt the frontend to send that shape.

**If backend doesn't match:** Extend the existing request DTO in Java to accept the new field shape, then update mappers. (Out of scope for this mobile plan if intrusive — alternative is to resolve client-side: look up full lineup objects and send them).

- [ ] **Step 8.8: Run tests**

Run: `cd frontend && npm test 2>&1 | tail -10`

- [ ] **Step 8.9: Commit**

```bash
git add frontend/src/views/OpponentAnalysis.vue frontend/src/components/LineupPreviewCard.vue frontend/src/components/OpponentAnalysisResult.vue frontend/src/views/__tests__/OpponentAnalysis.test.js
git commit -m "feat(mobile): OpponentAnalysis dropdown-only flow with live previews + result cards"
```

---

## Task 9: E2E tests for mobile layout

**Files:**
- Create: `frontend/e2e/tests/mobile-layout.spec.js`

**Why:** Validate the mobile experience end-to-end at iPhone SE viewport (375x667).

- [ ] **Step 9.1: Create E2E spec**

File: `frontend/e2e/tests/mobile-layout.spec.js`

```js
import { test, expect, devices } from '@playwright/test'

test.use({ viewport: { width: 375, height: 667 } })

test.describe('Mobile layout', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
  })

  test('hamburger opens and closes sidebar drawer', async ({ page }) => {
    // Hamburger visible on mobile
    await expect(page.getByTestId('hamburger')).toBeVisible()
    // Sidebar initially hidden (has translate class)
    const aside = page.locator('aside').first()
    // Click hamburger
    await page.getByTestId('hamburger').click()
    // Drawer should be translated in (visible)
    await expect(aside).toHaveClass(/translate-x-0/)
    // Click overlay to close
    await page.locator('.bg-black\\/40').click()
    await expect(aside).not.toHaveClass(/translate-x-0$/)
  })

  test('team list → detail navigation hides list', async ({ page }) => {
    // List panel visible at /
    await expect(page.getByTestId('team-list-panel')).toBeVisible()
    // Click any team link
    const firstTeam = page.locator('a[href*="/teams/"]').first()
    const name = await firstTeam.textContent()
    await firstTeam.click()
    // Team list hidden on mobile when in detail
    await expect(page.getByTestId('team-list-panel')).toBeHidden()
    // Back button visible
    await expect(page.getByTestId('back-btn')).toBeVisible()
    // Click back
    await page.getByTestId('back-btn').click()
    await expect(page).toHaveURL('/')
    await expect(page.getByTestId('team-list-panel')).toBeVisible()
  })

  test('saved lineups displayed in single column on mobile', async ({ page }) => {
    // Requires a team with saved lineups — use existing seeded data
    await page.locator('a[href*="/teams/"]').first().click()
    // Navigate to lineups (find link)
    const lineupsLink = page.locator('a[href*="/lineups"]').first()
    if (await lineupsLink.isVisible()) {
      await lineupsLink.click()
      // The grid container should have grid-cols-1 (mobile default)
      const grid = page.locator('.grid').first()
      await expect(grid).toHaveClass(/grid-cols-1/)
    }
  })
})
```

- [ ] **Step 9.2: Run E2E (backend + frontend must be running)**

Run: `cd frontend && npm run test:e2e -- --grep 'Mobile layout'`
Expected: All pass. Fix selectors if needed.

- [ ] **Step 9.3: Commit**

```bash
git add frontend/e2e/tests/mobile-layout.spec.js
git commit -m "test(e2e): add mobile layout E2E coverage"
```

---

## Task 10: Desktop visual regression — manual check

**Files:** None (manual step)

- [ ] **Step 10.1: Start dev server**

Run: `cd frontend && npm run dev` (keep running)

- [ ] **Step 10.2: Visit http://localhost:5173 in browser at ≥ 1280px width**

Navigate through:
1. `/` — team list + detail two-column
2. Click a team → `/teams/:id` — two-column still
3. Navigate to 已保存排阵 — 2-column grid at `lg`
4. 排阵生成 — full desktop form
5. 对手分析 — full desktop form

Expected: All pages look the same as before this work (take screenshots if available).

- [ ] **Step 10.3: If any desktop regression found, fix by adding `lg:` variants**

Any Mobile-only style that leaked to desktop: wrap it with `lg:hidden` or scope to `< lg` only.

- [ ] **Step 10.4: Final commit if fixes made**

```bash
git add frontend/src/...
git commit -m "fix(desktop): preserve desktop layout after mobile optimization"
```

---

## Task 11: Deploy

- [ ] **Step 11.1: Run full test suite**

Run: `cd frontend && npm test 2>&1 | tail -5`
Expected: All pass.

Run backend: `cd backend && JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 /c/Users/lorra/tools/apache-maven-3.9.6/bin/mvn test 2>&1 | tail -5`
Expected: All pass.

- [ ] **Step 11.2: Push to origin**

```bash
git push origin main
```

- [ ] **Step 11.3: Deploy to Fly.io**

```bash
cd /c/Users/lorra/projects/tennis && flyctl deploy 2>&1 | tail -5
```

Expected: `Visit your newly deployed app at https://tennis-lineup.fly.dev/`

- [ ] **Step 11.4: Production verification**

Open https://tennis-lineup.fly.dev/ on:
1. Mobile browser (iPhone / Android Chrome)
2. Desktop browser at `>=1024px`

Verify both layouts work as expected.

---

## Self-Review Checklist

- **Spec coverage:**
  - Hamburger + drawer → Tasks 1, 2
  - Back button → Task 1 (AppHeader)
  - Team list/detail XOR on mobile → Task 3
  - Player mobile card → Task 4
  - LineupCard player row → Task 5
  - Saved lineups single column → Task 6 (verification; already exists)
  - Generator form + results → Task 7
  - Opponent analysis dropdowns + preview + result → Task 8
  - Desktop preservation → Task 10

- **Placeholders:** None — every step has concrete code or command.

- **Type consistency:**
  - `sidebarOpen` is a Vue ref in both MainLayout (`ref(false)`) and AppHeader (`inject` returns it) ✓
  - `data-testid` values consistent: `hamburger`, `back-btn`, `team-list-panel`, `player-card-mobile`, `player-card-detail`, `gender-tag`, `pair-player-row`, `select-my-team`, `select-my-lineup`, `select-opp-team`, `select-opp-lineup`, `analyze-btn`, `my-preview`, `opp-preview` ✓

- **Known risk:** Task 8 (OpponentAnalysis) depends on the backend matchup endpoint accepting the new input shape. Verify in Step 8.7 before committing. If backend change is needed, add a separate backend task before Task 8.9.
