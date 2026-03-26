## Why

对手分析页面的概率计算和 AI 推荐目前混用了官方 UTR 与实际 UTR——己方用实际 UTR、对手用官方 UTR——导致胜率预测失真。同时，对手排阵选择器和逐线对比均未展示实际 UTR，AI 逐线评析也缺乏 notes 和搭档笔记上下文，影响分析质量。

## What Changes

- **最佳三阵**：对手排阵选择器预览新增每组 UTR + 实际 UTR 显示
- **UTR 最佳三阵**：逐线卡片同时展示己方和对手的 UTR 与实际 UTR；概率对比改为双方实际 UTR 对比
- **AI 推荐排阵**：对手排阵 prompt 中补充实际 UTR；AI 候选排阵按实际 UTR 排序；已有 notes 和搭档笔记上下文保持正确传递
- **逐线比较**：逐线卡片同时展示己方和对手的 UTR 与实际 UTR
- **逐线比较概率**：win probability 改为双方实际 UTR 对比
- **逐线比较 AI 评析**：enrichment 注入当前球员实际 UTR；评析 prompt 中加入实际 UTR delta；通过 API 传递搭档笔记上下文

## Capabilities

### New Capabilities

无新 capability。

### Modified Capabilities

- `opponent-analysis`: 扩展对手分析页面，使所有展示和计算均支持实际 UTR，并在 AI 分析中融入 notes 和搭档笔记

## Impact

- **Backend**
  - `Pair.java` — 新增 `combinedActualUtr` 字段
  - `LineAnalysis.java` — 新增 `ownCombinedRegularUtr`、`opponentCombinedActualUtr` 字段
  - `OpponentAnalysisService` — `computeLineAnalysis` 签名扩展，接受对手实际 UTR 映射；`computeUtrRecommendation` 构建实际 UTR 映射
  - `LineupMatchupService` — `enrichLineup` 设置 `combinedActualUtr`；`matchup` 构建对手实际 UTR 映射
  - `MatchupCommentaryService` — enrichment 注入实际 UTR；fetch + 传递搭档笔记
  - `ZhipuAiService` — `buildPromptWithOpponent` 展示对手实际 UTR；`buildCommentaryPrompt` 使用实际 UTR delta 并接受搭档笔记；`getCommentary` 新增搭档笔记重载
  - `MatchupCommentaryRequest` — 新增可选 `ownPartnerNotes` / `opponentPartnerNotes` 字段

- **Frontend**
  - `OpponentAnalysis.vue` — 对手排阵预览展示 UTR + 实际 UTR；逐线卡片双 UTR 展示
  - `useOpponentMatchup.js` — `runCommentary` fetch 并传递双方搭档笔记
