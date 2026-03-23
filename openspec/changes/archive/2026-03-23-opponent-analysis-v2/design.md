## Context

对手分析页面已完成"最佳三阵"和"逐线对比"两种模式的基础功能（UTR 概率分析）。现有 AI 功能仅用于从所有保存排阵中选出最佳一阵，不支持对指定排阵对比做解说。球员模型目前只有 UTR 数字，没有描述性特点字段。

## Goals / Non-Goals

**Goals:**
- 球员增加 `notes` 备注字段（自由文字，可空），前端表单可编辑
- 最佳三阵增加 AI 推荐按钮，AI 综合 UTR + 球员 notes 给出最佳排阵及理由
- 逐线对比选择排阵后立即预览该排阵的 D1-D4 球员组合
- 逐线对比增加 AI 评析：对已选定的两个排阵逐线给出战术文字（D1-D4 各一条，不推荐新排阵）
- AI 评析结合球员 notes（若有）提供更有战术价值的分析

**Non-Goals:**
- 不改变最佳三阵的 UTR 排序逻辑
- 不改变逐线对比的 UTR 概率计算
- 不支持批量修改球员备注
- AI 不自动分析所有历史排阵

## Decisions

### 1. 球员 notes 字段
- `Player.java` 增加 `String notes`（可空）
- `PlayerRequest` 增加对应字段（无长度限制，前端用 textarea）
- 已有数据无需迁移，Jackson 读取时默认 null

### 2. 最佳三阵 AI 推荐
- 复用现有 `POST /api/lineups/matchup` + `includeAi: true`（不加 `ownLineupId`）
- `ZhipuAiService.buildPromptWithOpponent()` 在球员信息行后追加 notes（若非空）
- 响应中 `aiRecommendation` 含 lineup + explanation，前端高亮显示为"AI 推荐"卡片，排在 UTR 前三阵前面或用不同颜色区分

### 3. 逐线对比排阵预览
- 选好排阵后，前端从已加载的 `ownLineups` / `opponentLineups` 数组中找到对应对象，直接渲染预览（无需额外 API 请求）
- 预览格式：每行 `D1: 张三 + 李四`，简洁紧凑

### 4. 逐线对比 AI 评析 — 新接口
- 新端点：`POST /api/lineups/matchup-commentary`
  - 请求体：`{ teamId, ownLineupId, opponentTeamId, opponentLineupId }`
  - 响应体：`{ lines: [{ position, commentary }] }`（D1-D4 各一条评析文字）
- 不复用 matchup 接口，因为逻辑完全不同（不选排阵，只分析对比）
- AI Prompt 模板（每条线）：
  ```
  D1: 己方 张三(UTR 6.0, 备注:正手强) + 李四(UTR 5.5) vs 对手 乙一(UTR 5.5) + 乙二(UTR 5.0)
  Delta=+1.0，胜率80%。请用一句话给出战术建议。
  ```
- AI 返回格式：`D1\t评析文字\nD2\t评析文字\n...`（tab 分隔，换行分线）
- 若 AI 不可用，返回基于 delta 的规则文本替代：delta>0.5 → "具备优势，建议主动进攻"；delta≈0 → "势均力敌，注重稳定发挥"；delta<-0.5 → "处于劣势，多以防守反击为主"

### 5. 前端组织
- `useOpponentMatchup.js` 增加 `runCommentary(ownTeamId, ownLineupId, opponentTeamId, opponentLineupId)` → 返回 `{ lines }`
- 逐线对比页面：UTR 分析结果卡片不变；AI 评析显示为独立卡片，每线一行，紧跟在对应位置名称旁

## Risks / Trade-offs

- **AI 评析长度**：4 条线同时请求一次 AI，prompt 较长但 token 控制在 500 以内，仍用 glm-4-air（非 reasoning）
- **notes 显示**：AI prompt 中 notes 过长会占 token，前端建议限制输入 100 字以内（软限制，提示用户）
- **排阵预览数据**：利用前端已有 lineup 对象，无需新 API，但若排阵数据未含球员 gender/notes，需确认 lineup 保存时包含完整 player 字段快照
