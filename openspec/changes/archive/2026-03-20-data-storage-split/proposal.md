## Why

随着功能迭代（lineup、constraint presets 等），`tennis-data.json` 将持续膨胀。单文件模式存在两个实际问题：①任何字段变更都需要读写整个文件，写锁期间所有操作阻塞；②调试和手动排查数据时文件过大、结构混杂。现在是在 constraint-presets 功能上线之前拆分的最佳时机，避免后续迁移成本更高。

## What Changes

将 `tennis-data.json` 拆分为多个文件，具体方案待定（见下方分析）。`JsonRepository` 需同时管理多个文件，对外接口（Service 层以上）不变。

---

## 方案对比

### 方案 A：按 Team 分文件
每个队伍一个文件：`data/teams/team-{id}.json`，另有一个索引文件 `data/teams-index.json` 保存队伍 id/name 列表。

**优点：**
- 读/写某队伍时只加锁该队伍文件，多队伍并发互不影响
- 单队伍文件小，调试方便

**缺点：**
- 需要维护索引文件（队伍列表、创建时间等元数据），引入双写一致性问题
- 新增跨队伍操作（未来可能有）复杂度高
- `JsonRepository` 从"管理1个文件"变为"管理 N+1 个文件"，ReadWriteLock 需按文件分组，实现复杂度显著上升
- 目前最多几十支队伍，收益不明显

### 方案 B：按业务域分文件
核心实体文件 `data/tennis-data.json`（Team + Player + Lineup），配置文件 `data/tennis-config.json`（ConstraintPresets 及未来其他配置）。

**优点：**
- 改动量小：`JsonRepository` 只需增加第二个文件的读写，锁机制复用
- 核心数据与配置数据读写频率不同（核心数据频繁，配置偶尔），分离后写锁互不影响
- 结构清晰，未来配置类数据（用户偏好、系统设置等）天然归入 config 文件
- 迁移简单：旧 `tennis-data.json` 保留原结构，config 是全新文件

**缺点：**
- 核心文件仍然包含 team/player/lineup 三类数据，若未来 lineup 数据量大仍需再拆

### 推荐：方案 B（按业务域分文件）

理由：收益与成本比最优。当前痛点是 constraint presets 等配置数据混入核心文件，方案 B 精准解决这个问题，且实现改动极小。方案 A 的队伍数量在可预见范围内不会带来性能问题，引入的索引一致性复杂度得不偿失。

---

## Capabilities

### New Capabilities
（无新能力，纯存储重构）

### Modified Capabilities
- `data-persistence`：存储层从单文件改为双文件（核心数据 + 配置数据），`JsonRepository` 扩展为管理两个独立文件，各自有独立的 ReadWriteLock；上层 Service 接口不变。

## Impact

- **后端**：`JsonRepository` 新增 config 文件路径配置与读写方法；`TeamData` 中 `constraintPresets` 字段移至 `ConfigData` 模型；`application.yml` 新增 `storage.config-file` 配置项。
- **数据迁移**：首次启动时，若 `tennis-config.json` 不存在则创建空文件；`tennis-data.json` 中若有 `constraintPresets` 字段（未来版本）则迁移到 config 文件（启动时一次性迁移）。
- **其他**：API 和前端无变化；测试中使用 `@TempDir` 的需提供两个临时路径。
- **风险**：低。Service 层以上完全不感知文件拆分；原子写入和 ReadWriteLock 机制复用。
