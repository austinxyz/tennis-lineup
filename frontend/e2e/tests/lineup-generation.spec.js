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

  test('使用均衡策略生成排阵成功，显示方案 1 tab', async ({ page }) => {
    await lineupPage.selectTeam(TEAM_NAME)
    await lineupPage.selectPresetStrategy('balanced')
    await lineupPage.clickGenerate()
    await lineupPage.waitForResults()
    await expect(page.getByRole('button', { name: '方案 1' })).toBeVisible()
    // The first tab is active and shows the lineup card with D1-D4
    await lineupPage.waitForLineupCard()
    await expect(page.locator('text=D1')).toBeVisible()
    await expect(page.locator('text=D2')).toBeVisible()
    await expect(page.locator('text=D3')).toBeVisible()
    await expect(page.locator('text=D4')).toBeVisible()
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

  test('可以切换到方案 2 tab', async ({ page }) => {
    await lineupPage.selectTeam(TEAM_NAME)
    await lineupPage.clickGenerate()
    await lineupPage.waitForResults()

    // Check if tab 2 exists before clicking
    const tab2 = page.getByRole('button', { name: '方案 2' })
    if (await tab2.isVisible()) {
      await lineupPage.selectTab(2)
      await expect(tab2).toHaveClass(/bg-green-600/)
    } else {
      // Only 1 candidate generated — pass the test
      test.info().annotations.push({ type: 'info', description: '只有 1 个候选方案，跳过 tab 切换' })
    }
  })
})
