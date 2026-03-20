## 1. 后端：ConstraintPreset 模型与存储

- [ ] 1.1 新增 `ConstraintPreset` 模型类（id, name, createdAt, excludePlayers, includePlayers, pinPlayers）
- [ ] 1.2 在 `Team`（或 `TeamData`）模型中新增 `constraintPresets: List<ConstraintPreset>` 字段，默认空列表，向后兼容
- [ ] 1.3 在 `JsonRepository` 中确认 `constraintPresets` 字段能正确序列化/反序列化（null 兼容处理）

## 2. 后端：约束预设 API

- [ ] 2.1 新增 `ConstraintPresetService`：createPreset、listPresets、deletePreset 方法
- [ ] 2.2 新增 `ConstraintPresetController`：`GET /api/teams/{id}/constraint-presets`、`POST /api/teams/{id}/constraint-presets`、`DELETE /api/teams/{id}/constraint-presets/{presetId}`
- [ ] 2.3 删除预设时，若 presetId 不存在返回 404

## 3. 后端：已保存排阵 UTR 重校验

- [ ] 3.1 在 `LineupService.getLineups(teamId)` 中，对每条已保存排阵调用 `ConstraintService.validateLineup`，附加 `currentValid` 和 `currentViolations` 字段到响应
- [ ] 3.2 确认 `Lineup` 响应 DTO 包含 `currentValid: boolean` 和 `currentViolations: List<String>` 字段（不持久化）

## 4. 后端测试

- [ ] 4.1 `ConstraintPresetServiceTest`：创建、列表、删除预设；删除不存在预设返回 404
- [ ] 4.2 `LineupServiceTest`（或等价）：getLineups 返回的排阵包含正确的 `currentValid` / `currentViolations`（UTR 变化后校验结果变化）
- [ ] 4.3 运行全量后端测试，确认所有已有测试通过

## 5. 前端：约束预设选择器组件

- [ ] 5.1 新增 `ConstraintPresetSelector.vue` 组件：预设下拉列表 + 保存当前约束为预设（输入名称 + 保存按钮）+ 删除预设按钮
- [ ] 5.2 新增 `useConstraintPresets.js` composable：fetchPresets、savePreset、deletePreset（调用对应 API）
- [ ] 5.3 在 `LineupGenerator.vue` 中集成 `ConstraintPresetSelector`，加载预设时更新 PlayerConstraintSelector 的状态
- [ ] 5.4 处理预设含已删除球员的情况：加载时过滤不存在的球员 ID 并展示警告提示

## 6. 前端：已保存排阵合法性标识

- [ ] 6.1 在 `LineupHistory.vue`（或已保存排阵卡片组件）中，根据 `currentValid` 展示绿色"合法"或红色"已失效"徽标
- [ ] 6.2 当 `currentValid: false` 时，展示 `currentViolations` 列表（折叠或直接显示）

## 7. 前端：排阵生成页跳转链接

- [ ] 7.1 在 `LineupGenerator.vue` 中新增 `<router-link>` "查看已保存排阵 →"，指向 `/teams/:id/lineups`，仅在已选择队伍时显示

## 8. 前端测试

- [ ] 8.1 `useConstraintPresets.test.js`：fetchPresets、savePreset、deletePreset 正常流程和错误处理
- [ ] 8.2 `ConstraintPresetSelector.test.js`：加载预设触发约束更新、保存预设、删除预设、空预设占位符
- [ ] 8.3 运行全量前端测试，确认所有已有测试通过

## 9. 重启后端并验证

- [ ] 9.1 停止当前 Spring Boot 进程，重新执行 `mvn spring-boot:run` 使代码变更生效
- [ ] 9.2 手动验证：保存约束预设 → 重新加载 → 选择预设 → 生成排阵；修改球员 UTR → 查看历史排阵合法性标识更新

## 10. 测试报告

- [ ] 10.1 运行 `JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 mvn test` 并记录后端测试报告（通过数 / 总数 / 失败明细）
- [ ] 10.2 运行前端测试并记录报告（通过数 / 总数 / 失败明细）
