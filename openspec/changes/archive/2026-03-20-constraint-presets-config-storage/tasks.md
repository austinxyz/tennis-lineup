## 1. 后端：ConfigData 模型 & ConstraintPreset 模型

- [x] 1.1 新增 `ConstraintPreset` 模型类（id, name, createdAt, excludePlayers: List<String>, includePlayers: List<String>, pinPlayers: Map<String,String>）
- [x] 1.2 新增 `ConfigData` 模型类：`Map<String, List<ConstraintPreset>> constraintPresets`（key = teamId），默认空 HashMap

## 2. 后端：JsonRepository 扩展双文件支持

- [x] 2.1 在 `application.yml` 新增 `storage.config-file` 配置项，默认值 `data/tennis-config.json`
- [x] 2.2 `JsonRepository` 注入 `configFilePath`（`@Value("${storage.config-file:data/tennis-config.json}")`）
- [x] 2.3 `JsonRepository` 新增独立的 `ReadWriteLock configLock` 和 `Path configPath`
- [x] 2.4 实现 `readConfig(): ConfigData` 方法（文件不存在时返回空 ConfigData，复用现有逻辑）
- [x] 2.5 实现 `writeConfig(ConfigData)` 方法（configLock 写锁 + 原子写入 temp + rename）
- [x] 2.6 构造函数中初始化 configPath，确保父目录存在

## 3. 后端：约束预设 API

- [x] 3.1 新增 `ConstraintPresetService`：listPresets(teamId)、createPreset(teamId, preset)、deletePreset(teamId, presetId) — 读写 ConfigData
- [x] 3.2 新增 `ConstraintPresetController`：`GET /api/teams/{id}/constraint-presets`、`POST /api/teams/{id}/constraint-presets`、`DELETE /api/teams/{id}/constraint-presets/{presetId}`
- [x] 3.3 deletePreset 时若 presetId 不存在返回 404

## 4. 后端：已保存排阵 UTR 重校验

- [x] 4.1 `Lineup` 响应 DTO 新增 transient 字段 `currentValid: boolean` 和 `currentViolations: List<String>`（不持久化，Jackson 序列化但不反序列化）
- [x] 4.2 在 `LineupService.getLineups(teamId)` 中，对每条已保存排阵调用 `ConstraintService.validateLineup(lineup, currentPlayers)`，将结果写入 transient 字段

## 5. 后端测试

- [x] 5.1 更新 `JsonRepositoryTest`：为 config 文件读写提供独立 `@TempDir` 路径；新增 readConfig / writeConfig 正常流程、文件不存在、并发安全、双锁独立测试
- [x] 5.2 新增 `ConstraintPresetServiceTest`：创建/列表/删除预设正常流程；删除不存在预设返回 404；读写经过 ConfigData
- [x] 5.3 新增/更新 `LineupServiceTest`：getLineups 返回的排阵包含正确的 `currentValid` / `currentViolations`（模拟 UTR 变化后校验结果变化）
- [x] 5.4 运行全量后端测试，确认所有已有测试通过（172 tests passed）

## 6. 前端：约束预设选择器

- [x] 6.1 新增 `useConstraintPresets.js` composable：fetchPresets(teamId)、savePreset(teamId, name, constraints)、deletePreset(teamId, presetId)
- [x] 6.2 新增 `ConstraintPresetSelector.vue`：预设下拉列表 + 名称输入 + 保存按钮 + 删除按钮；空预设时显示"暂无预设"
- [x] 6.3 在 `LineupGenerator.vue` 中集成 `ConstraintPresetSelector`，加载预设时更新 `PlayerConstraintSelector` 状态
- [x] 6.4 加载预设时过滤不存在的球员 ID，展示警告提示"部分球员已不在队伍中，相关约束已跳过"

## 7. 前端：已保存排阵合法性标识

- [x] 7.1 在 `LineupHistory.vue`（或排阵卡片组件）中根据 `currentValid` 展示绿色"合法"或红色"已失效"徽标
- [x] 7.2 当 `currentValid: false` 时，在卡片中展示 `currentViolations` 列表

## 8. 前端：排阵生成页跳转链接

- [x] 8.1 在 `LineupGenerator.vue` 中新增 `<router-link>` "查看已保存排阵 →"，指向 `/teams/:id/lineups`，仅在已选择队伍时显示

## 9. 前端测试

- [x] 9.1 新增 `useConstraintPresets.test.js`：fetchPresets、savePreset、deletePreset 正常流程和错误处理
- [x] 9.2 新增 `ConstraintPresetSelector.test.js`：加载预设更新约束、保存预设、删除预设、空预设占位符、已删除球员警告
- [x] 9.3 运行全量前端测试，确认所有已有测试通过（256 tests passed）

## 10. 重启后端并验证

- [x] 10.1 停止当前 Spring Boot 进程，重新执行 `mvn spring-boot:run` 使代码变更生效
- [x] 10.2 确认 `tennis-config.json` 首次启动自动创建，`tennis-data.json` 内容完全不变
- [x] 10.3 手动验证：API 返回 `currentValid`/`currentViolations`；constraint-presets CRUD 端点正常

## 11. 测试报告

- [x] 11.1 后端测试：172/172 通过，0 失败（11 个测试类：controller × 2, repository, service × 8）
- [x] 11.2 前端测试：256/256 通过，0 失败（20 个测试文件，含新增 useConstraintPresets + ConstraintPresetSelector）
