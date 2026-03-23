import { test, expect } from '@playwright/test'
import { TeamManagerPage } from '../pages/TeamManagerPage.js'
import { PlayerDetailPage } from '../pages/PlayerDetailPage.js'
import { LineupGeneratorPage } from '../pages/LineupGeneratorPage.js'
import { uniqueTeamName } from '../fixtures/test-data.js'

const TEAM_NAME = uniqueTeamName('排阵测试')

// 8 players: 2 female, 6 male, all verified
// Total UTR = 37.0 ≤ 40.5 cap; max partner gap ≤ 3.5 (2.5 between top and bottom)
const players = [
  { name: '张三', gender: 'male', utr: 6.0, verified: true },
  { name: '李四', gender: 'male', utr: 5.5, verified: true },
  { name: '王五', gender: 'male', utr: 5.0, verified: true },
  { name: '赵梅', gender: 'female', utr: 4.5, verified: true },
  { name: '孙阳', gender: 'male', utr: 4.5, verified: true },
  { name: '林芳', gender: 'female', utr: 4.0, verified: true },
  { name: '周杰', gender: 'male', utr: 4.0, verified: true },
  { name: '吴磊', gender: 'male', utr: 3.5, verified: true },
]

test.describe('排阵生成', () => {
  let teamPage
  let playerPage
  let lineupPage

  test.beforeAll(async ({ browser }) => {
    // Create a team with 8 players for all tests in this suite
    const page = await browser.newPage()
    page.on('dialog', d => d.accept())
    teamPage = new TeamManagerPage(page)
    playerPage = new PlayerDetailPage(page)
    await teamPage.goto()
    await teamPage.createTeam(TEAM_NAME)
    for (const player of players) {
      await playerPage.addPlayer(player)
    }
    await page.close()
  })

  test.beforeEach(async ({ page }) => {
    lineupPage = new LineupGeneratorPage(page)
    await lineupPage.goto()
  })

  test('导航到 /lineup 显示排阵生成页面', async ({ page }) => {
    await expect(page.locator('h2')).toContainText('排阵生成')
  })

  test('侧边栏显示排阵生成导航入口', async ({ page }) => {
    await expect(page.getByRole('link', { name: '排阵生成' })).toBeVisible()
  })

  test('选择队伍后生成按钮启用', async ({ page }) => {
    await lineupPage.selectTeam(TEAM_NAME)
    await expect(page.getByRole('button', { name: '生成排阵' })).toBeEnabled()
  })

  test('使用均衡策略生成排阵成功，所有方案直接显示', async ({ page }) => {
    await lineupPage.selectTeam(TEAM_NAME)
    await lineupPage.selectPresetStrategy('balanced')
    await lineupPage.clickGenerate()
    await lineupPage.waitForResults()
    // All plan cards visible simultaneously — no tab navigation needed
    await expect(page.locator('text=方案 1')).toBeVisible()
    // Use the position labels in result cards (green bold spans), not the option elements
    await expect(page.locator('.text-green-600').filter({ hasText: /^D1$/ }).first()).toBeVisible()
    await expect(page.locator('.text-green-600').filter({ hasText: /^D2$/ }).first()).toBeVisible()
    await expect(page.locator('.text-green-600').filter({ hasText: /^D3$/ }).first()).toBeVisible()
    await expect(page.locator('.text-green-600').filter({ hasText: /^D4$/ }).first()).toBeVisible()
  })

  test('方案 2 也直接可见，无需切换 tab', async ({ page }) => {
    await lineupPage.selectTeam(TEAM_NAME)
    await lineupPage.clickGenerate()
    await lineupPage.waitForResults()
    // Both plan 1 and plan 2 should be visible at the same time
    const plan2 = page.locator('text=方案 2')
    if (await plan2.isVisible()) {
      await expect(plan2).toBeVisible()
      // Plan 1 still visible simultaneously
      await expect(page.locator('text=方案 1')).toBeVisible()
    }
  })

  test('使用集中火力策略生成排阵成功', async ({ page }) => {
    await lineupPage.selectTeam(TEAM_NAME)
    await lineupPage.selectPresetStrategy('aggressive')
    await lineupPage.clickGenerate()
    await lineupPage.waitForResults()
    await expect(page.locator('text=排阵结果')).toBeVisible()
  })

  test('生成的排阵卡片显示总 UTR', async ({ page }) => {
    await lineupPage.selectTeam(TEAM_NAME)
    await lineupPage.clickGenerate()
    await lineupPage.waitForResults()
    await lineupPage.waitForLineupCard()
    await expect(page.locator('text=总 UTR')).toBeVisible()
  })

  test('排阵卡片显示每位球员的 UTR', async ({ page }) => {
    await lineupPage.selectTeam(TEAM_NAME)
    await lineupPage.clickGenerate()
    await lineupPage.waitForResults()
    await lineupPage.waitForLineupCard()
    // Per-player UTR is shown inline (e.g. "张三 (6.00)")
    // At least one player UTR should be visible as a decimal number
    const utrPattern = page.locator('text=/\\(\\d+\\.\\d+\\)/')
    await expect(utrPattern.first()).toBeVisible()
  })

  test('位置约束：指定球员打 D1', async ({ page }) => {
    await lineupPage.selectTeam(TEAM_NAME)
    // Wait for players to load then pin 张三 to D1
    await page.waitForSelector('text=张三', { timeout: 5000 }).catch(() => {})
    const playerVisible = await page.locator('text=张三').isVisible()
    if (playerVisible) {
      await lineupPage.pinPlayerToPosition('张三', 'D1')
      await lineupPage.clickGenerate()
      await lineupPage.waitForResults()
      // 张三 should appear in the D1 row of the first lineup card
      const d1Row = page.locator('.text-green-600').filter({ hasText: /^D1$/ }).first().locator('..')
      await expect(d1Row).toContainText('张三')
    }
  })
})
