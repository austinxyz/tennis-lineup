## 1. 后端：Pair 模型扩展

- [x] 1.1 在 `Pair.java` 中添加 `player1Gender` 和 `player2Gender` 字段（String，值为 "male"|"female"），加 `@JsonProperty` 注解
- [x] 1.2 在 `LineupGenerationService.buildLineup` 中填充 `player1Gender` / `player2Gender`（从 Player 对象读取）

## 2. 后端：GenerateLineupRequest 扩展

- [x] 2.1 在 `GenerateLineupRequest.java` 中恢复 `includePlayers: List<String>` 字段（默认空 List），表示"一定上场"（不限位置）

## 3. 后端：LineupGenerationService 算法重构

- [x] 3.1 新增私有方法 `enumerateSubsets(List<Player> roster, int size)`：返回所有 C(n, size) 子集，按总 UTR 降序排列
- [x] 3.2 修改 `generateCandidates(players, include, exclude, pinPlayers)` 主逻辑：先通过 `enumerateSubsets` 得到候选 8 人组合，再对每组合运行 `backtrack`；子集评分：总 UTR ≤ 40.5 且女性数量最少但 ≥ 2
- [x] 3.3 修复同位置双人 pin 逻辑：按位置 group pinPlayers 条目；若同一位置有 2 人 → 要求他们在该位置配对；若 >2 人 → 抛 400
- [x] 3.4 在 `generateCandidates` 中处理 `includePlayers`：确保所有 include 球员出现在结果排阵中（与 pinPlayers 中球员自动合并到 include 集合）
- [x] 3.5 女性最少化：枚举子集时，优先选取恰好 2 名女性的子集；仅当无满足约束的 2-女子集时才允许更多

## 4. 后端：LineupController 更新

- [x] 4.1 在 `POST /api/lineups/generate` 处理器中读取 `request.getIncludePlayers()`，传入 `generateMultipleAndSave` 的 include 参数

## 5. 后端：LineupService 更新

- [x] 5.1 更新 `generateMultipleAndSave` 签名：`includePlayers` 参数由原来被删除恢复为 `Set<String>`；调用链传入 `generateCandidates`

## 6. 后端测试更新

- [x] 6.1 更新 `LineupGenerationServiceTest`：覆盖子集枚举（roster > 8 时选高 UTR 子集）、2-女优先、双人同位置 pin、>2 人同位置 400
- [x] 6.2 更新 `LineupControllerTest`：覆盖 includePlayers 参数传递、player1Gender/player2Gender 出现在响应中
- [x] 6.3 更新 `LineupServiceTest`：`generateMultipleAndSave` mock 调用签名匹配新的 include 参数

## 7. 前端：PlayerConstraintSelector 改为下拉列表

- [x] 7.1 将 `<button>` 切换控件替换为 `<select>` 下拉，选项为：中立 / 不上 / 一定上 / D1 / D2 / D3 / D4
- [x] 7.2 内部状态从 6 态改为 7 态字符串：`'neutral' | 'exclude' | 'include' | 'D1' | 'D2' | 'D3' | 'D4'`
- [x] 7.3 更新 emit payload：加入 `includePlayers: string[]`（包含 `include` 状态的球员 + D1-D4 状态的球员 ID）；`pinPlayers` 仅包含 D1-D4 状态
- [x] 7.4 更新摘要行：显示"固定位置: N 人 / 一定上场: M 人 / 排除: P 人"（其中一定上场 M 不含固定位置的人）
- [x] 7.5 在每个球员行加入性别徽章：male → "M"（蓝色）；female → "F"（粉色）

## 8. 前端：LineupCard 加入性别显示

- [x] 8.1 在 LineupCard 中为每个球员名旁加入性别文字（读取 `pair.player1Gender` / `pair.player2Gender`）；显示为"男"/"女"文字值（蓝色/粉色）

## 9. 前端：LineupSwapPanel 改为自动重排

- [x] 9.1 移除 swap 后的 D1≥D2≥D3≥D4 校验逻辑，改为：swap 完成后按 `combinedUtr` 降序重排 4 个 pairs 并重新赋予位置 D1/D2/D3/D4
- [x] 9.2 移除行内错误提示（不再有约束违反错误）

## 10. 前端：useLineup.js 和 LineupGenerator.vue 更新

- [x] 10.1 在 `useLineup.js` 的 `generateLineup` 中恢复 `includePlayers` 参数（默认 `[]`），加入请求体
- [x] 10.2 在 `LineupGenerator.vue` 中更新 `constraints` ref：加入 `includePlayers: []`；在 `generate()` 函数中传递给 `generateLineup`
- [x] 10.3 `PlayerConstraintSelector` 的 `update:constraints` 事件处理：同步更新 `constraints.includePlayers`

## 11. 前端测试更新

- [x] 11.1 更新 `PlayerConstraintSelector.test.js`：覆盖下拉选项（7个值）、性别徽章显示、新 emit payload（含 includePlayers）、摘要行三计数
- [x] 11.2 更新 `LineupCard.test.js`：覆盖 player1Gender/player2Gender 文字显示
- [x] 11.3 更新 `LineupSwapPanel.test.js`：覆盖 swap 后自动重排逻辑（D4 变 D1 case）；删除"显示错误信息"测试
- [x] 11.4 更新 `useLineup.test.js`：覆盖 includePlayers 参数出现在请求体
- [x] 11.5 更新 `LineupGenerator.test.js`：验证 includePlayers 传递给 generateLineup

## 12. 测试运行验证

- [x] 12.1 运行后端测试 `mvn test`，全部通过（无失败）
- [x] 12.2 运行前端测试 `npm test`，全部通过（无失败）
