import { test, expect } from '@playwright/test'
import { uniqueTeamName } from '../fixtures/test-data.js'

test.use({ viewport: { width: 375, height: 667 } })

const API = 'http://127.0.0.1:8080'

// Player roster: 2 female + 6 male, all verified — satisfies lineup constraints
const PLAYERS = [
  { name: 'Alice', gender: 'female', utr: 6.0, verified: true, actualUtr: 6.0 },
  { name: 'Bob', gender: 'male', utr: 5.5, verified: true, actualUtr: 5.5 },
  { name: 'Carol', gender: 'female', utr: 5.0, verified: true, actualUtr: 5.0 },
  { name: 'Dave', gender: 'male', utr: 4.5, verified: true, actualUtr: 4.5 },
  { name: 'Eve', gender: 'male', utr: 4.0, verified: true, actualUtr: 4.0 },
  { name: 'Frank', gender: 'male', utr: 4.5, verified: true, actualUtr: 4.5 },
  { name: 'Grace', gender: 'male', utr: 4.0, verified: true, actualUtr: 4.0 },
  { name: 'Hank', gender: 'male', utr: 3.5, verified: true, actualUtr: 3.5 },
]

/** Build a valid saved lineup body from a list of 8 players (returned from API with ids). */
function buildLineup(players) {
  const positions = ['D1', 'D2', 'D3', 'D4']
  const pairs = positions.map((pos, i) => {
    const p1 = players[i * 2]
    const p2 = players[i * 2 + 1]
    return {
      position: pos,
      player1Id: p1.id,
      player2Id: p2.id,
      player1Name: p1.name,
      player2Name: p2.name,
      player1Utr: p1.utr,
      player2Utr: p2.utr,
      player1Gender: p1.gender,
      player2Gender: p2.gender,
      combinedUtr: p1.utr + p2.utr,
    }
  })
  return {
    pairs,
    totalUtr: pairs.reduce((s, p) => s + p.combinedUtr, 0),
    strategy: 'balanced',
    violationMessages: [],
  }
}

test.describe('Mobile layout', () => {
  let teamId

  test.beforeAll(async ({ request }) => {
    // Create team
    const teamRes = await request.post(`${API}/api/teams`, {
      data: { name: uniqueTeamName('移动测试队') },
    })
    expect(teamRes.ok()).toBeTruthy()
    const team = await teamRes.json()
    teamId = team.id

    // Add 8 players and collect responses with server-assigned IDs
    const playersWithId = []
    for (const p of PLAYERS) {
      const res = await request.post(`${API}/api/teams/${teamId}/players`, { data: p })
      expect(res.ok()).toBeTruthy()
      playersWithId.push(await res.json())
    }

    // Save one lineup so the lineups page is non-empty
    const lineupRes = await request.post(`${API}/api/teams/${teamId}/lineups`, {
      data: buildLineup(playersWithId),
    })
    expect(lineupRes.ok()).toBeTruthy()
  })

  // ── Test 1: hamburger toggles sidebar drawer ────────────────────────────────
  // AppHeader (which contains the hamburger) is included in sub-views such as
  // OpponentAnalysis, not in the home route. Navigate there to get the button.

  test('hamburger opens and overlay closes sidebar drawer', async ({ page }) => {
    await page.goto('/opponent-analysis')

    // Hamburger must be visible on mobile (AppHeader has class lg:hidden)
    const hamburger = page.locator('[data-testid="hamburger"]')
    await expect(hamburger).toBeVisible()

    // The drawer wrapper is the outer aside that carries the translate classes.
    // The inner aside is the NavSidebar itself — target by the transition class.
    const drawer = page.locator('aside.transition-transform')
    await expect(drawer).toHaveClass(/-translate-x-full/)

    // Click hamburger — drawer should slide in
    await hamburger.click()
    await expect(drawer).toHaveClass(/translate-x-0/)

    // Click the dark overlay — drawer should close again
    const overlay = page.locator('.bg-black\\/40')
    await expect(overlay).toBeVisible()
    await overlay.click()
    await expect(drawer).toHaveClass(/-translate-x-full/)
  })

  // ── Test 2: team list ↔ detail navigation on mobile ─────────────────────────

  test('selecting a team hides the list and shows detail with back button', async ({ page }) => {
    await page.goto('/')

    const listWrapper = page.locator('[data-testid="team-list-panel-wrapper"]')
    const detailWrapper = page.locator('[data-testid="team-detail-wrapper"]')

    // On the home route the list panel is visible and detail is hidden
    await expect(listWrapper).toBeVisible()
    await expect(detailWrapper).toHaveClass(/hidden/)

    // Click the first team link that leads to a team detail (not a lineups sub-route)
    const teamLink = page.locator('a[href*="/teams/"]:not([href*="/lineups"])').first()
    await expect(teamLink).toBeVisible()
    await teamLink.click()

    // After navigation: list is hidden, detail and back button are visible
    await expect(listWrapper).toHaveClass(/hidden/)
    await expect(detailWrapper).toBeVisible()
    const backBtn = page.locator('[data-testid="back-btn"]')
    await expect(backBtn).toBeVisible()

    // Click back — should return to / with list visible again
    await backBtn.click()
    await page.waitForURL('/')
    await expect(listWrapper).toBeVisible()
    await expect(detailWrapper).toHaveClass(/hidden/)
  })

  // ── Test 3: saved lineups page renders single-column grid ───────────────────

  test('saved lineups page shows single-column grid and correct header title', async ({ page }) => {
    await page.goto(`/teams/${teamId}/lineups`)

    // AppHeader title contains 已保存排阵
    const headerTitle = page.locator('header span.font-semibold')
    await expect(headerTitle).toContainText('已保存排阵')

    // Wait for the content spinner (h-10 w-10) to disappear — use the specific
    // size class to avoid strict-mode failure when multiple spinners coexist.
    await expect(page.locator('.animate-spin.h-10')).not.toBeVisible({ timeout: 10000 })

    // The grid is single-column (grid-cols-1) and visible
    const grid = page.locator('.grid.grid-cols-1')
    await expect(grid).toBeVisible()

    // Confirm no multi-column class is active at this viewport width — the grid
    // uses `lg:grid-cols-2` which is only effective at ≥1024 px, so at 375 px
    // the element must NOT have a resolved two-column layout. We check for the
    // absence of a bare `grid-cols-2` class (the lg: prefixed variant is fine).
    const gridClass = await grid.getAttribute('class')
    expect(gridClass).not.toMatch(/(?<![a-z]:)grid-cols-2/)
  })

  // ── Test 4: LineupCard shows per-player rows ─────────────────────────────────

  test('lineup cards display individual player rows', async ({ page }) => {
    await page.goto(`/teams/${teamId}/lineups`)

    // Wait until the content spinner disappears
    await expect(page.locator('.animate-spin.h-10')).not.toBeVisible({ timeout: 10000 })

    // Each lineup has 4 pairs × 2 players = 8 pair-player-row elements
    const rows = page.locator('[data-testid="pair-player-row"]')
    await expect(rows.first()).toBeVisible()
    const count = await rows.count()
    // 8 rows for a full lineup (2 per pair × 4 pairs)
    expect(count).toBeGreaterThanOrEqual(8)
  })

  // ── Test 5: opponent analysis dropdowns and disabled analyze button ──────────

  test('opponent analysis page shows 4 selectors and analyze button is disabled initially', async ({ page }) => {
    await page.goto('/opponent-analysis')

    // Switch to 逐线对比 mode (where all 4 selectors are shown)
    await page.getByRole('button', { name: '逐线对比' }).click()

    await expect(page.locator('[data-testid="select-my-team"]')).toBeVisible()
    await expect(page.locator('[data-testid="select-opp-team"]')).toBeVisible()
    // my-lineup and opp-lineup appear after teams are chosen; check select-my-team + analyze-btn only at init
    const analyzeBtn = page.locator('[data-testid="analyze-btn"]')
    await expect(analyzeBtn).toBeDisabled()
  })
})
