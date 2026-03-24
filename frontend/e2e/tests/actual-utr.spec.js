import { test, expect } from '@playwright/test'
import { TeamManagerPage } from '../pages/TeamManagerPage.js'
import { PlayerDetailPage } from '../pages/PlayerDetailPage.js'
import { LineupGeneratorPage } from '../pages/LineupGeneratorPage.js'
import { uniqueTeamName } from '../fixtures/test-data.js'

// ─── Scenario 9.1: Add player with actualUtr set ────────────────────────────

test.describe('实际UTR — 添加球员', () => {
  let teamPage
  let playerPage
  let teamName

  test.beforeEach(async ({ page }) => {
    page.on('dialog', dialog => dialog.accept())
    teamPage = new TeamManagerPage(page)
    playerPage = new PlayerDetailPage(page)
    teamName = uniqueTeamName('实际UTR添加')
    await page.goto('/')
    await teamPage.createTeam(teamName)
  })

  test('9.1 添加球员时填写实际UTR，球员出现在列表中', async ({ page }) => {
    // Intercept the POST to capture request payload
    let capturedBody = null
    await page.route('**/api/teams/*/players', async (route) => {
      if (route.request().method() === 'POST') {
        capturedBody = route.request().postDataJSON()
      }
      await route.continue()
    })

    // Open "添加球员" modal
    await page.getByRole('button', { name: '添加球员' }).waitFor({ timeout: 5000 })
    await page.getByRole('button', { name: '添加球员' }).click()
    await page.locator('#playerName').waitFor({ timeout: 5000 })

    // Fill in player fields including actualUtr
    await page.locator('#playerName').fill('实UTR球员')
    await page.locator('#playerGender').selectOption('male')
    await page.locator('#playerUtr').fill('7.0')
    await page.locator('#playerActualUtr').fill('7.5')

    // Submit
    await page.getByRole('button', { name: '添加', exact: true }).click()
    // Wait for modal to close
    await page.waitForSelector('#playerName', { state: 'hidden', timeout: 5000 })

    // Verify player appears in the table
    const names = await playerPage.getPlayerNames()
    expect(names).toContain('实UTR球员')

    // Verify the request payload included actualUtr = 7.5
    expect(capturedBody).not.toBeNull()
    expect(capturedBody.actualUtr).toBe(7.5)
  })
})

// ─── Scenario 9.2: Bulk edit actualUtr for existing player ──────────────────
// ─── Scenario 9.3: Clear actualUtr in bulk edit ─────────────────────────────

test.describe('实际UTR — 批量编辑', () => {
  let teamPage
  let playerPage
  let teamName

  test.beforeEach(async ({ page }) => {
    page.on('dialog', dialog => dialog.accept())
    teamPage = new TeamManagerPage(page)
    playerPage = new PlayerDetailPage(page)
    teamName = uniqueTeamName('实际UTR批量')
    await page.goto('/')
    await teamPage.createTeam(teamName)

    // Add a single player to work with
    await playerPage.addPlayer({ name: '批量UTR球员', gender: 'male', utr: 6.0, verified: true })
  })

  test('9.2 批量编辑模式下显示"实际 UTR"列，设值后保存成功无错误', async ({ page }) => {
    // Enter UTR bulk edit mode
    await page.getByRole('button', { name: '批量编辑 UTR' }).click()

    // "实际 UTR" column header should be visible in the table
    await expect(page.getByRole('columnheader', { name: '实际 UTR' })).toBeVisible()

    // Find the actualUtr input for our player. It has placeholder "同UTR".
    // The player row is identified by the player name text in that row.
    const playerRow = page.locator('tbody tr').filter({ hasText: '批量UTR球员' }).first()
    const actualUtrInput = playerRow.locator('input[placeholder="同UTR"]')
    await actualUtrInput.waitFor({ timeout: 5000 })
    await actualUtrInput.fill('8.0')

    // Save
    await page.getByRole('button', { name: '保存', exact: true }).click()
    await page.waitForLoadState('networkidle')

    // No error banner should be visible
    await expect(page.locator('.bg-red-50')).not.toBeVisible()

    // Should have exited bulk edit mode (the "保存" button disappears)
    await expect(page.getByRole('button', { name: '批量编辑 UTR' })).toBeVisible()

    // The actualUtr cell should now display the orange value 8.00
    const actualUtrCell = page.locator('tbody tr').filter({ hasText: '批量UTR球员' }).first()
    await expect(actualUtrCell.locator('span.text-orange-600')).toContainText('8.00')
  })

  test('9.3 批量编辑清空实际UTR后保存成功，单元格回到"同UTR"灰色占位', async ({ page }) => {
    // First, set an actualUtr so there is one to clear
    await page.getByRole('button', { name: '批量编辑 UTR' }).click()
    const playerRow = page.locator('tbody tr').filter({ hasText: '批量UTR球员' }).first()
    const actualUtrInput = playerRow.locator('input[placeholder="同UTR"]')
    await actualUtrInput.waitFor({ timeout: 5000 })
    await actualUtrInput.fill('9.0')
    await page.getByRole('button', { name: '保存', exact: true }).click()
    await page.waitForLoadState('networkidle')

    // Now enter bulk edit again and clear the value
    await page.getByRole('button', { name: '批量编辑 UTR' }).click()
    const playerRow2 = page.locator('tbody tr').filter({ hasText: '批量UTR球员' }).first()
    const actualUtrInput2 = playerRow2.locator('input[placeholder="同UTR"]')
    await actualUtrInput2.waitFor({ timeout: 5000 })
    await actualUtrInput2.fill('')   // clear — null is valid

    // Save
    await page.getByRole('button', { name: '保存', exact: true }).click()
    await page.waitForLoadState('networkidle')

    // No error banner
    await expect(page.locator('.bg-red-50')).not.toBeVisible()

    // The actualUtr cell should now show the "同UTR" gray placeholder, not an orange value
    const row = page.locator('tbody tr').filter({ hasText: '批量UTR球员' }).first()
    await expect(row.locator('span.text-orange-600')).not.toBeVisible()
    await expect(row.locator('span.text-gray-300', { hasText: '同UTR' })).toBeVisible()
  })
})

// ─── Scenario 9.4: Lineup card shows "实:" when actualUtr is set ─────────────

const LINEUP_TEAM_NAME = uniqueTeamName('实际UTR排阵')

// 8 players: 2 female, 6 male — two of the males have actualUtr set so cards
// should render the orange "实:X.XX" indicator
const lineupPlayers = [
  { name: '张三实', gender: 'male', utr: 6.0, verified: true },
  { name: '李四实', gender: 'male', utr: 5.5, verified: true },
  { name: '王五实', gender: 'male', utr: 5.0, verified: true },
  { name: '赵梅实', gender: 'female', utr: 4.5, verified: true },
  { name: '孙阳实', gender: 'male', utr: 4.5, verified: true },
  { name: '林芳实', gender: 'female', utr: 4.0, verified: true },
  { name: '周杰实', gender: 'male', utr: 4.0, verified: true },
  { name: '吴磊实', gender: 'male', utr: 3.5, verified: true },
]

// actualUtr values to set for 张三实 and 李四实 via bulk edit after initial add
const actualUtrOverrides = {
  '张三实': 5.5,   // intentionally lower than UTR 6.0 to make sum differ
  '李四实': 5.0,
}

test.describe('实际UTR — 排阵卡片显示', () => {
  let teamPage
  let playerPage
  let lineupPage

  test.beforeAll(async ({ browser }) => {
    // Create team with 8 players, then set actualUtr for two players via bulk edit
    const page = await browser.newPage()
    page.on('dialog', d => d.accept())
    teamPage = new TeamManagerPage(page)
    playerPage = new PlayerDetailPage(page)

    await teamPage.goto()
    await teamPage.createTeam(LINEUP_TEAM_NAME)
    for (const player of lineupPlayers) {
      await playerPage.addPlayer(player)
    }

    // Enter UTR bulk edit to set actualUtr for 张三实 and 李四实
    await page.getByRole('button', { name: '批量编辑 UTR' }).click()
    for (const [name, value] of Object.entries(actualUtrOverrides)) {
      const row = page.locator('tbody tr').filter({ hasText: name }).first()
      const actualUtrInput = row.locator('input[placeholder="同UTR"]')
      await actualUtrInput.waitFor({ timeout: 5000 })
      await actualUtrInput.fill(String(value))
    }
    await page.getByRole('button', { name: '保存', exact: true }).click()
    await page.waitForLoadState('networkidle')
    await page.close()
  })

  test.beforeEach(async ({ page }) => {
    lineupPage = new LineupGeneratorPage(page)
    await lineupPage.goto()
  })

  test('9.4 生成排阵卡片：有实际UTR的球员在组合行显示"实:"指示', async ({ page }) => {
    await lineupPage.selectTeam(LINEUP_TEAM_NAME)
    await lineupPage.clickGenerate()
    await lineupPage.waitForResults()
    await lineupPage.waitForLineupCard()

    // At least one "实:X.XX" span should appear (orange text, inside a pair row)
    const actualUtrIndicators = page.locator('span.text-orange-500').filter({ hasText: /^实:/ })
    await expect(actualUtrIndicators.first()).toBeVisible({ timeout: 5000 })
  })

  test('9.4b 生成排阵卡片：标题区域显示"实际 UTR: X.XX"（当actualUtrSum与totalUtr不同时）', async ({ page }) => {
    await lineupPage.selectTeam(LINEUP_TEAM_NAME)
    await lineupPage.clickGenerate()
    await lineupPage.waitForResults()
    await lineupPage.waitForLineupCard()

    // The header "实际 UTR: X.XX" appears only when actualUtrSum != totalUtr (delta > 0.01)
    // Since we set actualUtr lower for two players, the sums will differ
    const headerActualUtr = page.locator('span.text-orange-500').filter({ hasText: /实际 UTR:/ })
    await expect(headerActualUtr.first()).toBeVisible({ timeout: 5000 })
  })
})
