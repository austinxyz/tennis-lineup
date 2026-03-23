import { test, expect, request } from '@playwright/test'
import { uniqueTeamName } from '../fixtures/test-data.js'
import { TeamManagerPage } from '../pages/TeamManagerPage.js'
import { OpponentAnalysisPage } from '../pages/OpponentAnalysisPage.js'

const API = 'http://localhost:8080'

const OWN_TEAM_NAME = uniqueTeamName('笔记己方')
const OPP_TEAM_NAME = uniqueTeamName('笔记对手')

const ownPlayers = [
  { name: '甲一', gender: 'male', utr: 6.0, verified: true },
  { name: '甲二', gender: 'male', utr: 5.5, verified: true },
  { name: '甲三', gender: 'male', utr: 5.0, verified: true },
  { name: '甲四', gender: 'female', utr: 4.5, verified: true },
  { name: '甲五', gender: 'male', utr: 4.5, verified: true },
  { name: '甲六', gender: 'female', utr: 4.0, verified: true },
  { name: '甲七', gender: 'male', utr: 4.0, verified: true },
  { name: '甲八', gender: 'male', utr: 3.5, verified: true },
]

const oppPlayers = [
  { name: '乙一', gender: 'male', utr: 5.5, verified: true },
  { name: '乙二', gender: 'male', utr: 5.0, verified: true },
  { name: '乙三', gender: 'male', utr: 4.5, verified: true },
  { name: '乙四', gender: 'female', utr: 4.5, verified: true },
  { name: '乙五', gender: 'male', utr: 4.0, verified: true },
  { name: '乙六', gender: 'female', utr: 4.0, verified: true },
  { name: '乙七', gender: 'male', utr: 3.5, verified: true },
  { name: '乙八', gender: 'male', utr: 3.0, verified: true },
]

let ownTeamId, oppTeamId, player1Id, player2Id

test.describe('队员笔记功能', () => {
  test.beforeAll(async () => {
    const api = await request.newContext({ baseURL: API })

    const ownTeamRes = await api.post('/api/teams', { data: { name: OWN_TEAM_NAME } })
    ownTeamId = (await ownTeamRes.json()).id

    const addedPlayers = []
    for (const p of ownPlayers) {
      const res = await api.post(`/api/teams/${ownTeamId}/players`, { data: p })
      addedPlayers.push(await res.json())
    }
    player1Id = addedPlayers[0].id
    player2Id = addedPlayers[1].id

    const oppTeamRes = await api.post('/api/teams', { data: { name: OPP_TEAM_NAME } })
    oppTeamId = (await oppTeamRes.json()).id
    for (const p of oppPlayers) {
      await api.post(`/api/teams/${oppTeamId}/players`, { data: p })
    }

    await api.dispose()
  })

  // --- 个人备注列 ---

  test('球员表格显示「个人备注」列', async ({ page }) => {
    const teamPage = new TeamManagerPage(page)
    await teamPage.goto()
    await teamPage.selectTeam(OWN_TEAM_NAME)
    await expect(page.getByRole('columnheader', { name: '个人备注' })).toBeVisible()
  })

  test('球员表格显示「搭档笔记」列', async ({ page }) => {
    const teamPage = new TeamManagerPage(page)
    await teamPage.goto()
    await teamPage.selectTeam(OWN_TEAM_NAME)
    await expect(page.getByRole('columnheader', { name: '搭档笔记' })).toBeVisible()
  })

  test('批量编辑 Notes：编辑并保存后个人备注显示在行内', async ({ page }) => {
    const teamPage = new TeamManagerPage(page)
    await teamPage.goto()
    await teamPage.selectTeam(OWN_TEAM_NAME)

    await page.getByRole('button', { name: '批量编辑 Notes' }).click()
    await expect(page.locator('text=正在批量编辑个人备注')).toBeVisible()

    // Fill first player's note
    const inputs = page.locator('td input[type="text"]')
    await inputs.first().waitFor({ timeout: 5000 })
    await inputs.first().fill('攻击型，发球强')
    await page.getByRole('button', { name: '保存全部' }).click()
    await page.waitForLoadState('networkidle')

    // Should exit bulk mode and show note inline
    await expect(page.locator('text=正在批量编辑个人备注')).not.toBeVisible()
    await expect(page.locator('text=攻击型，发球强')).toBeVisible()
  })

  test('批量编辑 Notes：保存后刷新仍显示', async ({ page }) => {
    const teamPage = new TeamManagerPage(page)
    await teamPage.goto()
    await teamPage.selectTeam(OWN_TEAM_NAME)

    await page.getByRole('button', { name: '批量编辑 Notes' }).click()
    const inputs = page.locator('td input[type="text"]')
    await inputs.first().waitFor({ timeout: 5000 })
    await inputs.first().fill('持续备注')
    await page.getByRole('button', { name: '保存全部' }).click()
    await page.waitForLoadState('networkidle')

    await page.reload()
    await page.waitForLoadState('networkidle')
    await expect(page.locator('text=持续备注')).toBeVisible({ timeout: 5000 })
  })

  test('展开行：添加搭档笔记后 chip 出现在行内', async ({ page }) => {
    const teamPage = new TeamManagerPage(page)
    await teamPage.goto()
    await teamPage.selectTeam(OWN_TEAM_NAME)

    // Click the expand arrow on the first player row
    const expandBtn = page.locator('tbody tr').first().locator('button').first()
    await expandBtn.click()

    // Partner notes row should appear
    const partnerSection = page.locator('text=搭档笔记').first()
    await expect(partnerSection).toBeVisible({ timeout: 3000 })

    // Select a partner and fill note in the blank row
    const selects = page.locator('.bg-purple-50 select')
    await selects.last().selectOption({ index: 1 })
    const noteInput = page.locator('.bg-purple-50 input').last()
    await noteInput.fill('一起打过USTA，默契好')

    await page.locator('.bg-purple-50').getByRole('button', { name: '保存' }).click()
    await page.waitForLoadState('networkidle')

    // Chip should now appear in the row
    await expect(page.locator('.bg-purple-100').first()).toBeVisible({ timeout: 5000 })
  })

  // --- 对手分析页 ---

  test('对手分析页选择对手队伍后显示对手搭档笔记入口', async ({ page }) => {
    const analysisPage = new OpponentAnalysisPage(page)
    await analysisPage.goto()
    await analysisPage.selectOpponentTeam(OPP_TEAM_NAME)
    await page.waitForLoadState('networkidle')

    await expect(page.locator('text=对手搭档笔记')).toBeVisible()
  })

  test('对手分析页可展开对手搭档笔记并添加', async ({ page }) => {
    const analysisPage = new OpponentAnalysisPage(page)
    await analysisPage.goto()
    await analysisPage.selectOpponentTeam(OPP_TEAM_NAME)
    await page.waitForLoadState('networkidle')

    await page.locator('button:has-text("对手搭档笔记")').click()
    await expect(page.getByRole('button', { name: '添加搭档笔记' })).toBeVisible({ timeout: 3000 })
  })
})
