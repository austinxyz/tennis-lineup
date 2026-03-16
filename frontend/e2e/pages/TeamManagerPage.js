export class TeamManagerPage {
  constructor(page) {
    this.page = page
  }

  async goto() {
    await this.page.goto('/')
  }

  /**
   * Click "创建队伍" and submit the form.
   * Waits for the team detail h2 heading to appear, confirming the team data has loaded.
   */
  async createTeam(name) {
    await this.page.getByRole('button', { name: '创建队伍' }).click()
    await this.page.getByPlaceholder('请输入队名').fill(name)
    await this.page.getByRole('button', { name: '创建', exact: true }).click()
    // h2 with the team name appears only after navigation AND team data has loaded
    await this.page.locator('h2').filter({ hasText: name }).waitFor({ timeout: 8000 })
  }

  /**
   * Delete a team by making its hidden button visible via JS, then clicking normally.
   * The button starts as opacity-0 (Tailwind group-hover). We force opacity:1 via
   * inline style so Playwright can click it without force, which properly fires Vue's
   * @click.prevent handler and allows dialog handling.
   * Caller's beforeEach must register: page.on('dialog', d => d.accept())
   */
  async deleteTeam(name) {
    const teamLink = this.page.locator('a[href*="/teams/"]').filter({ hasText: name })
    const deleteBtn = teamLink.locator('button')
    // Scroll into view in case the team is below the fold in the scrollable nav
    await teamLink.scrollIntoViewIfNeeded()
    // Force the button visible so Playwright can click it normally (avoids deadlock from evaluate(el.click()))
    await deleteBtn.evaluate(el => { el.style.opacity = '1'; el.style.pointerEvents = 'auto' })
    await deleteBtn.click()
  }

  /**
   * Click a team in the left panel to select it.
   * Waits for the team detail h2 heading to confirm the correct team loaded.
   */
  async selectTeam(name) {
    await this.page.locator('a[href*="/teams/"]').filter({ hasText: name }).click()
    await this.page.locator('h2').filter({ hasText: name }).waitFor({ timeout: 8000 })
  }

  /** Returns the player count shown under the team name (e.g. "3 名球员" → 3) */
  async getTeamPlayerCount(name) {
    const teamItem = this.page.locator('a[href*="/teams/"]').filter({ hasText: name })
    const countText = await teamItem.locator('.text-xs.text-gray-400').textContent()
    return parseInt(countText.trim(), 10)
  }

  /** Returns all team names currently in the left panel list */
  async getTeamNames() {
    const links = this.page.locator('a[href*="/teams/"]')
    const count = await links.count()
    const names = []
    for (let i = 0; i < count; i++) {
      const text = await links.nth(i).locator('.font-medium').textContent()
      if (text) names.push(text.trim())
    }
    return names
  }
}
