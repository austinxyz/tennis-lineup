# 测试计划

## 概述

本文档列出系统所有模块的测试范围、测试用例清单及当前通过状态。

**当前测试覆盖**：后端 153 个测试 · 前端 235 个测试 · 全部通过

---

## 后端测试（JUnit 5 + Mockito）

运行方式：
```bash
cd backend
JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 mvn test
# Windows: set JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 && mvn test
```

报告位置：`backend/target/surefire-reports/`

---

### TeamService（13 项）

| 测试用例 | 说明 |
|---------|------|
| shouldCreateTeamWithValidName | 正常创建队伍 |
| shouldThrowExceptionWhenCreatingTeamWithEmptyName | 空名称拒绝 |
| shouldThrowExceptionWhenCreatingTeamWithNullName | null 名称拒绝 |
| shouldThrowExceptionWhenCreatingTeamWithDuplicateName | 重复名称拒绝 |
| shouldThrowExceptionWhenCreatingTeamWithLongName | 超过 50 字符拒绝 |
| shouldGetAllTeamsSortedByCreationDate | 返回列表按创建时间降序 |
| shouldGetTeamById | 按 ID 查询队伍 |
| shouldThrowExceptionWhenGettingTeamWithNonExistentId | 不存在的 ID 返回 404 |
| shouldUpdateTeamName | 更新队伍名称 |
| shouldThrowExceptionWhenUpdatingTeamNameToDuplicate | 更新为已有名称拒绝 |
| shouldDeleteTeam | 删除队伍 |
| shouldThrowExceptionWhenDeletingNonExistentTeam | 删除不存在队伍报错 |
| shouldGenerateUniqueTeamIds | ID 唯一性 |

---

### PlayerService（15 项）

| 测试用例 | 说明 |
|---------|------|
| shouldAddPlayerToTeamWithValidData | 正常添加球员 |
| shouldThrowExceptionWhenAddingPlayerWithEmptyName | 空名称拒绝 |
| shouldThrowExceptionWhenAddingPlayerWithNullName | null 名称拒绝 |
| shouldThrowExceptionWhenAddingPlayerWithInvalidGender | 非法性别拒绝 |
| shouldThrowExceptionWhenAddingPlayerWithInvalidUTR | 负值或 >16 的 UTR 拒绝 |
| shouldThrowExceptionWhenAddingPlayerToNonExistentTeam | 队伍不存在时拒绝 |
| shouldUpdatePlayerWithValidData | 正常更新球员 |
| shouldThrowExceptionWhenUpdatingNonExistentPlayer | 更新不存在球员报错 |
| shouldDeletePlayerFromTeam | 删除球员 |
| shouldThrowExceptionWhenDeletingNonExistentPlayer | 删除不存在球员报错 |
| shouldGetPlayersByTeamId | 按队伍查询球员列表 |
| shouldReturnEmptyListWhenGettingPlayersFromTeamWithNoPlayers | 无球员时返回空列表 |
| shouldThrowExceptionWhenGettingPlayersFromNonExistentTeam | 队伍不存在报错 |
| shouldConvertGenderToLowercase | 性别统一转小写 |
| shouldTrimPlayerName | 名称去除首尾空格 |

---

### BatchImportService（14 项）

| 测试用例 | 说明 |
|---------|------|
| shouldImportPlayersFromCSVContent | 正常导入 CSV |
| shouldSkipHeaderRowWhenImportingFromCSV | 跳过 CSV 表头行 |
| shouldHandleEmptyCSVContent | 空 CSV 内容处理 |
| shouldHandleCSVWithMissingFields | 缺少字段的行跳过 |
| shouldHandleInvalidUTRInCSV | 非法 UTR 行跳过 |
| shouldHandleUTROfRangeInCSV | 超范围 UTR 行跳过 |
| shouldHandleInvalidGenderInCSV | 非法性别行跳过 |
| shouldHandleEmptyNameInCSV | 空名称行跳过 |
| shouldImportPlayersFromJSONContent | 正常导入 JSON |
| shouldImportFromJSONWithoutBrackets | 无括号 JSON 处理 |
| shouldHandleEmptyJSONContent | 空 JSON 处理 |
| shouldHandleJSONParsingErrors | JSON 解析错误处理 |
| shouldHandlePartialImportWithSomeValidAndSomeInvalidRows | 部分成功、部分失败时返回成功数 |
| shouldContinueImportAfterPlayerServiceThrowsException | 单条失败不中断整体导入 |

---

### ConstraintService（13 项）

| 测试用例 | 说明 |
|---------|------|
| testValidLineup | 合法排阵通过所有约束 |
| testUtrOrderingViolation | D2 高于 D1 时报错 |
| testTotalUtrCapViolation | 总 UTR > 40.5 时报错 |
| testMinFemaleViolation | 女性少于 2 人时报错 |
| testPartnerUtrGapViolation | 搭档 UTR 差 > 3.5 时报错 |
| testD4VerifiedViolation | D4 含未验证球员时报错 |
| testPlayerUniquenessViolation | 同一球员出现两次时报错 |
| testExactlyTwoFemalesPasses | 恰好 2 名女性通过 |
| testTotalUtrExactlyAtCapPasses | 总 UTR 恰好 40.5 通过 |
| testPartnerUtrGapExactly35Passes | 搭档差恰好 3.5 通过 |
| testBothD4VerifiedPasses | D4 两人均已验证通过 |
| testMultipleViolationsReported | 多条违规同时报告 |

---

### LineupGenerationService（22 项）

| 测试用例 | 说明 |
|---------|------|
| testGeneratesAtLeastOneCandidate | 8 人阵容至少生成 1 个候选 |
| testAllCandidatesAreValid | 所有候选均通过约束验证 |
| testEachLineupHas4Pairs | 每个排阵含 4 对 |
| testPositionAssignmentDescendingUtr | D1 ≥ D2 ≥ D3 ≥ D4 按 UTR 降序排列 |
| testPlayerUniquePerLineup | 每个排阵中球员不重复 |
| testBalancedHeuristicSelectsMinVariance | 平衡策略选择最小方差方案 |
| testAggressiveHeuristicSelectsMaxTopThree | 激进策略选择 D1+D2+D3 最高方案 |
| testPruningReducesCandidates | 剪枝减少候选数 |
| testEmptyCandidatesReturnsNull | 无有效候选时返回 null |
| testD4ContainsVerifiedPlayers | D4 位置分配已验证球员 |
| testTotalUtrIsCorrect | 总 UTR 等于 8 名球员之和 |
| testExcludePlayersFilter | 排除指定球员 |
| testIncludePlayersFilter | 强制包含指定球员 |
| testPinPlayerToPosition | 钉选球员到指定位置 |
| testPinPlayerInvalidPositionThrows | 非法位置钉选报错 |
| testPinAndExcludeConflictThrows | 同一球员既钉选又排除时报错 |
| testIncludeExcludeOverlapThrows | 强制包含与排除重叠时报错 |
| testTooManyIncludePlayersThrows | 强制包含超过 8 人时报错 |
| testTooManyExcludePlayersThrows | 排除后剩余少于 8 人时报错 |
| testSubsetEnumerationSelectsHighUtrCombination | 超过 8 人时选最高 UTR 子集 |
| testGenderFieldsPopulated | 生成结果含性别字段 |

---

### LineupService（19 项）

| 测试用例 | 说明 |
|---------|------|
| testGenerateMultipleSuccess | 生成结果含 id 和 createdAt |
| testGenerateMultipleReturnsSix | 候选数上限为 6 |
| testGenerateMultipleDoesNotPersist | 生成不自动保存 |
| testGenerateThrowsForInsufficientPlayers | 球员不足时报错 |
| testGenerateThrowsForUnknownTeam | 队伍不存在时报错 |
| testGenerateThrowsWhenNoValidLineup | 无合法排阵时报错 |
| testBalancedPrimaryRankByUtrProximity | 平衡策略选最接近 40.5 的方案 |
| testPinPlayersPassedToGenerationService | 钉选球员传递给生成服务 |
| testIncludePlayersPassedToGenerationService | 强制包含传递给生成服务 |
| testSaveLineupPersistsAndReturns | 保存排阵成功并返回 |
| testSaveLineupPreservesExistingId | 保留已有 ID（不重复生成） |
| testSaveLineupUnknownTeam | 队伍不存在时保存报错 |
| testSaveLineupRejectsDuplicate | 相同 8 人组合拒绝重复保存 |
| testSaveLineupAcceptsNonDuplicate | 不同球员组合正常保存 |
| testSaveLineupFirstSaveAlwaysAccepted | 首次保存无需重复检查 |
| testGetLineupsByTeamReturnsReverseChronological | 历史记录按时间降序返回 |
| testGetLineupsByTeamEnrichment | 读取历史时补充 UTR/性别 |
| testDeleteLineupSuccess | 删除排阵成功 |
| testDeleteLineupThrowsNotFound | 删除不存在排阵报 404 |

---

### TeamController（17 项）

| 测试用例 | 说明 |
|---------|------|
| shouldGetAllTeams | GET /api/teams 返回队伍列表 |
| shouldGetTeamById | GET /api/teams/:id 返回单个队伍 |
| shouldGetPlayersByTeamId | GET /api/teams/:id/players 返回球员列表 |
| shouldReturn404WhenGettingNonExistentTeam | 不存在队伍返回 404 |
| shouldCreateNewTeam | POST /api/teams 创建队伍 |
| shouldReturn400WhenCreatingTeamWithEmptyName | 空名称返回 400 |
| shouldUpdateTeamName | PUT /api/teams/:id 更新名称 |
| shouldDeleteTeam | DELETE /api/teams/:id 删除队伍 |
| shouldAddPlayerToTeam | POST /api/teams/:id/players 添加球员 |
| shouldUpdatePlayer | PUT /api/teams/:id/players/:playerId 更新球员 |
| shouldDeletePlayerFromTeam | DELETE /api/teams/:id/players/:playerId 删除球员 |
| shouldImportPlayersFromCSVFile | POST /api/teams/import 批量导入 |
| shouldReturn400WhenImportingEmptyFile | 空文件返回 400 |
| shouldReturn400WhenImportingUnsupportedFileType | 不支持格式返回 400 |
| shouldAddPlayerWithProfileUrl | 添加球员含 profileUrl |
| shouldIncludeProfileUrlInGetPlayersResponse | 查询球员响应含 profileUrl |
| shouldHandleImportErrorsGracefully | 导入部分失败时仍返回成功条目 |

---

### LineupController（15 项）

| 测试用例 | 说明 |
|---------|------|
| testGenerateLineupSuccess | POST /api/lineups/generate 正常返回候选 |
| testGenerateLineupReturnsMultiple | 返回最多 6 个候选 |
| testGenerateLineupInsufficientPlayers | 球员不足返回 400 |
| testGenerateLineupConstraintViolation | 约束冲突返回 400 |
| testGenerateLineupTeamNotFound | 队伍不存在返回 404 |
| testGenerateLineupWithPinPlayers | 钉选球员正确传递 |
| testGenerateLineupWithIncludePlayers | 强制包含正确传递 |
| testResponseIncludesGenderFields | 响应含性别字段 |
| testSaveLineupSuccess | POST /api/teams/:id/lineups 保存成功 |
| testSaveLineupTeamNotFound | 队伍不存在返回 404 |
| testGetLineupHistory | GET /api/teams/:id/lineups 返回历史 |
| testGetLineupHistoryTeamNotFound | 队伍不存在返回 404 |
| testGetLineupHistoryEmpty | 无历史返回空数组 |
| testDeleteLineupSuccess | DELETE /api/lineups/:id 返回 204 |
| testDeleteLineupNotFound | 不存在排阵返回 404 |

---

### JsonRepository（12 项）

| 测试用例 | 说明 |
|---------|------|
| shouldCreateDataFileIfItDoesntExist | 文件不存在时自动创建 |
| shouldReadDataFromExistingFile | 读取现有文件数据 |
| shouldWriteDataToFile | 写入数据到文件 |
| shouldHandleFileNotFoundExceptionGracefully | 文件不可读时返回空数据 |
| shouldPreserveDataWhenWritingMultipleTimes | 多次写入不丢失数据 |
| shouldHandleConcurrentReadsSafely | 10 并发读取安全 |
| shouldHandleConcurrentWritesSafely | 5 线程各 10 次写入安全 |
| shouldMaintainDataConsistencyDuringConcurrentReadWrite | 5 读 2 写并发下数据一致 |
| shouldHandleLargeDataSets | 1000 条数据读写正常 |
| shouldUseAtomicWrite | 使用临时文件 + 重命名原子写入 |
| shouldHandleConcurrentMixedReadsAndWritesWithoutDataCorruption | 10 线程混合读写无数据损坏 |

---

## 前端测试（Vitest + Vue Test Utils）

运行方式：
```bash
cd frontend
npm test                # 单次运行
npm run test:coverage   # 覆盖率报告（输出到 coverage/index.html）
```

---

### Composables

#### useApi（13 项）
- get / post / put / del 请求方法、loading 状态管理、HTTP 错误处理、错误清除

#### useTeams（13 项）
- fetchTeams、fetchTeamById、createTeam、updateTeam、deleteTeam（含确认对话框）、loading 状态

#### usePlayers（15 项）
- fetchPlayers、addPlayer、updatePlayer、deletePlayer（含确认）、bulkUpdateUtrs（含部分失败）、loading 状态

#### useBatchImport（12 项）
- importFromCSV、importFromJSON、loading 状态、结果覆盖行为

#### useLineup（12 项）
- generateLineup（含策略/钉选/排除参数）、saveLineup、fetchLineupHistory、deleteLineup

#### useLineupHistory（6 项）
- fetchLineups（URL 验证、错误处理）、deleteLineup（成功移除、失败不修改）

---

### Components

#### PlayerForm（25 项）
- 初始渲染（空表单、预填充）、提交按钮状态（完整才启用）
- 名称/性别/UTR 验证（边界值 0 和 16 通过）
- 事件（cancel / submit 含完整数据）、profileUrl 字段、watch initialData 重置

#### TeamListPanel（11 项）
- 队伍列表渲染、loading 状态、空状态、创建/导入弹窗、删除确认、高亮当前队伍

#### LineupCard（16 项）
- 4 个位置、球员名称、组合 UTR、总 UTR 渲染
- AI 标志（aiUsed）、showPlayerUtr 控制、性别显示（M/F）、null 性别处理、位置排序

#### PlayerConstraintSelector（16 项）
- 无球员空状态、排序（女性优先、同性别按 UTR 降序）
- 性别/已验证标签、7 个下拉选项、约束 emit（pin/include/exclude）、汇总行

#### LineupSwapPanel（9 项）
- 初始状态（按钮禁用）、选择与取消选择、合法互换 emit
- 自动重排（互换后 UTR 顺序违规时自动排序）、重置功能

#### StrategySelector（8 项）
- 初始显示平衡策略、自定义模式切换、preset 选择 emit

#### NavSidebar（8 项）
- 导航链接渲染、active 状态、navigate 事件 emit

#### LineupResultGrid（13 项）
- 空状态、所有方案同时显示（网格布局）
- 方案 1 "最佳"标签与绿色高亮、最多 6 个方案
- 保存按钮 → 已保留 ✓、保存失败保留按钮

---

### Views

#### TeamDetail（22 项）
- 团队名称、球员表格（姓名/性别/UTR/已验证/操作）
- loading/空状态、添加/编辑/删除球员（含确认弹窗）
- UTR 格式化（2 位小数）、profileUrl 链接、球员数量显示
- 批量编辑 UTR（进入/保存/取消/部分失败错误列表）

#### LineupGenerator（12 项）
- 双栏布局（左 2/5、右 3/5）、队伍选择、生成按钮状态
- 生成调用含约束参数、loading 状态、错误显示与清除

#### LineupHistoryView（7 项）
- 挂载时调用 fetchLineups、空状态"暂无保存的排阵"
- 每个排阵渲染 LineupCard、删除成功移除卡片、删除失败显示错误、取消确认不删除

---

## 测试覆盖范围总结

| 模块 | 测试文件数 | 测试项数 |
|------|-----------|---------|
| 后端 Service | 6 | 96 |
| 后端 Controller | 2 | 32 |
| 后端 Repository | 1 | 12 |
| 后端 AI Service | 1 | 6 |
| **后端合计** | **10** | **153** |
| 前端 Composables | 6 | 71 |
| 前端 Components | 8 | 106 |
| 前端 Views | 3 | 41 |
| **前端合计** | **18** | **235** |
| **总计** | **28** | **388** |

---

## 当前未覆盖的场景

- E2E 流程测试（已有 Playwright 基础设施，暂无完整测试用例）
- 并发保存排阵（防重复保存在高并发下的行为）
- 大型阵容（>20 名球员）生成性能
- 文件存储损坏恢复
