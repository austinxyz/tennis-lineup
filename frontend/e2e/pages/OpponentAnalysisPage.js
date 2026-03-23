export class OpponentAnalysisPage {
  constructor(page) {
    this.page = page
  }

  async goto() {
    await this.page.goto('/opponent-analysis')
    await this.page.waitForLoadState('networkidle')
  }

  async selectMode(mode) {
    // mode: 'bestThree' | 'headToHead'
    const label = mode === 'bestThree' ? '最佳三阵' : '逐线对比'
    await this.page.getByRole('button', { name: label, exact: true }).click()
  }

  async selectOwnTeam(teamName) {
    await this.page.locator('label:has-text("己方队伍") + select').selectOption({ label: teamName })
  }

  async selectOpponentTeam(teamName) {
    await this.page.locator('label:has-text("对手队伍") + select').selectOption({ label: teamName })
    await this.page.waitForLoadState('networkidle')
  }

  async selectOpponentLineup(label) {
    await this.page.locator('label:has-text("对手排阵") + select').selectOption({ label })
  }

  async selectOwnLineup(label) {
    await this.page.locator('label:has-text("己方排阵") + select').selectOption({ label })
  }

  async clickFindBestThree() {
    await this.page.getByRole('button', { name: '查找最佳三阵' }).click()
  }

  async clickHeadToHeadAnalysis() {
    await this.page.getByRole('button', { name: '对比分析' }).click()
  }

  async waitForResults() {
    // Wait for per-line comparison to appear
    await this.page.locator('text=D1').first().waitFor({ timeout: 10000 })
  }

  async getResultVerdicts() {
    const badges = this.page.locator('.rounded-full').filter({
      hasText: /能赢|势均力敌|劣势/,
    })
    return badges.allTextContents()
  }
}
