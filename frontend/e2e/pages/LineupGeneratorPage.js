export class LineupGeneratorPage {
  constructor(page) {
    this.page = page
  }

  async goto() {
    await this.page.goto('/lineup')
    await this.page.waitForLoadState('networkidle')
  }

  async selectTeam(teamName) {
    await this.page.locator('select').selectOption({ label: teamName })
  }

  async selectPresetStrategy(preset) {
    // preset: 'balanced' or 'aggressive'
    const label = preset === 'balanced' ? '均衡' : '集中火力'
    await this.page.getByText(label, { exact: true }).click()
  }

  async switchToCustomStrategy() {
    await this.page.getByText('自定义策略').click()
  }

  async enterNaturalLanguage(text) {
    await this.page.locator('textarea').fill(text)
  }

  async clickGenerate() {
    await this.page.getByRole('button', { name: '生成排阵' }).click()
  }

  async waitForResults() {
    // Wait for "方案 1" label to appear in the results grid (no tab button needed)
    await this.page.locator('text=方案 1').waitFor({ timeout: 10000 })
  }

  async waitForLineupCard() {
    await this.page.locator('text=排阵结果').waitFor({ timeout: 10000 })
  }

  /**
   * Verify that all plan cards (方案 1 through 方案 N) are simultaneously visible without tab navigation.
   * @param {number} count expected number of plan cards
   */
  async verifyAllCardsVisible(count) {
    for (let i = 1; i <= count; i++) {
      await this.page.locator(`text=方案 ${i}`).waitFor({ timeout: 5000 })
    }
  }

  /**
   * Pin a player to a specific position using the constraint combobox.
   * @param {string} name player name
   * @param {string} position 'D1'|'D2'|'D3'|'D4'|'exclude'
   */
  async pinPlayerToPosition(name, position) {
    const value = position === 'exclude' ? '不上' : position
    // Find the row containing the player name, then select its combobox
    const row = this.page.locator('div').filter({ hasText: name }).filter({ has: this.page.locator('select') }).last()
    await row.locator('select').selectOption(value)
  }

  /**
   * Exclude a player by selecting '不上' in their constraint combobox.
   * @param {string} name player name
   */
  async excludePlayer(name) {
    await this.pinPlayerToPosition(name, 'exclude')
  }

  /**
   * Swap two players within a lineup card by opening the swap panel and selecting them.
   * @param {number} cardIndex 1-indexed lineup card number
   * @param {string} name1 first player name
   * @param {string} name2 second player name
   */
  async swapPlayers(cardIndex, name1, name2) {
    // Open the swap panel (click the <details> summary for the card)
    const cards = this.page.locator('[class*="ring-"]').or(this.page.locator('.border.rounded'))
    const card = cards.nth(cardIndex - 1)
    const detailsSummary = card.locator('details summary')
    if (await detailsSummary.isVisible()) {
      await detailsSummary.click()
    }
    // Click first player button
    const btn1 = card.getByRole('button', { name: new RegExp(name1) })
    await btn1.first().click()
    // Click second player button
    const btn2 = card.getByRole('button', { name: new RegExp(name2) })
    await btn2.first().click()
    // Click swap
    await card.getByRole('button', { name: '互换' }).click()
  }

  async getLineupPositions() {
    return this.page.locator('text=/D[1-4]/').allTextContents()
  }

  async getErrorMessage() {
    return this.page.locator('.bg-red-50').textContent()
  }

  isGenerateButtonEnabled() {
    return this.page.getByRole('button', { name: '生成排阵' }).isEnabled()
  }
}
