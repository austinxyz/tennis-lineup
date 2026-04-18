/**
 * E2E tests for lineup export/import.
 *
 * Data isolation: Playwright global-setup replaces tennis-data.json with an
 * empty file before all tests run, and global-teardown restores the real file
 * after. These tests therefore operate on a clean slate and never touch
 * production data.
 *
 * Strategy:
 *  - beforeAll: create a source team + saved lineup via API, create a target team
 *  - Export test: navigate to source team lineup history, click export, intercept download
 *  - Import test: navigate to target team lineup history, import the downloaded file
 *  - Dedup test: import same file again → verify skipped count
 */
import { test, expect } from '@playwright/test'
import { uniqueTeamName } from '../fixtures/test-data.js'
import path from 'path'
import fs from 'fs'
import os from 'os'

const SOURCE_TEAM = uniqueTeamName('导出源队')
const TARGET_TEAM = uniqueTeamName('导入目标队')
// Use Vite dev server as proxy (avoids localhost→IPv6 issue on Windows where
// the backend binds on 127.0.0.1 but localhost resolves to ::1)
const API = 'http://127.0.0.1:8080'

// Fixture lineup matching the export envelope format
function makeExportJson(teamId, teamName) {
  return JSON.stringify({
    exportedAt: new Date().toISOString(),
    teamId,
    teamName,
    lineups: [
      {
        id: 'lineup-e2e-fixture',
        createdAt: new Date().toISOString(),
        strategy: 'balanced',
        aiUsed: false,
        valid: true,
        totalUtr: 37.0,
        actualUtrSum: 37.0,
        violationMessages: [],
        pairs: [
          {
            position: 'D1',
            player1Id: 'p-e2e-1', player1Name: 'E2E张三', player1Utr: 6.0, player1Gender: 'male',
            player2Id: 'p-e2e-2', player2Name: 'E2E李四', player2Utr: 5.5, player2Gender: 'male',
            combinedUtr: 11.5,
          },
          {
            position: 'D2',
            player1Id: 'p-e2e-3', player1Name: 'E2E王五', player1Utr: 5.0, player1Gender: 'male',
            player2Id: 'p-e2e-4', player2Name: 'E2E赵梅', player2Utr: 4.5, player2Gender: 'female',
            combinedUtr: 9.5,
          },
          {
            position: 'D3',
            player1Id: 'p-e2e-5', player1Name: 'E2E孙阳', player1Utr: 4.5, player1Gender: 'male',
            player2Id: 'p-e2e-6', player2Name: 'E2E林芳', player2Utr: 4.0, player2Gender: 'female',
            combinedUtr: 8.5,
          },
          {
            position: 'D4',
            player1Id: 'p-e2e-7', player1Name: 'E2E周杰', player1Utr: 4.0, player1Gender: 'male',
            player2Id: 'p-e2e-8', player2Name: 'E2E吴磊', player2Utr: 3.5, player2Gender: 'male',
            combinedUtr: 7.5,
          },
        ],
      },
    ],
  })
}

test.describe('排阵导出导入', () => {
  let sourceTeamId
  let targetTeamId
  let exportFilePath

  test.beforeAll(async ({ request }) => {
    // Create source team via API
    const srcRes = await request.post(`${API}/api/teams`, {
      data: { name: SOURCE_TEAM },
    })
    expect(srcRes.ok()).toBeTruthy()
    sourceTeamId = (await srcRes.json()).id

    // Save a lineup directly via API (avoids slow AI lineup generation)
    const lineup = JSON.parse(makeExportJson(sourceTeamId, SOURCE_TEAM)).lineups[0]
    const lineupRes = await request.post(`${API}/api/teams/${sourceTeamId}/lineups`, {
      data: lineup,
    })
    expect(lineupRes.ok()).toBeTruthy()

    // Create target team via API
    const tgtRes = await request.post(`${API}/api/teams`, {
      data: { name: TARGET_TEAM },
    })
    expect(tgtRes.ok()).toBeTruthy()
    targetTeamId = (await tgtRes.json()).id

    // Write fixture export JSON to temp file (used by import tests)
    exportFilePath = path.join(os.tmpdir(), `lineup-export-e2e-${Date.now()}.json`)
    fs.writeFileSync(exportFilePath, makeExportJson(sourceTeamId, SOURCE_TEAM), 'utf-8')
  })

  test.afterAll(async () => {
    if (exportFilePath && fs.existsSync(exportFilePath)) {
      fs.unlinkSync(exportFilePath)
    }
  })

  // ── Export ────────────────────────────────────────────────────────────────

  test('导出按钮触发导出请求，响应包含正确的 teamId 和 lineups', async ({ page }) => {
    await page.goto(`/teams/${sourceTeamId}/lineups`)
    await expect(page.getByRole('button', { name: '导出排阵' })).toBeVisible({ timeout: 8000 })

    // Intercept the export API response (more reliable than download event in headless Chromium)
    const [response] = await Promise.all([
      page.waitForResponse(r => r.url().includes('/lineups/export'), { timeout: 10000 }),
      page.getByRole('button', { name: '导出排阵' }).click(),
    ])

    expect(response.status()).toBe(200)
    const body = await response.json()
    expect(body.teamId).toBe(sourceTeamId)
    expect(body.teamName).toBe(SOURCE_TEAM)
    expect(Array.isArray(body.lineups)).toBe(true)
    expect(body.lineups.length).toBeGreaterThanOrEqual(1)
    expect(body.exportedAt).toBeTruthy()
  })

  // ── Import ────────────────────────────────────────────────────────────────

  test('导入排阵后显示成功提示并刷新列表', async ({ page }) => {
    await page.goto(`/teams/${targetTeamId}/lineups`)
    await expect(page.getByRole('button', { name: '导入排阵' })).toBeVisible({ timeout: 8000 })

    // Upload the fixture export JSON
    const fileInput = page.locator('input[type="file"][accept=".json"]')
    await fileInput.setInputFiles(exportFilePath)

    // Wait for success message
    const successMsg = page.locator('[data-testid="import-result"]')
    await expect(successMsg).toBeVisible({ timeout: 10000 })
    await expect(successMsg).toContainText('导入成功：1 条，跳过：0 条')

    // Lineup card should appear in the list
    await expect(page.locator('[data-testid="lineup-card"], .bg-white.rounded-xl').first()).toBeVisible()
  })

  // ── Dedup ─────────────────────────────────────────────────────────────────

  test('重复导入同一文件时显示 skipped 计数', async ({ page }) => {
    await page.goto(`/teams/${targetTeamId}/lineups`)
    await expect(page.getByRole('button', { name: '导入排阵' })).toBeVisible({ timeout: 8000 })

    // Import the same file again — all lineups already exist (by player name)
    const fileInput = page.locator('input[type="file"][accept=".json"]')
    await fileInput.setInputFiles(exportFilePath)

    const resultMsg = page.locator('[data-testid="import-result"]')
    await expect(resultMsg).toBeVisible({ timeout: 10000 })
    await expect(resultMsg).toContainText('导入成功：0 条，跳过：1 条')
  })

  // ── Error handling ────────────────────────────────────────────────────────

  test('导入格式非法的文件时显示错误提示', async ({ page }) => {
    await page.goto(`/teams/${targetTeamId}/lineups`)
    await expect(page.getByRole('button', { name: '导入排阵' })).toBeVisible({ timeout: 8000 })

    // Write invalid JSON to temp file
    const badFile = path.join(os.tmpdir(), `bad-${Date.now()}.json`)
    fs.writeFileSync(badFile, 'not-valid-json', 'utf-8')

    const fileInput = page.locator('input[type="file"][accept=".json"]')
    await fileInput.setInputFiles(badFile)

    const errorMsg = page.locator('[data-testid="import-result"]')
    await expect(errorMsg).toBeVisible({ timeout: 10000 })

    fs.unlinkSync(badFile)
  })
})
