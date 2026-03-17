## Context

3.1 组队管理已完整实现（Team/Player CRUD、批量导入、JSON 存储），测试覆盖率高。目前 Team 数据模型中 `lineups` 字段存在于设计文档但后端尚未实现，JSON 文件中不含排阵数据。

本次需要在现有架构上叠加排阵生成能力：后端新增服务层逻辑（排列组合算法 + 硬约束验证 + AI 选优）、新增 API 端点、扩展数据模型；前端新增排阵生成页面和组件。

## Goals / Non-Goals

**Goals:**
- 实现 `POST /api/lineups/generate`：输入 teamId + 策略，输出满足所有硬约束的排阵
- 实现排阵历史 CRUD：保存到队伍、查询、删除
- 支持预设策略（均衡/集中火力）和自定义自然语言策略（AI 解析）
- 前端排阵生成页面：队伍选择 → 策略配置 → 一键生成 → 结果卡片展示
- AI 调用失败时有确定性 Fallback，保证功能可用

**Non-Goals:**
- 对手策略分析（3.3，独立 change）
- 排阵历史可视化统计（3.5）
- 移动端适配优化

## Decisions

### 决策 1：排列组合算法 — 回溯 vs 全量枚举

**选择**：回溯剪枝算法

**理由**：8 名球员的全量配对约 10,000 组合，回溯可在生成阶段通过"搭档 UTR 差 ≤ 3.5"剪枝，减少无效组合。实测复杂度 O((8! / (2^4 × 4!)) ≈ 105 基础组合，剪枝后更少，远低于 5 秒限制。

**替代方案**：Stream 全量生成后过滤 — 可行但浪费内存；穷举后随机抽样 — 不保证最优解。

---

### 决策 2：AI 集成方式 — 让 AI 选择 vs 让 AI 生成

**选择**：后端先枚举所有合法排阵，再调用 AI 从合法排阵集合中选出最优解（编号选择）

**理由**：
- 保证 AI 返回的排阵一定满足硬约束（约束由 Java 代码保证，不信任 AI 生成的数据）
- Prompt 更简单：输入候选列表 + 策略，输出一个整数编号
- Fallback 更容易：AI 失败时直接用启发式在同一合法集合中选

**替代方案**：让 AI 直接生成 JSON 排阵 — 无法保证约束合规，解析复杂，易幻觉。

---

### 决策 3：AI Fallback 策略

当 AI 超时（> 3s）或返回无法解析的响应时，降级为确定性启发式：

| 预设策略 | Fallback 算法 |
|---------|--------------|
| `balanced`（均衡）| 选各线 UTR 方差最小的排阵 |
| `aggressive`（集中火力）| 选 D1+D2+D3 UTR 总和最大的排阵 |
| `custom`（自然语言）| 降级为 `balanced` |

**理由**：自然语言策略无法在无 AI 时完整实现，退化到均衡是最安全的选择，且对用户有明确提示。

---

### 决策 4：排阵数据存储位置 — 嵌套在 Team vs 独立集合

**选择**：排阵嵌套存储在 Team.lineups[] 中（与设计文档一致）

**理由**：现有 `JsonRepository` 读写整个 `TeamData` 对象，嵌套结构无需修改存储层架构，只需扩展模型类。查询排阵历史时读取单个队伍即可。

**替代方案**：独立 `lineups` 顶层集合 — 需要跨集合关联查询，复杂度高，不必要。

---

### 决策 5：前端状态管理 — composable vs store

**选择**：新增 `useLineup.js` composable，与现有 `useTeams.js` 风格保持一致

**理由**：本功能无全局共享状态需求，composable 足够，无需引入 Pinia。

## Risks / Trade-offs

| 风险 | 缓解措施 |
|------|---------|
| 智谱 AI API Key 未配置时生成失败 | Fallback 降级到启发式，返回 `aiUsed: false` 标志给前端展示 |
| 球员数 > 8 时候选排阵爆炸式增长（12人→ ~10,000+） | 生成阶段保持剪枝；文档说明当前优化针对 8-12 人场景 |
| `Instant` 序列化格式不一致 | 明确配置 Jackson `WRITE_DATES_AS_TIMESTAMPS=false`，保持 ISO 8601 |
| 并发写排阵时数据竞争 | 复用现有 `JsonRepository` 的 `ReadWriteLock`，无需额外锁 |
| AI 返回编号超出范围 | 后端 `parseLineupResponse` 捕获 `IndexOutOfBoundsException`，触发 Fallback |

## Migration Plan

1. 扩展后端数据模型（`Lineup`、`Pair` model 类，无破坏性变更）
2. 现有 `tennis-data.json` 无 `lineups` 字段 → Jackson `@JsonProperty` 设默认空数组，读取时自动兼容
3. 新增 `LineupController` 和服务层，不修改现有 `TeamController`
4. 前端新增路由 `/lineup` 和侧边栏入口，不改动现有路由

无需数据迁移脚本。旧数据文件加载后 `lineups` 默认为空数组，完全向后兼容。

## Open Questions

- **智谱 AI 模型版本**：设计文档指定 `glm-4`，是否使用更新的 `glm-4-flash`（更快更便宜）？实现时如无特殊要求，默认使用 `glm-4`。
- **排阵数量上限**：历史记录是否需要限制条数（如最多保留 10 条/队伍）？当前不做限制，后续迭代按需加。
