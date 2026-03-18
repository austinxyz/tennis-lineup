## Context

排阵生成已实现（lineup-fixes-v3），但存在三个问题：
1. 算法对 40.5 上限利用不足——子集排序时对"接近上限"和"恰好2女"同等优先，导致部分高 UTR 子集被推后
2. 历史排阵 JSON 中 Pair 对象缺少 `player1Utr`/`player2Utr`/`player1Gender`/`player2Gender` 字段（lineup-fixes-v3 前保存的），显示时无法读取
3. 自动持久化设计不符合用户工作流：用户需要先浏览多个候选方案，再选择性保留

## Goals / Non-Goals

**Goals:**
- 算法优先返回总 UTR 最接近 40.5（不超过）的候选排阵
- top-20/top-40 分批策略：先试前 20 个子集，若有效排阵 < 6 则继续试 21-40
- 历史排阵加载时用当前球员数据补全缺失字段（无损兼容）
- 生成结果不再自动保存；用户手动点击"保留"才持久化
- 前端生成结果卡片提供独立"保留"按钮，保存选中排阵

**Non-Goals:**
- 不修改排阵历史页布局（已有 lineup history 浏览/删除功能）
- 不修改约束规则本身（UTR gap、女性最少、D4 verified 等均保持不变）
- 不支持批量保留（每次只能逐个保留）

## Decisions

**1. 子集排序只按 UTR 接近上限，不内嵌女性偏好**

当前代码在子集排序时同时考虑"恰好2女"优先，导致高 UTR 子集可能被3女子集压后。新排序改为：
- cap-valid（≤ 40.5）first
- 同组内：总 UTR 降序（最接近 40.5 的排最前）

女性约束（≥ 2 女）仍由 `ConstraintService.validateLineup` 在 backtracking 阶段检查，而非子集筛选阶段。这样更高 UTR 的子集即使女性数量 > 2 也会排在前面，只要能组成满足约束的配对。

**2. top-20 → top-40 分批而非一次处理全部**

原来处理所有子集直到 100 个候选，开销大且不保证"最优先"子集被充分探索。新策略：先跑 top-20，过滤后若 < 6 结果再跑 21-40。C(20,8)=125970，只处理 40 个子集大幅降低无效计算。

**3. 历史排阵 enrichment 在读取层做（不修改存储文件）**

在 `getLineupsByTeam` 读取后、返回前，对每个 Pair 检查字段是否为 null，从 team.players 中查找对应球员并填充。不回写文件，避免无意义写入和并发锁争用。

**4. 去掉自动保存，新增手动保存接口**

`generateMultipleAndSave` 重命名/拆分为：
- `generateCandidates(...)` 纯生成，不持久化
- `saveLineup(teamId, lineup)` 手动保存单个排阵

后端新增 `POST /api/teams/{teamId}/lineups` 接受完整 Lineup 对象并持久化。前端每个排阵卡片有"保留"按钮，点击后调用此接口。

**5. 前端 saveLineup 结果反馈**

保留成功后按钮变为"已保留 ✓"并禁用，防止重复保存。失败时显示内联错误提示。

## Risks / Trade-offs

- **同一排阵重复保留** → 由于排阵 id 每次生成时由 nanoTime 赋予，重复点击会重复保存。通过保留成功后禁用按钮缓解；不做后端去重（简单场景下可接受）
- **top-40 仍不足 6 个** → 正常返回能找到的所有结果（可能 < 6），由 LineupService 取 min(results, 6) 展示
- **老 lineups 中 player 已被删除** → enrichment 时查不到对应球员，字段保持 null，前端用"?"显示

## Migration Plan

1. 部署新后端：`generateMultipleAndSave` 不再保存，新增 `POST /api/teams/{id}/lineups` 端点
2. 已存在的历史排阵不受影响（只读时补全字段）
3. 前端同步部署：生成结果不再依赖自动保存，"保留"按钮调新端点
4. 回滚：恢复旧 `generateMultipleAndSave`（自动保存）+ 移除"保留"按钮

## Open Questions

- （已决策）历史排阵里 totalUtr 字段：老数据已有，无需 enrichment
