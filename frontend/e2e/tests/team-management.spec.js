import { test, expect } from '@playwright/test'
import { TeamManagerPage } from '../pages/TeamManagerPage.js'
import { uniqueTeamName } from '../fixtures/test-data.js'

test.describe('队伍管理', () => {
  let teamPage

  test.beforeEach(async ({ page }) => {
    // Accept all browser confirm dialogs (team delete triggers 2 confirms)
    page.on('dialog', dialog => dialog.accept())
    teamPage = new TeamManagerPage(page)
    await page.goto('/')
  })

  test('创建队伍后出现在左侧列表', async ({ page }) => {
    const name = uniqueTeamName('创建测试')
    await teamPage.createTeam(name)

    const names = await teamPage.getTeamNames()
    expect(names).toContain(name)
  })

  test('删除队伍后从左侧列表消失', async ({ page }) => {
    const name = uniqueTeamName('删除测试')
    await teamPage.createTeam(name)

    let names = await teamPage.getTeamNames()
    expect(names).toContain(name)

    await teamPage.deleteTeam(name)

    // Wait for the sidebar link to be removed from the DOM (reactive teams.value update)
    await page.locator('a[href*="/teams/"]').filter({ hasText: name }).waitFor({
      state: 'detached',
      timeout: 5000,
    })

    names = await teamPage.getTeamNames()
    expect(names).not.toContain(name)
  })
})
