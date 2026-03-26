import { test, expect, request } from '@playwright/test'
import { uniqueTeamName } from '../fixtures/test-data.js'
import { OpponentAnalysisPage } from '../pages/OpponentAnalysisPage.js'

const API = 'http://localhost:8080'

const OWN_TEAM_NAME = uniqueTeamName('己方队伍')
const OPP_TEAM_NAME = uniqueTeamName('对手队伍')

// 8 players satisfying constraints: 2 female, 6 male, totalUtr ≤ 40.5, gap ≤ 3.5
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

let ownTeamId
let oppTeamId

/**
 * Build a minimal saved lineup from a list of 8 players.
 * Pairs: D1=(0,1), D2=(2,3), D3=(4,5), D4=(6,7)
 */
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

test.describe('对手策略分析', () => {
  let analysisPage

  test.beforeAll(async () => {
    const api = await request.newContext({ baseURL: API })

    // Create own team
    const ownTeamRes = await api.post('/api/teams', {
      data: { name: OWN_TEAM_NAME },
    })
    const ownTeam = await ownTeamRes.json()
    ownTeamId = ownTeam.id

    // Add players to own team and collect with IDs
    const ownPlayersWithId = []
    for (const p of ownPlayers) {
      const res = await api.post(`/api/teams/${ownTeamId}/players`, { data: p })
      ownPlayersWithId.push(await res.json())
    }

    // Save a lineup for own team
    await api.post(`/api/teams/${ownTeamId}/lineups`, {
      data: buildLineup(ownPlayersWithId),
    })

    // Create opponent team
    const oppTeamRes = await api.post('/api/teams', {
      data: { name: OPP_TEAM_NAME },
    })
    const oppTeam = await oppTeamRes.json()
    oppTeamId = oppTeam.id

    // Add players to opponent team
    const oppPlayersWithId = []
    for (const p of oppPlayers) {
      const res = await api.post(`/api/teams/${oppTeamId}/players`, { data: p })
      oppPlayersWithId.push(await res.json())
    }

    // Save a lineup for opponent team
    await api.post(`/api/teams/${oppTeamId}/lineups`, {
      data: buildLineup(oppPlayersWithId),
    })

    await api.dispose()
  })

  test.beforeEach(async ({ page }) => {
    analysisPage = new OpponentAnalysisPage(page)
    await analysisPage.goto()
  })

  // --- Page structure ---

  test('页面显示对手策略分析标题', async ({ page }) => {
    await expect(page.locator('h2')).toContainText('对手策略分析')
  })

  test('显示最佳三阵和逐线对比两个模式按钮', async ({ page }) => {
    await expect(page.getByRole('button', { name: '最佳三阵', exact: true })).toBeVisible()
    await expect(page.getByRole('button', { name: '逐线对比', exact: true })).toBeVisible()
  })

  test('默认选中最佳三阵模式', async ({ page }) => {
    const btn = page.getByRole('button', { name: '最佳三阵', exact: true })
    await expect(btn).toHaveClass(/bg-white/)
  })

  test('侧边栏显示对手分析导航入口', async ({ page }) => {
    await expect(page.getByRole('link', { name: /对手/ })).toBeVisible()
  })

  // --- 最佳三阵 mode ---

  test('最佳三阵：选择队伍后查找按钮可点击', async ({ page }) => {
    await analysisPage.selectOwnTeam(OWN_TEAM_NAME)
    await analysisPage.selectOpponentTeam(OPP_TEAM_NAME)
    await page.waitForLoadState('networkidle')
    // select first available opponent lineup
    const oppSelect = page.locator('label:has-text("对手排阵") + select')
    await oppSelect.selectOption({ index: 1 })

    const btn = page.getByRole('button', { name: '查找最佳三阵' })
    await expect(btn).toBeEnabled()
  })

  test('最佳三阵：返回排阵对比结果并显示逐线分析', async ({ page }) => {
    await analysisPage.selectOwnTeam(OWN_TEAM_NAME)
    await analysisPage.selectOpponentTeam(OPP_TEAM_NAME)
    await page.waitForLoadState('networkidle')
    await page.locator('label:has-text("对手排阵") + select').selectOption({ index: 1 })

    await analysisPage.clickFindBestThree()
    await analysisPage.waitForResults()

    // Should show at least 1 result with per-line comparison
    await expect(page.locator('text=D1').first()).toBeVisible()
    await expect(page.locator('text=预期得分').first()).toBeVisible()
  })

  test('最佳三阵：结果显示胜负判断标签', async ({ page }) => {
    await analysisPage.selectOwnTeam(OWN_TEAM_NAME)
    await analysisPage.selectOpponentTeam(OPP_TEAM_NAME)
    await page.waitForLoadState('networkidle')
    await page.locator('label:has-text("对手排阵") + select').selectOption({ index: 1 })

    await analysisPage.clickFindBestThree()
    await analysisPage.waitForResults()

    // At least one verdict badge should be present
    const verdicts = page.locator('.rounded-full').filter({ hasText: /能赢|势均力敌|劣势/ })
    await expect(verdicts.first()).toBeVisible()
  })

  test('对手队伍无排阵时显示提示', async ({ page }) => {
    const api = await request.newContext({ baseURL: API })
    // Create a team with no lineups
    const res = await api.post('/api/teams', { data: { name: uniqueTeamName('空队伍') } })
    const emptyTeam = await res.json()
    await api.dispose()

    // Navigate fresh so teams list includes the newly created team
    await analysisPage.goto()
    await analysisPage.selectOpponentTeam(emptyTeam.name)
    await page.waitForLoadState('networkidle')
    await expect(page.locator('text=对手队伍暂无保存排阵')).toBeVisible()
  })

  // --- 逐线对比 mode ---

  test('切换到逐线对比模式显示己方排阵选择器', async ({ page }) => {
    await analysisPage.selectMode('headToHead')
    await expect(page.locator('label:has-text("己方排阵")')).toBeVisible()
    await expect(page.getByRole('button', { name: '对比分析' })).toBeVisible()
  })

  test('逐线对比：选择排阵后显示 UTR 比较结果', async ({ page }) => {
    await analysisPage.selectMode('headToHead')
    await analysisPage.selectOwnTeam(OWN_TEAM_NAME)
    await analysisPage.selectOpponentTeam(OPP_TEAM_NAME)
    await page.waitForLoadState('networkidle')

    await page.locator('label:has-text("对手排阵") + select').selectOption({ index: 1 })
    await page.locator('label:has-text("己方排阵") + select').selectOption({ index: 1 })

    await analysisPage.clickHeadToHeadAnalysis()
    await analysisPage.waitForResults()

    await expect(page.locator('text=UTR 比较分析')).toBeVisible()
    await expect(page.locator('text=预期得分').first()).toBeVisible()
  })

  test('逐线对比：结果显示后出现 AI 逐线评析按钮', async ({ page }) => {
    await analysisPage.selectMode('headToHead')
    await analysisPage.selectOwnTeam(OWN_TEAM_NAME)
    await analysisPage.selectOpponentTeam(OPP_TEAM_NAME)
    await page.waitForLoadState('networkidle')

    await page.locator('label:has-text("对手排阵") + select').selectOption({ index: 1 })
    await page.locator('label:has-text("己方排阵") + select').selectOption({ index: 1 })

    await analysisPage.clickHeadToHeadAnalysis()
    await analysisPage.waitForResults()

    await expect(page.getByRole('button', { name: /AI 逐线评析/ })).toBeVisible()
  })

  // --- 新功能: 排阵预览 ---

  test('逐线对比：选择对手排阵后显示预览（D1 出现在预览区域）', async ({ page }) => {
    await analysisPage.selectMode('headToHead')
    await analysisPage.selectOpponentTeam(OPP_TEAM_NAME)
    await page.waitForLoadState('networkidle')

    await page.locator('label:has-text("对手排阵") + select').selectOption({ index: 1 })

    // Preview should show D1 with player names (gray text below the dropdown, format: "D1: 乙一(UTR) + 乙二(UTR)")
    await expect(page.locator('div').filter({ hasText: /^D1:.*乙一.*乙二/ }).first()).toBeVisible()
  })

  // --- 新功能: 最佳三阵 AI 推荐 ---

  test('最佳三阵：结果显示后出现 AI 推荐按钮', async ({ page }) => {
    await analysisPage.selectOwnTeam(OWN_TEAM_NAME)
    await analysisPage.selectOpponentTeam(OPP_TEAM_NAME)
    await page.waitForLoadState('networkidle')
    await page.locator('label:has-text("对手排阵") + select').selectOption({ index: 1 })

    await analysisPage.clickFindBestThree()
    await analysisPage.waitForResults()

    await expect(page.getByRole('button', { name: '获取 AI 推荐' })).toBeVisible()
  })

  // --- 新功能: 逐线对比 AI 评析 ---

  test('逐线对比：点击 AI 逐线评析后显示评析结果或兜底文字', async ({ page }) => {
    await analysisPage.selectMode('headToHead')
    await analysisPage.selectOwnTeam(OWN_TEAM_NAME)
    await analysisPage.selectOpponentTeam(OPP_TEAM_NAME)
    await page.waitForLoadState('networkidle')

    await page.locator('label:has-text("对手排阵") + select').selectOption({ index: 1 })
    await page.locator('label:has-text("己方排阵") + select').selectOption({ index: 1 })

    await analysisPage.clickHeadToHeadAnalysis()
    await analysisPage.waitForResults()

    await page.getByRole('button', { name: /AI 逐线评析/ }).click()
    // Wait for the commentary result card header to appear (AI or fallback, both render this span)
    await expect(page.locator('.text-purple-800').filter({ hasText: 'AI 逐线评析' }).first()).toBeVisible({ timeout: 20000 })
  })

  test('逐线对比：切换回最佳三阵后结果被清空', async ({ page }) => {
    await analysisPage.selectMode('headToHead')
    // switch back
    await analysisPage.selectMode('bestThree')
    await expect(page.locator('text=UTR 比较分析')).not.toBeVisible()
    await expect(page.getByRole('button', { name: '查找最佳三阵' })).toBeVisible()
  })
})
