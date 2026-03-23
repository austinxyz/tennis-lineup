## ADDED Requirements

### Matchup Commentary Endpoint
- 新端点：`POST /api/lineups/matchup-commentary`
- 请求体：
  ```json
  {
    "teamId": "string",
    "ownLineupId": "string",
    "opponentTeamId": "string",
    "opponentLineupId": "string"
  }
  ```
- 响应体：
  ```json
  {
    "lines": [
      { "position": "D1", "commentary": "string" },
      { "position": "D2", "commentary": "string" },
      { "position": "D3", "commentary": "string" },
      { "position": "D4", "commentary": "string" }
    ],
    "aiUsed": true
  }
  ```
- 服务行为：
  - 找到己方和对手的指定排阵（不存在则 404）
  - 对 D1-D4 每条线，结合 UTR delta、球员 notes（若有）构建 AI prompt
  - 调用 ZhipuAiService 获取逐线评析文字
  - AI 不可用时，根据 delta 返回规则文本：delta > 0.5 → "具备优势，建议主动进攻"；|delta| ≤ 0.5 → "势均力敌，注重稳定发挥"；delta < -0.5 → "处于劣势，多以防守反击为主"
  - 响应中 `aiUsed: false` 表示使用了规则兜底
- AI Prompt 要求：一次请求包含所有 4 条线，格式参见 design.md，返回 4 行 `位置\t评析`，限制 token ≤ 500
