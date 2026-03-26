## Context

对手分析页面有两种模式：最佳三阵（Mode A，对比己方保存排阵与对手保存排阵）和逐线对比（Mode B，指定己方与对手各一个排阵）。

当前 `OpponentAnalysisService.computeLineAnalysis` 计算胜率时，己方用实际 UTR（actualUtr），但对手用的是 `opponentUtrByPosition` 映射——该映射是从 `Pair.combinedUtr`（官方 UTR 求和）构建的。这导致 delta 和胜率混用两套标准，预测结果失真。

AI 逐线评析（`MatchupCommentaryService`）目前不对排阵做 enrichment，也不传递搭档笔记，评析质量受限。

## Goals / Non-Goals

**Goals:**
- 所有胜率计算（`winProbability`）统一使用双方实际 UTR（若无实际 UTR 则回退到官方 UTR）
- 前端逐线卡片同时展示官方 UTR 与实际 UTR
- 对手排阵选择器预览展示每组 UTR + 实际 UTR
- AI prompt（推荐排阵、逐线评析）包含对手实际 UTR、球员 notes 和搭档笔记
- 逐线评析 enrichment 注入当前球员实际 UTR

**Non-Goals:**
- 不修改 UTR 数值本身的存储结构
- 不改变胜率映射函数（delta 阈值保持不变）
- 不引入新的 API 端点

## Decisions

### D1：在 `LineAnalysis` 中新增原始 UTR 字段，保留实际 UTR 字段

**决定**：`LineAnalysis` 新增 `ownCombinedRegularUtr` 和 `opponentCombinedActualUtr`，原有 `ownCombinedUtr` 继续承载实际 UTR（已是此行为），`opponentCombinedUtr` 继续承载对手官方 UTR。

**理由**：最小侵入性。前端需要同时展示官方 UTR 和实际 UTR，通过新增字段而非重命名已有字段可避免大规模 UI 重构和 API 破坏性变更。

**替代方案**：重命名 `ownCombinedUtr` → `ownActualUtr` 并新增 `ownRegularUtr`，但会破坏所有已有前端引用。

---

### D2：`computeLineAnalysis` 接收对手实际 UTR 映射作为第三个参数

**决定**：签名改为 `computeLineAnalysis(Lineup candidate, Map<String,Double> opponentUtrByPosition, Map<String,Double> opponentActualUtrByPosition)`。调用方构建两个映射后传入。

**理由**：`OpponentAnalysisService` 被 `LineupMatchupService` 共用，两处调用均需注入对手实际 UTR。通过参数传入比在方法内部查询数据更解耦，也便于测试。

---

### D3：`MatchupCommentaryService` 主动 enrich 排阵并 fetch 搭档笔记

**决定**：在 `MatchupCommentaryService.getCommentary` 中，先从 repository 加载双方当前球员数据，对排阵中的 pair 注入最新 actualUtr；同时通过 repository 读取 partnerNotes 并传入 `ZhipuAiService.getCommentary`。

**理由**：保存排阵时快照的 UTR 可能已过期，评析应基于当前数据。搭档笔记是逐线评析的重要上下文。

**替代方案**：让前端在 request body 中传递 partner notes——增加了前端复杂度，且前端已在 `runBestThreeAi` 路径下做了此操作，commentary 路径也可复用此模式。但 service 层自行 fetch 更简洁，无需变更 `useOpponentMatchup.js` 的 API 边界（除传递 teamId 已有）。

---

### D4：`Pair` 新增 `combinedActualUtr` 字段

**决定**：在 `enrichLineup`（`LineupMatchupService`）和 `findAndEnrichOpponentLineup`（`OpponentAnalysisService`）中计算并设置 `combinedActualUtr = actualUtr1 + actualUtr2`（回退到 utr）。

**理由**：便于前端直接读取，也便于构建 `opponentActualUtrByPosition` 映射。

## Risks / Trade-offs

- [兼容性] `LineAnalysis` 新增字段是纯加法，不破坏现有 API 消费方 → 低风险，新字段可选读
- [enrichment] `MatchupCommentaryService` 需额外读取球员和 partnerNotes 数据，轻微增加 I/O → 可接受，JSON 文件读取延迟极低
- [测试覆盖] `computeLineAnalysis` 签名变更会导致现有单元测试编译失败 → 需同步更新测试，在 tasks 中明确列出
- [前端展示空间] 同时展示 UTR + 实际 UTR 会使逐线行更宽 → 实际 UTR 仅在与官方 UTR 不同时显示，保持界面简洁

## Migration Plan

1. 后端变更（model → service → controller）→ 重启后端
2. 前端变更（composable → view）→ 热更新生效
3. 无数据迁移需求，JSON 文件格式不变

## Open Questions

无。
