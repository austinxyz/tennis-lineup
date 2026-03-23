export class PlayerDetailPage {
  constructor(page) {
    this.page = page
  }

  /** Click "添加球员" and fill the form, then submit */
  async addPlayer({ name, gender = 'male', utr = 5.0, verified = false, notes } = {}) {
    // Ensure the detail page is ready before clicking
    await this.page.getByRole('button', { name: '添加球员' }).waitFor({ timeout: 5000 })
    await this.page.getByRole('button', { name: '添加球员' }).click()
    await this.page.locator('#playerName').waitFor({ timeout: 5000 })
    await this.page.locator('#playerName').fill(name)
    await this.page.locator('#playerGender').selectOption(gender)
    await this.page.locator('#playerUtr').fill(String(utr))
    if (verified) {
      await this.page.locator('#playerVerified').check()
    }
    if (notes) {
      await this.page.locator('textarea').fill(notes)
    }
    await this.page.getByRole('button', { name: '添加', exact: true }).click()
    // Wait for modal to close
    await this.page.waitForSelector('#playerName', { state: 'hidden', timeout: 5000 })
  }

  /** Click the edit button for a player by name, update fields, then save */
  async editPlayer(name, { newName, gender, utr, verified, notes } = {}) {
    const row = this.page.locator('tbody tr').filter({ hasText: name })
    await row.getByRole('button', { name: '编辑' }).click()
    if (newName !== undefined) {
      await this.page.locator('#playerName').fill(newName)
    }
    if (gender !== undefined) {
      await this.page.locator('#playerGender').selectOption(gender)
    }
    if (utr !== undefined) {
      await this.page.locator('#playerUtr').fill(String(utr))
    }
    if (verified !== undefined) {
      const checkbox = this.page.locator('#playerVerified')
      if (verified) await checkbox.check()
      else await checkbox.uncheck()
    }
    if (notes !== undefined) {
      await this.page.locator('textarea').fill(notes)
    }
    await this.page.getByRole('button', { name: '更新' }).click()
    await this.page.waitForSelector('#playerName', { state: 'hidden', timeout: 5000 })
  }

  /**
   * Click the delete button for a player by name.
   * NOTE: The caller test must register a dialog handler BEFORE calling this method,
   * e.g.: page.on('dialog', d => d.accept()) in beforeEach.
   */
  async deletePlayer(name) {
    const row = this.page.locator('tbody tr').filter({ hasText: name })
    await row.locator('button').filter({ hasText: '删除' }).click()
    // Wait for the row to be removed from the DOM
    await row.waitFor({ state: 'detached', timeout: 5000 })
  }

  /** Returns array of player names in the table */
  async getPlayerNames() {
    const rows = this.page.locator('tbody tr td:nth-child(2) .text-sm.font-medium')
    const count = await rows.count()
    const names = []
    for (let i = 0; i < count; i++) {
      const text = await rows.nth(i).textContent()
      if (text) names.push(text.trim())
    }
    return names
  }

  /** Returns the displayed player count from the section heading "球员列表 (N)" */
  async getDisplayedPlayerCount() {
    const heading = await this.page.locator('h3').filter({ hasText: '球员列表' }).textContent()
    const match = heading.match(/\((\d+)\)/)
    return match ? parseInt(match[1], 10) : 0
  }
}
