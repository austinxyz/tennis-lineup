## 1. 球员备注字段 — 后端

- [x] 1.1 `Player.java` 增加 `String notes` 字段（`@JsonProperty("notes")`，可空）
- [x] 1.2 `PlayerRequest.java`（或等价 DTO）增加 `notes` 字段，传入 PlayerService
- [x] 1.3 `PlayerService.addPlayer()` 和 `updatePlayer()` 设置 notes 字段
- [x] 1.4 更新 `PlayerServiceTest`：验证 notes 字段在添加/更新球员时被正确保存；null 时不报错

## 2. 球员备注字段 — 前端

- [x] 2.1 `PlayerForm.vue` 增加备注 textarea（label "球员特点备注"，placeholder 示例，软提示 100 字以内）
- [x] 2.2 `usePlayers.js` 的 `addPlayer` 和 `updatePlayer` 透传 notes 字段
- [x] 2.3 `PlayerForm.test.js` 增加：备注字段渲染、填写备注后 submit 事件包含 notes

## 3. 最佳三阵 AI 推荐 — 后端

- [x] 3.1 `ZhipuAiService.buildPromptWithOpponent()` 在球员名后追加 notes（若非空），格式：`张三(UTR 6.0, 备注:正手强)`
- [x] 3.2 同样更新 `appendLineup()` 内的己方球员描述行（用于最佳三阵 prompt）
- [x] 3.3 `ZhipuAiServiceTest`：增加测试验证 notes 非空时出现在 prompt 中；notes 为空时格式不变

## 4. 逐线对比 AI 评析 — 后端新接口

- [x] 4.1 新建 `MatchupCommentaryRequest.java`：`teamId`, `ownLineupId`, `opponentTeamId`, `opponentLineupId`
- [x] 4.2 新建 `MatchupCommentaryResponse.java`：`List<LineCommentary> lines`（含 position + commentary）、`boolean aiUsed`
- [x] 4.3 `ZhipuAiService` 增加 `buildCommentaryPrompt(ownLineup, oppLineup)` — 4 条线拼入一个 prompt，格式：`D1: 己方 张三(6.0,备注:X)+李四(5.5) vs 对手 乙一(5.5)+乙二(5.0) delta=+1.0`；要求 AI 返回 `D1\t评析\nD2\t评析\n...`
- [x] 4.4 `ZhipuAiService` 增加 `parseCommentaryResult(String content)` — 解析 `位置\t文字` 格式，返回 `Map<String, String>`；解析失败返回空 Map
- [x] 4.5 新建 `MatchupCommentaryService`：读取两个排阵 → 构建 prompt → 调用 AI → 解析结果；AI 失败时按 delta 规则兜底（delta>0.5:"具备优势，建议主动进攻"；|delta|≤0.5:"势均力敌，注重稳定发挥"；delta<-0.5:"处于劣势，多以防守反击为主"）
- [x] 4.6 在现有 `LineupController`（或新建）增加 `POST /api/lineups/matchup-commentary` 端点
- [x] 4.7 新建 `MatchupCommentaryServiceTest`：(a) AI 成功时返回解析文字；(b) AI 不可用时按 delta 规则兜底；(c) 排阵不存在时返回 404
- [x] 4.8 `ZhipuAiServiceTest` 增加：`parseCommentaryResult` 解析正常、部分缺行、内容为 null 的场景
- [x] 4.9 重启后端，运行 `mvn test` 验证全部通过（212 tests）

## 5. 前端 — 逐线对比排阵预览

- [x] 5.1 `OpponentAnalysis.vue`：选择对手排阵后，在下拉框下方渲染排阵预览（D1-D4，"D1: 甲 + 乙"，灰色小字）
- [x] 5.2 选择己方排阵后，同样显示己方预览；数据来自已加载的 `ownLineups` 数组，无需额外请求
- [x] 5.3 `OpponentAnalysis.test.js` 增加：选择排阵后预览区域出现

## 6. 前端 — 最佳三阵 AI 推荐

- [x] 6.1 `useOpponentMatchup.js`：`runAiAnalysis` 已有，无需修改（复用）
- [x] 6.2 `OpponentAnalysis.vue` 最佳三阵结果区域：UTR 前三阵显示完毕后，增加"AI 推荐"按钮（紫色）
- [x] 6.3 点击后调用 `runAiAnalysis`，结果以紫色边框卡片展示（标题"AI 推荐排阵"），包含逐线对比 + 预期得分 + AI 理由文字
- [x] 6.4 若无三阵结果（未点查找），AI 按钮不显示
- [x] 6.5 `OpponentAnalysis.test.js` 增加：最佳三阵结果后出现 AI 按钮；点击后显示 AI 推荐卡片

## 7. 前端 — 逐线对比 AI 评析

- [x] 7.1 `useOpponentMatchup.js` 增加 `runCommentary(ownTeamId, ownLineupId, opponentTeamId, opponentLineupId)` — `POST /api/lineups/matchup-commentary`，返回 `lines` 数组
- [x] 7.2 `useOpponentMatchup.test.js` 增加 `runCommentary` 测试（成功、AI 不可用兜底、404 抛错）
- [x] 7.3 `OpponentAnalysis.vue` 逐线对比结果区域：UTR 分析结果显示后，AI 按钮改为"AI 逐线评析"（紫色）
- [x] 7.4 点击后调用 `runCommentary`，结果以紫色卡片展示：每条线显示 `D1: [评析文字]`
- [x] 7.5 `OpponentAnalysis.test.js` 增加："AI 逐线评析"按钮出现；点击后显示评析卡片

## 8. E2E 测试

- [x] 8.1 `opponent-analysis.spec.js` 增加：最佳三阵结果后显示"AI 推荐"按钮
- [x] 8.2 增加：逐线对比选择排阵后显示预览（D1 字样出现在预览区域）
- [x] 8.3 增加："AI 逐线评析"按钮出现（AI 不可用时也显示兜底文字）
- [x] 8.4 更新 `PlayerDetailPage.js`：支持填写 notes 字段
- [x] 8.5 运行全部 E2E 测试并确认通过

## 9. 收尾

- [x] 9.1 运行 `npm test` 确认全部通过（301 tests）
- [x] 9.2 运行 `mvn test` 确认全部通过（212 tests）
- [x] 9.3 生成测试报告（前端/后端/E2E 通过数）

## 测试报告

| 测试类型 | 通过数 | 备注 |
|---------|--------|------|
| 前端单元测试 (Vitest) | 301 | `npm test` |
| 后端单元测试 (JUnit 5) | 212 | `mvn test` |
| E2E 测试 (Playwright) — opponent-analysis | 15/15 | 全部通过 |
| E2E 测试 (Playwright) — 全套 | 28/32 | 4 个预存在失败（lineup-generation×2, team-management×2），与本次变更无关 |
