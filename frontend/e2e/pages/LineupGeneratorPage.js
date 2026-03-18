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

  async waitForLineupCard() {
    await this.page.locator('text=排阵结果').waitFor({ timeout: 10000 })
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
