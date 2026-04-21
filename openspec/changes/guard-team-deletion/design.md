## Context

当前 `TeamService.deleteTeam(String teamId)` 只做 "存在即删" 的语义：

```java
// backend/src/main/java/com/tennis/service/TeamService.java:101-111
public void deleteTeam(String teamId) {
    TeamData teamData = jsonRepository.readData();
    boolean removed = teamData.getTeams().removeIf(team -> team.getId().equals(teamId));
    if (!removed) {
        throw new IllegalArgumentException("队伍不存在");
    }
    jsonRepository.writeData(teamData);
    log.info("Deleted team: {}", teamId);
}
```

前端两处 UI 入口（`TeamList.vue:129-133`、`TeamListPanel.vue:167-173`）都只是一个原生 `confirm()`，然后调用 `useTeams.js:59 deleteTeam(id)`，直接 `DELETE /api/teams/{id}` → 204 返回。

Team 模型上已经同时挂着 `players: Player[]` 和 `lineups: Lineup[]`（后者由 `team-management` spec 保障为 `[]` 而非 `null`）。所以要做的"非空判断"直接读 in-memory 的 `Team` 即可，不需要跨 service 查询。

排阵生成的多候选能力已经在 `openspec/specs/lineup-multi-result/spec.md` 定义（最多 6 个方案、去重、并排展示），**本次改动不会修改它的生成/渲染逻辑**，只是在 spec 中显式加一条保护条款，避免后续无关改动误把它退化成单候选。

## Goals / Non-Goals

**Goals:**
- 阻止误删除非空队伍（球员或保存的排阵任一不为空）。
- 错误语义明确：HTTP 409 + `code: "TEAM_NOT_EMPTY"` + 人类可读的中文 message。
- 前端在"明显能看出来队伍非空"时直接禁用删除按钮，避免发请求被打回来。
- spec 层为 "排阵多候选" 能力加一条防回退条款。

**Non-Goals:**
- **不**引入软删除 / 归档 / 回收站。如果产品后续需要，另开 proposal。
- **不**做级联删除（自动先删球员再删队伍）—— 明确让用户自己走"先清空再删除"路径，避免一次误触毁掉所有东西。
- **不**改动 `lineup-generation` / `lineup-multi-result` 的实际生成算法、渲染、保存路径。
- **不**改动 `DELETE /api/teams/{id}/players/{pid}`（球员删除）的行为。

## Decisions

### D1. 非空判定在 Service 层，而非 Controller 层
**选择**：`TeamService.deleteTeam` 内部做判定。  
**理由**：和项目既有约定一致（[CLAUDE.md] "后端 service 层验证"）；`TeamController.deleteTeam` 现在就是无脑 `teamService.deleteTeam(id)` + 返回 204，保持 Controller 薄。  
**拒绝的替代**：在 Controller 里查 team → 判定 → 再调 service。这会让并发下的 TOCTOU 窗口更大（两次读），而且和现有风格不符。

### D2. 用新异常 `TeamNotEmptyException` 而不是复用 `IllegalArgumentException`
**选择**：新增 `TeamNotEmptyException extends RuntimeException`，在 `GlobalExceptionHandler` 映射到 HTTP 409 + `code: "TEAM_NOT_EMPTY"`。  
**理由**：
- 现有 `IllegalArgumentException` → 400 `VALIDATION_ERROR` 的映射已经被 `NotFoundException extends IllegalArgumentException` 用"更具体的类型优先"复用了一次（见 context）。再往里套一层会让 handler 的优先级更脆弱。
- 409 Conflict 语义上比 400 Validation 更准确 —— "请求本身格式正确，但当前资源状态不允许此操作"。
- 单独异常类未来好扩展（比如加 `playerCount` / `lineupCount` 到 details）。  

**拒绝的替代**：复用 `ValidationException` + 400。语义错，且前端要靠 message 字符串匹配才能区分"名字重复"和"非空"，脆弱。

### D3. 前端按钮：非空队伍 disable + tooltip，不隐藏
**选择**：按钮保留但 `disabled`，hover 时 tooltip 显示"请先移除球员和已保存的排阵"。  
**理由**：可发现性 > 隐藏。用户能看到删除按钮存在，只是当前状态下不可用，比"按钮消失了"更不困惑。  
**拒绝的替代**：
- 直接隐藏按钮 → 用户会以为"这个队不能删"而不是"当前状态不让删"。
- 按钮常亮 + 点击后报错 → 可以，但浪费一次网络往返，且 UX 差。

### D4. 前端不自己做终极判定，后端仍是唯一真相
**选择**：即便前端禁用了按钮，后端依然在 service 层做完整的非空校验。前端拿到 team 列表时的 `players.length`/`lineups.length` 只作为 UI hint。  
**理由**：前端列表可能是缓存、可能被另一个标签页同时修改。服务端校验是唯一可信来源。前端禁用只是 UX 优化。

### D5. 错误响应 payload 里带上数量
**选择**：409 响应的 `details` 字段填 `{"playerCount": N, "lineupCount": M}`。  
**理由**：前端可以直接显示 "队伍里还有 3 名球员和 2 个已保存排阵"，比干巴巴的 "非空" 更可操作。和现有 `ErrorResponse` 模型的 `details` 字段（目前是 `null`）兼容。

### D6. `lineup-multi-result` 只加一条 spec 需求，不写代码
**选择**：只在 spec.md 里加 "Requirement: Multi-candidate lineup generation is preserved" + 一条场景，保证 `POST /api/lineups/generate` 返回 array 而非 single object。  
**理由**：用户明确提到"排阵生成还是要有多个选择的" —— 这是"确认保留"而非"新增"。用 spec 条款冻结当前契约，防止无关重构把它改掉。不需要任何代码改动。  
**拒绝的替代**：跳过这条，只改 team 删除。风险是 spec 没沉淀用户的明确意图，未来重构时容易丢。

## Risks / Trade-offs

- **[Risk] 用户被"卡住"不知道怎么清空队伍** → Mitigation：409 的 message 里写清楚"请先删除球员 / 排阵"，tooltip 同步，`details` 带数量。TeamDetail 页已经有删除球员的能力；排阵删除能力后续若缺可另开 ticket（本次先不捆绑）。
- **[Risk] 现有 E2E `team-management.spec.js` 可能在"创建→立即删除"的流程里依赖"非空也能删"** → Mitigation：检查 + 更新测试，确保删除前先走"清空球员"路径，或用空队伍做删除断言。
- **[Risk] 与未来"软删除"方案冲突** → Mitigation：本次只加硬防护，不引入新数据字段。未来如果做软删除，换成"标记 archived" 语义即可，本次代码不会成为迁移障碍。
- **[Trade-off] 不做级联删除** → 用户多两步操作。但换来的是不会一次误触丢掉球员/排阵的主数据，符合 CLAUDE.md 的"执行带破坏性的操作要谨慎"原则。
- **[Trade-off] `lineup-multi-result` 加保护条款看起来多此一举** → 成本极低（几行 spec），收益是把用户明确表达的意图落到契约里；未来重构/改版时 diff review 会看到这条，防止回退。

## Migration Plan

1. 合并后无数据迁移 —— 不改 schema，不改 `tennis-data.json` 结构。
2. 已有的空队伍继续可删除（行为一致）；非空队伍的"删除"操作从"成功 204"变为"409 拒绝"，这是**行为破坏性变更**，需在 PR 描述和 `docs/log/YYYY-MM-DD.md` 中标注。
3. **回滚策略**：如需紧急回滚，revert 后端 `TeamService.deleteTeam` 改动即可；前端按钮禁用逻辑即使不回滚也不会导致错误（只是空队伍也会禁用，需要看实际实现）。建议连同前端一起回滚保持一致。
4. 部署顺序：后端先行（前端旧版本即使不禁用按钮，点击也会被 409 挡住）→ 前端跟进（补 tooltip 与 disabled 状态）。
