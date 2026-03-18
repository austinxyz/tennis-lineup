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
    // Wait for at least one tab button ("方案 1") to appear in the results grid
    await this.page.getByRole('button', { name: '方案 1' }).waitFor({ timeout: 10000 })
  }

  async waitForLineupCard() {
    await this.page.locator('text=排阵结果').waitFor({ timeout: 10000 })
  }

  async selectTab(n) {
    // n is 1-indexed
    await this.page.getByRole('button', { name: `方案 ${n}` }).click()
  }

  /**
   * Click the constraint toggle button next to the player with the given name.
   * Toggle cycles: 中立 → 必须上场 → 排除 → 中立
   * @param {string} name player name
   */
  async pinPlayer(name) {
    // Find the row containing the player name and click its toggle button once
    const row = this.page.locator('div').filter({ hasText: name }).last()
    await row.getByRole('button').click()
  }

  /**
   * Exclude a player by clicking their toggle button twice (neutral → include → exclude).
   * @param {string} name player name
   */
  async excludePlayer(name) {
    const row = this.page.locator('div').filter({ hasText: name }).last()
    const btn = row.getByRole('button')
    await btn.click() // neutral → 必须上场
    await btn.click() // 必须上场 → 排除
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
