import { test, expect } from '@playwright/test'
import { TeamManagerPage } from '../pages/TeamManagerPage.js'
import { PlayerDetailPage } from '../pages/PlayerDetailPage.js'
import { uniqueTeamName, samplePlayers } from '../fixtures/test-data.js'

test.describe('球员管理', () => {
  let teamPage
  let playerPage
  let teamName

  test.beforeEach(async ({ page }) => {
    // Accept all browser confirm dialogs (player delete shows one confirm)
    page.on('dialog', dialog => dialog.accept())

    teamPage = new TeamManagerPage(page)
    playerPage = new PlayerDetailPage(page)

    // Create a fresh team for each test
    teamName = uniqueTeamName('球员测试')
    await page.goto('/')
    await teamPage.createTeam(teamName)
  })

  test('添加球员后出现在列表且左侧计数加1', async ({ page }) => {
    const countBefore = await teamPage.getTeamPlayerCount(teamName)

    await playerPage.addPlayer(samplePlayers.male)

    const names = await playerPage.getPlayerNames()
    expect(names).toContain(samplePlayers.male.name)

    const countAfter = await teamPage.getTeamPlayerCount(teamName)
    expect(countAfter).toBe(countBefore + 1)
  })

  test('编辑球员后信息更新', async ({ page }) => {
    await playerPage.addPlayer(samplePlayers.female)

    const updatedName = `${samplePlayers.female.name}-改`
    await playerPage.editPlayer(samplePlayers.female.name, { newName: updatedName, utr: 6.0 })

    const names = await playerPage.getPlayerNames()
    expect(names).toContain(updatedName)
    expect(names).not.toContain(samplePlayers.female.name)
  })

  test('删除球员后从列表消失', async ({ page }) => {
    await playerPage.addPlayer(samplePlayers.highUtr)

    let names = await playerPage.getPlayerNames()
    expect(names).toContain(samplePlayers.highUtr.name)

    await playerPage.deletePlayer(samplePlayers.highUtr.name)

    names = await playerPage.getPlayerNames()
    expect(names).not.toContain(samplePlayers.highUtr.name)
  })
})
