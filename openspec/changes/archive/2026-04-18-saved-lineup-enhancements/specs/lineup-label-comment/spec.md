## ADDED Requirements

### Requirement: Name and comment on a saved lineup
系统 SHALL 允许用户给每套已保存排阵设置自定义名称（label）和文字备注（comment），两者均可选，持久化到后端。

`Lineup` 模型新增：
- `label: String`（nullable，最多 50 字符）— 显示名，优先于策略名
- `comment: String`（nullable，最多 200 字符）— 备注文字

#### Scenario: 编辑排阵名称
- **WHEN** 用户点击已保存排阵 card 上的名称区域（或铅笔图标）
- **THEN** 名称变为可编辑输入框，失焦或回车后调用 PATCH 保存

#### Scenario: 名称为空时回退到策略名
- **WHEN** label 为空或 null
- **THEN** card 头部显示 `lineup.strategy` 作为名称

#### Scenario: 添加/编辑备注
- **WHEN** 用户点击「添加备注」或现有备注文字
- **THEN** 展开多行文本输入框，失焦后调用 PATCH 保存

#### Scenario: 备注为空时不显示
- **WHEN** comment 为 null 或空字符串
- **THEN** 显示「添加备注」占位入口，不显示空白区域
