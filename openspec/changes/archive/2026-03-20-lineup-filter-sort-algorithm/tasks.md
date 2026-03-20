## 1. 探索现有排阵生成代码

- [x] 1.1 找到 `LineupService`（或等价类）和配对枚举入口，理解当前8人子集选择实现

## 2. 实现三阶段候选池构建

- [x] 2.1 在服务层新增 `buildCandidatePool(players, excludePlayers, includePlayers, pinPlayers)` 方法
- [x] 2.2 Phase 1：从完整名单中移除 `excludePlayers`
- [x] 2.3 Phase 2：将 `includePlayers ∪ pinPlayers.keys()` 加入 locked 集，校验 locked.size ≤ 8
- [x] 2.4 Phase 3：剩余球员按接近40.5贪心排序补充，直到队列满8人
- [x] 2.5 替换原有 C(n,8) 子集枚举逻辑，改为调用 `buildCandidatePool()`

## 3. 实现 Top20/Top40 截断

- [x] 3.1 在配对候选生成后，按组合UTR降序排序所有有效对子（满足 |UTR差| ≤ 3.5）
- [x] 3.2 D1/D2 位置仅从 top20 对子中选择
- [x] 3.3 D3/D4 位置使用全部有效对子（≤ 28，相当于 top40 全覆盖）

## 4. 贪心回退兜底

- [x] 4.1 当初始8人池回溯全部失败时，尝试替换最后一个贪心补充球员，使用 remaining 列表中的下一个候选
- [x] 4.2 若回退后仍无有效排阵，返回 400 "无法生成满足约束的排阵"

## 5. 后端单元测试

- [x] 5.1 测试 `buildCandidatePool`：无约束时，8人池按接近40.5贪心选择
- [x] 5.2 测试 `buildCandidatePool`：excludePlayers 被正确移除
- [x] 5.3 测试 `buildCandidatePool`：includePlayers + pinPlayers 优先进入队列
- [x] 5.4 测试 `buildCandidatePool`：locked 超过8人时返回400
- [x] 5.5 测试 top20 截断：D1/D2 候选仅来自 top20 对子
- [x] 5.6 测试贪心回退：初始池无效时换最后一个球员后找到合法排阵
- [x] 5.7 运行全量后端测试，确认 70 个已有测试全部通过

## 6. 测试报告

- [x] 6.1 运行 `JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 mvn test` 并记录测试报告（通过数 / 总数 / 失败明细）

## 7. 重启后端

- [x] 7.1 停止当前运行的 Spring Boot 进程，重新执行 `mvn spring-boot:run` 使代码变更生效
