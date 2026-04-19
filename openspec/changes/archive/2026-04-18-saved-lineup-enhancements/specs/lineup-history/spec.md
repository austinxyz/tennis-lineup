## MODIFIED Requirements

### Requirement: Saved lineup history page supports inline editing
已保存排阵页面 SHALL 在每张排阵 card 上提供内联编辑入口，包含：替换球员、swap 配对、命名/备注、上移/下移。所有操作均在 card 内完成，无需跳转新页面。

#### Scenario: 各编辑功能入口可见
- **WHEN** 用户打开已保存排阵页面
- **THEN** 每张排阵 card 显示：编辑按钮（铅笔）、「调整配对」展开区、备注入口、上移/下移箭头、删除按钮

#### Scenario: 编辑操作保存后刷新校验状态
- **WHEN** 任意编辑操作（替换、swap、命名）保存完成
- **THEN** 列表重新获取该排阵数据，`currentValid`/`currentViolations` 反映最新球员 UTR
