import { test, expect } from '@playwright/test'
import { TeamManagerPage } from '../pages/TeamManagerPage.js'
import { PlayerDetailPage } from '../pages/PlayerDetailPage.js'
import { uniqueTeamName } from '../fixtures/test-data.js'

test.describe('队伍切换', () => {
  test('切换队伍后球员列表正确刷新', async ({ page }) => {
    // Accept all confirm dialogs
    page.on('dialog', dialog => dialog.accept())

    const teamPage = new TeamManagerPage(page)
    const playerPage = new PlayerDetailPage(page)

    await page.goto('/')

    // Create Team A and add a player
    const teamA = uniqueTeamName('切换A')
    await teamPage.createTeam(teamA)
    await playerPage.addPlayer({ name: `${teamA}-球员`, gender: 'male', utr: 5.0 })

    // Create Team B and add a different player
    const teamB = uniqueTeamName('切换B')
    await teamPage.createTeam(teamB)
    await playerPage.addPlayer({ name: `${teamB}-球员`, gender: 'female', utr: 4.0 })

    // Switch to Team A — verify only Team A's player is shown
    await teamPage.selectTeam(teamA)
    let names = await playerPage.getPlayerNames()
    expect(names).toContain(`${teamA}-球员`)
    expect(names).not.toContain(`${teamB}-球员`)

    // Switch to Team B — verify only Team B's player is shown
    await teamPage.selectTeam(teamB)
    names = await playerPage.getPlayerNames()
    expect(names).toContain(`${teamB}-球员`)
    expect(names).not.toContain(`${teamA}-球员`)
  })
})
