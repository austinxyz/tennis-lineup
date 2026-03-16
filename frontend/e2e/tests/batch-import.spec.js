import { test, expect } from '@playwright/test'
import { TeamManagerPage } from '../pages/TeamManagerPage.js'
import { uniqueTeamName, csvContent } from '../fixtures/test-data.js'
import path from 'path'
import fs from 'fs'
import os from 'os'

/** Write CSV content to a temp file and return its path */
function writeTempCsv(content, filename = 'import.csv') {
  const filePath = path.join(os.tmpdir(), filename)
  fs.writeFileSync(filePath, content, 'utf-8')
  return filePath
}

test.describe('批量导入', () => {
  let teamPage

  test.beforeEach(async ({ page }) => {
    teamPage = new TeamManagerPage(page)
    await page.goto('/')
  })

  test('上传有效 CSV 文件后显示导入结果', async ({ page }) => {
    const csvPath = writeTempCsv(csvContent.valid, `valid-${Date.now()}.csv`)

    // Open import modal
    await page.getByRole('button', { name: '导入' }).click()

    // Upload file via hidden input
    const fileInput = page.locator('input[type="file"]')
    await fileInput.setInputFiles(csvPath)

    // Click import
    await page.getByRole('button', { name: '开始导入' }).click()

    // Verify import result div appears (uses bg-green-50 with border-green-200)
    const resultDiv = page.locator('div.bg-green-50.border-green-200')
    await expect(resultDiv).toBeVisible({ timeout: 10000 })
    const resultText = await resultDiv.textContent()
    expect(resultText).toMatch(/导入成功：\d+/)

    fs.unlinkSync(csvPath)
  })

  test('上传含无效行的 CSV 后显示失败计数', async ({ page }) => {
    const csvPath = writeTempCsv(csvContent.partiallyInvalid, `partial-${Date.now()}.csv`)

    await page.getByRole('button', { name: '导入' }).click()

    const fileInput = page.locator('input[type="file"]')
    await fileInput.setInputFiles(csvPath)

    await page.getByRole('button', { name: '开始导入' }).click()

    const resultDiv = page.locator('div.bg-green-50.border-green-200')
    await expect(resultDiv).toBeVisible({ timeout: 10000 })
    const resultText = await resultDiv.textContent()
    // Partially invalid CSV should show at least 1 failure
    expect(resultText).toMatch(/失败：[1-9]\d*/)

    fs.unlinkSync(csvPath)
  })
})
