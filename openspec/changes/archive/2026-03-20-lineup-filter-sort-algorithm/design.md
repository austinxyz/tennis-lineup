## Context

当前排阵算法在候选球员超过8人时通过枚举所有 C(n,8) 子集来寻找最优8人组合，再对每个子集做全配对回溯。当名单有12-15人时，子集数量迅速膨胀（C(12,8)=495, C(15,8)=6435），且大量子集在 total UTR 层面就无法满足约束，造成不必要的计算。

业务上，每次生成排阵时用户实际上已知道：
- 哪些人**一定不上**（`excludePlayers`）
- 哪些人**一定上**（`includePlayers` / `pinPlayers`，含四线固定的D1/D2/D3/D4球员）
- 剩余的人中谁对凑近40.5上限贡献最大

利用这些先验信息，可以将枚举空间从 C(n,8) 缩减到接近1。

## Goals / Non-Goals

**Goals:**
- 将8人候选池构建从全子集枚举改为优先级分层选择，消除无效枚举
- 引入 top20/top40 截断，在完整约束验证前预过滤，减少无效的配对回溯
- 保持所有 Hard Constraint 逻辑不变（UTR顺序、40.5上限、2女、差≤3.5、D4 verified）
- 保持 API 接口完全不变（无破坏性变更）

**Non-Goals:**
- 不修改配对组合回溯算法本身（pair-level backtracking）
- 不修改 AI 策略选择逻辑
- 不引入任何前端改动
- 不修改 Hard Constraint 的定义或语义

## Decisions

### 决策1：分层队列构建算法

**选择**：三阶段贪心构建8人池

```
阶段1. 过滤层
  eligible = allPlayers - excludePlayers

阶段2. 锚定层（一定上）
  locked = includePlayers ∪ pinPlayers.keys()
  queue = locked  // 验证 locked.size <= 8

阶段3. 补充层（按接近40.5贪心）
  remaining = eligible - locked
  remaining 按 (40.5 - currentSum - player.utr) 绝对值升序排列
  （即：加入该球员后 totalUtr 最接近40.5的优先）
  while queue.size < 8 and remaining non-empty:
      queue.add(remaining.next())
```

**为何不继续全枚举**：锁定球员数量可能是0-8，剩余补充位置也可能是0-8，此时补充层只需贪心填满剩余槽，不需要枚举。这将 O(C(n,8)) 降到 O(n log n)。

**替代方案考虑**：多子集采样（随机采样k个子集）—— 拒绝理由：结果不确定，无法保证最优。

### 决策2：Top20/Top40 截断预过滤

**选择**：在对8人池做完整配对回溯前，先对`eligible`集合按个人UTR从高到低排序：
- **top40**：生成 pair 前先用 top40 名单（eligible前40人）作为配对候选池
- **top20**：作为更严格的截断，用于限制每个配对位置（D1/D2）的候选人

具体实现：
1. 从8人队列中生成所有满足"partner UTR差≤3.5"的候选对子（不超过 C(8,2)=28 对）
2. 所有对子按组合UTR降序排列，取top20作为 D1/D2 候选，top40（或全集）作为 D3/D4 候选
3. 在4位置分配时（D1→D4），每个位置从对应候选集中选择，而非全量回溯

**为何选择20/40**：
- 8人队列中最多C(8,2)=28个对子，top20覆盖约70%的高UTR对子，已足够找到最优解
- top40 ≈ 全集（C(8,2)=28 < 40），所以 D3/D4 候选不受限，保证完整性

### 决策3：locked球员验证提前

若 `locked.size > 8`，立即返回 400（`includePlayers` 与 `pinPlayers` 合并后超过8人）。提前失败比等到生成阶段报错更清晰。

## Risks / Trade-offs

- **[风险] 贪心可能错过最优子集**：当接近40.5的贪心组合恰好因female/D4-verified等约束失败时，可能存在次优UTR组合却满足所有约束。→ 缓解：补充层在贪心填满后，如回溯失败，可尝试替换最后一个补充球员（回退1步）。若仍失败，返回"无有效排阵"，与当前行为一致。
- **[权衡] top20/top40 定义依赖队列大小**：8人队列对子上限28 < 40，top40实际等于全集，这是有意的——确保 D3/D4 不丢失候选。

## Migration Plan

1. 在 `LineupService`（或等价服务）中新增 `buildCandidatePool()` 方法实现三阶段贪心
2. 在配对枚举入口处，将原有"枚举所有C(n,8)子集"逻辑替换为调用 `buildCandidatePool()`
3. 在配对对子生成后增加 top20/top40 截断步骤
4. 保持 `LineupController` 和请求/响应 DTO 完全不变
5. 无数据迁移，无配置变更
6. 回滚：feature flag 或直接 revert commit（纯算法变更，无状态影响）

## Open Questions

- top20/top40 的具体数字是否需要可配置（application.yml）？建议先硬编码，有性能数据后再决定。
- 贪心回退1步是否足够，还是需要回退多步？建议实现时先回退1步，测试覆盖边界case后决定。
