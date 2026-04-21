import { test, expect } from '@playwright/test'
import { TeamManagerPage } from '../pages/TeamManagerPage.js'
import { PlayerDetailPage } from '../pages/PlayerDetailPage.js'
import { uniqueTeamName } from '../fixtures/test-data.js'

test.describe('删除队伍保护：非空队伍不可删除', () => {
  let teamPage
  let playerPage

  test.beforeEach(async ({ page }) => {
    page.on('dialog', dialog => dialog.accept())
    teamPage = new TeamManagerPage(page)
    playerPage = new PlayerDetailPage(page)
    await page.goto('/')
  })

  test('有球员时删除按钮 disabled，清空后可删除', async ({ page }) => {
    const name = uniqueTeamName('删除保护')
    await teamPage.createTeam(name)

    // Add one player — team now non-empty
    await playerPage.addPlayer({ name: 'GuardAlice', gender: 'female', utr: 5.5 })

    // Go back to team list context (panel is always visible); re-select team to refresh state
    const teamLink = page.locator('a[href*="/teams/"]').filter({ hasText: name })
    const deleteBtn = teamLink.locator('button')
    await teamLink.scrollIntoViewIfNeeded()

    // Force visible so we can inspect the disabled attribute
    await deleteBtn.evaluate(el => { el.style.opacity = '1'; el.style.pointerEvents = 'auto' })

    // Assertion 1: button is disabled and carries an informative title
    await expect(deleteBtn).toBeDisabled()
    const title = await deleteBtn.getAttribute('title')
    expect(title).toContain('请先移除球员')

    // Remove the player, then the button should enable
    await teamPage.selectTeam(name)
    await playerPage.deletePlayer('GuardAlice')

    // Re-query the delete button (Vue re-rendered)
    const deleteBtnAfter = page.locator('a[href*="/teams/"]')
      .filter({ hasText: name })
      .locator('button')
    await deleteBtnAfter.evaluate(el => { el.style.opacity = '1'; el.style.pointerEvents = 'auto' })
    await expect(deleteBtnAfter).toBeEnabled()

    // Now delete the (now-empty) team — standard flow
    await teamPage.deleteTeam(name)
    await page.locator('a[href*="/teams/"]').filter({ hasText: name }).waitFor({
      state: 'detached',
      timeout: 5000,
    })
    const names = await teamPage.getTeamNames()
    expect(names).not.toContain(name)
  })
})
