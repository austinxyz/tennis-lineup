## MODIFIED Requirements

### Requirement: Player row mobile collapsible display
系统 SHALL 在 Mobile (`< 1024px`) 下用卡片列表形式显示球员，每行显示 `[性别徽章] 姓名 UTR[●Verified] 实:UTR`，点击行展开显示 Verified Doubles UTR、Notes、编辑/删除操作。桌面端保持现有表格显示。

#### Scenario: Mobile 球员行折叠态
- **WHEN** 用户在 Mobile 下打开队伍详情页
- **THEN** 每名球员显示为紧凑卡片行：粉/蓝性别徽章 + 加粗姓名 + UTR（带 Verified 绿点）+ 橙色实际 UTR（仅当 actualUtr 存在且不等于 utr 时显示）+ 右侧 ▸ 展开箭头

#### Scenario: 点击行展开详情
- **WHEN** 用户点击球员行
- **THEN** 行展开，箭头变 ▾，显示：Verified Doubles UTR 值 + 性别中文 + Notes 文字（白色框）+「编辑」「删除」按钮；背景浅绿色

#### Scenario: 再次点击折叠
- **WHEN** 用户再次点击已展开的行
- **THEN** 折叠回紧凑态

#### Scenario: 桌面端保留表格
- **WHEN** 视口 `>= 1024px`
- **THEN** 显示现有表格（列：姓名/性别/UTR/Verified Doubles UTR/Notes/操作），Mobile 卡片视图隐藏
