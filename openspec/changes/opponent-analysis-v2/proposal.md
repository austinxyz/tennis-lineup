## Why

对手分析页面目前的 AI 功能设计有两个问题：逐线对比的 AI 只是换推荐排阵而非分析对比；最佳三阵没有 AI 推荐。同时，AI 分析缺乏球员个人特点信息，给出的建议流于 UTR 数字对比，无法体现战术价值。

## What Changes

- **球员备注字段**：Player 模型增加 `notes` 字段（可选，自由文字），在球员详情页可编辑；排阵分析时 AI 可读取备注辅助决策
- **最佳三阵 AI 推荐**：在最佳三阵结果下方增加"AI 推荐"按钮，AI 从己方已保存排阵中综合 UTR 和球员特点备注，挑出最佳一阵并给出推荐理由
- **逐线对比排阵预览**：选择己方/对手排阵后立即展示各线球员名单（D1-D4 组合人员），让用户确认内容再点"对比分析"
- **逐线对比 AI 评析**：针对已选定的两个排阵，AI 对每条线给出战术评析（结合球员 UTR 差值和备注特点），不再推荐新排阵；后端新增专用接口 `POST /api/lineups/matchup-commentary`

## Capabilities

### New Capabilities
- `player-notes`: Player 增加 notes 字段，支持增删改查，在球员表单中展示和编辑
- `lineup-matchup-commentary`: 对两个指定排阵做 AI 逐线战术评析，返回每条线的分析文字

### Modified Capabilities
- `opponent-analysis`: 最佳三阵增加 AI 推荐入口；逐线对比增加排阵预览和 AI 评析入口
- `saved-lineup-matchup`: AI 推荐时将球员 notes 拼入 AI prompt

## Impact

- **后端**：`Player.java` 增加 `notes` 字段；新增 `MatchupCommentaryRequest/Response`；新增 `LineupMatchupCommentaryController`（或在现有 controller 增加端点）；`ZhipuAiService` 新增 `buildCommentaryPrompt()`
- **前端**：`PlayerForm.vue` 增加备注输入框；`OpponentAnalysis.vue` 增加排阵预览区域和逐线 AI 评析展示；`useOpponentMatchup.js` 增加 `runCommentary()` 函数
- **数据**：Player JSON 结构增加 `notes`（可选，已有数据无需迁移，读取时默认 null）
