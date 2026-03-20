## Why

当前排阵算法在球员超过8人时暴力枚举所有8人子集，没有利用业务先验信息（谁一定上、谁一定不上），导致无效计算过多。通过先过滤确定不上的、优先纳入确定上的，再按接近40.5排序补充剩余名额，并用top20/top40截断方式提前过滤约束，可以大幅降低枚举空间并提升结果质量。

## What Changes

- **修改8人候选池构建逻辑**：不再枚举所有C(n,8)子集，改为按优先级分层构建候选池
  1. 从完整名单中移除 `excludePlayers`（确定不上）
  2. 将 `includePlayers`（含 `pinPlayers` 固定位置球员）作为"一定上"加入8人队列
  3. 剩余球员按接近40.5总UTR排序（贪心补充），依次填入队列直到满8人
- **引入top20/top40截断过滤**：在完整约束验证前，先用top20和top40预筛候选集，减少无效的完整约束计算
- **保留所有已有Hard Constraint逻辑**：UTR排序、40.5上限、2名女性、搭档UTR差≤3.5、D4 verified等约束不变

## Capabilities

### New Capabilities

- `lineup-queue-builder`: 分层构建8人候选队列——先排除excluded，再纳入included/pinned，最后按接近40.5贪心补充剩余位置，并用top20/top40截断预过滤约束

### Modified Capabilities

- `lineup-generation`: 算法核心变更——8人子集选择从全枚举改为优先级分层队列构建 + top截断预过滤

## Impact

- **Backend**: `LineupGenerationService`（或等价服务类）中负责构建8人候选集的逻辑，以及排列枚举入口
- **测试**: 需更新或新增对应算法逻辑的后端单元测试
- **无API变更**: 入参/出参结构、HTTP端点不变
- **无前端变更**
