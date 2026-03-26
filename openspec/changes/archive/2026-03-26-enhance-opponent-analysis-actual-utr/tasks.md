## 1. Backend Model Changes

- [x] 1.1 在 `Pair.java` 中新增 `combinedActualUtr` 字段（`@JsonProperty("combinedActualUtr") private Double combinedActualUtr`）
- [x] 1.2 在 `LineAnalysis.java` 中新增 `ownCombinedRegularUtr` 和 `opponentCombinedActualUtr` 字段（Double 类型，可为 null）

## 2. Backend Service — Enrichment & UTR Mapping

- [x] 2.1 在 `LineupMatchupService.enrichLineup()` 中计算并设置 `pair.combinedActualUtr = actualUtr1 + actualUtr2`（无 actualUtr 时回退到 utr）
- [x] 2.2 在 `OpponentAnalysisService.findAndEnrichOpponentLineup()` 中计算并设置 `pair.combinedActualUtr`（同上逻辑）
- [x] 2.3 在 `LineupMatchupService.matchup()` 中构建 `opponentActualUtrByPosition` 映射（从 `pair.combinedActualUtr`）

## 3. Backend Service — Win Probability with Actual UTR

- [x] 3.1 更新 `OpponentAnalysisService.computeLineAnalysis` 签名，新增第三个参数 `Map<String, Double> opponentActualUtrByPosition`
- [x] 3.2 在 `computeLineAnalysis` 中：计算 `ownRegularUtr = p1Utr + p2Utr`，从 `opponentActualUtrByPosition` 取 `oppActualUtr`，以 `ownActualUtr - oppActualUtr` 计算 delta 和 winProbability，并填充 `ownCombinedRegularUtr` 和 `opponentCombinedActualUtr` 字段
- [x] 3.3 在 `OpponentAnalysisService.computeUtrRecommendation()` 中构建 `opponentActualUtrByPosition` 映射并传入更新后的 `computeLineAnalysis`
- [x] 3.4 在 `LineupMatchupService.matchup()` 中将 `opponentActualUtrByPosition` 传入 `opponentAnalysisService.computeLineAnalysis` 调用（包括 for 循环和 AI 推荐两处调用）
- [x] 3.5 在 `OpponentAnalysisService.computeAiRecommendation()` 中更新 `computeLineAnalysis` 调用，传入对手实际 UTR 映射

## 4. Backend Service — AI Prompt Enhancement

- [x] 4.1 在 `ZhipuAiService.buildPromptWithOpponent()` 对手排阵部分，将 `describePlayer(name, utr, notes)` 改为 `describePlayer(name, utr, actualUtr, notes)` 调用，以显示实际 UTR
- [x] 4.2 在 `ZhipuAiService.buildCommentaryPrompt()` 中，将 line delta 改为 `ownActualCombinedUtr - oppActualCombinedUtr`（使用 `combinedActualUtr`，回退到 `combinedUtr`）
- [x] 4.3 在 `ZhipuAiService` 中新增 `getCommentary(Lineup, Lineup, List<PartnerNoteDto>, List<PartnerNoteDto>)` 重载，在 `buildCommentaryPrompt` 内调用 `appendPartnerNotesSection`

## 5. Backend Service — Commentary Enrichment & Partner Notes

- [x] 5.1 在 `MatchupCommentaryRequest.java` 中新增可选字段 `ownPartnerNotes` 和 `opponentPartnerNotes`（`List<LineupMatchupRequest.PartnerNoteDto>`）
- [x] 5.2 在 `MatchupCommentaryService.getCommentary()` 中：从 repository 读取双方球员数据，对排阵 pair 注入当前 `actualUtr` 和 `combinedActualUtr`
- [x] 5.3 在 `MatchupCommentaryService.getCommentary()` 中：若 request 中无 partnerNotes 则从 repository 读取 partnerNotes，调用 `aiService.getCommentary(...)` 带笔记重载

## 6. Frontend — Opponent Lineup Preview Dual UTR

- [x] 6.1 在 `OpponentAnalysis.vue` 中更新对手排阵预览（`opponentLineupPreviewPairs`），展示每组的 UTR + 实际 UTR（格式：`D1: 张三(8.0/实7.5) + 李四(7.5)`，实际 UTR 仅在不同时显示）

## 7. Frontend — Line Analysis Dual UTR Display

- [x] 7.1 在 `OpponentAnalysis.vue` 中更新 `pairText()` 函数，对每位球员同时展示官方 UTR 和实际 UTR（实际 UTR 仅在与官方不同时显示）
- [x] 7.2 在 UTR 最佳三阵逐线卡片中，在 `ownCombinedUtr` 旁额外展示 `line.ownCombinedRegularUtr`（当两者不同时），格式 `UTR x.x / 实 y.y`
- [x] 7.3 在逐线对比（逐线对比 Mode B）卡片中，同样展示对手 `opponentCombinedUtr`（官方）和 `line.opponentCombinedActualUtr`（实际，当不同时）

## 8. Frontend — Commentary Partner Notes

- [x] 8.1 在 `useOpponentMatchup.js` 的 `runCommentary` 中，fetch 双方 partnerNotes 并在 `POST /api/lineups/matchup-commentary` body 中传递 `ownPartnerNotes` 和 `opponentPartnerNotes`

## 9. Restart & Verification

- [ ] 9.1 重启后端（kill 现有进程并运行 `JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 mvn spring-boot:run`）
- [x] 9.2 运行后端单元测试并修复因 `computeLineAnalysis` 签名变更导致的编译错误（`OpponentAnalysisServiceTest`、`LineupMatchupServiceTest` 等）
- [x] 9.3 运行前端单元测试（`npm test`），确认全部通过
- [x] 9.4 运行 E2E 测试（`npm run test:e2e`），确认对手分析流程正常

## 10. Test Report

- [x] 10.1 生成测试报告，记录后端测试通过数量（含新用例）和 E2E 测试结果
