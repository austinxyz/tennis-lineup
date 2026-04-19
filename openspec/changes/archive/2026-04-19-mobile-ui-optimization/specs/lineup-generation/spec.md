## MODIFIED Requirements

### Requirement: Lineup generator mobile form layout
系统 SHALL 在 Mobile 下将排阵生成表单重组为垂直单列：队伍下拉 → 策略按钮（3 个横排）→ 可折叠的「固定位置」「包含球员」「排除球员」三个 `<details>` 区块（每个带已选数量徽章）→ 全宽主按钮「生成排阵」。桌面端保持现有两栏/多列布局。

#### Scenario: Mobile 表单单列
- **WHEN** 视口 `< 1024px`
- **THEN** 所有表单字段垂直堆叠，宽度填满容器

#### Scenario: 高级选项默认折叠
- **WHEN** 用户进入 Mobile 排阵生成页
- **THEN** 固定位置/包含/排除三个区域默认折叠，显示"▾ 标题 [数量徽章]"

#### Scenario: 折叠区点击展开
- **WHEN** 用户点击折叠区标题
- **THEN** 展开内部内容（球员列表 / chip 列表）

### Requirement: Lineup generator results mobile single column
系统 SHALL 在 Mobile 下以单列堆叠显示所有生成的排阵候选（方案 1 - N）。每个方案独占一列卡片，顶部显示"方案 N"标签（第一方案额外带 `⭐ 最佳` 徽章），卡片内部与已保存排阵卡片采用同一布局（每名球员一行）。卡片底部提供`[保留此排阵]`主按钮 + `[调整配对 ▾]`次按钮。

#### Scenario: Mobile 结果单列
- **WHEN** 视口 `< 1024px` 且有生成结果
- **THEN** 所有方案卡片垂直单列显示

#### Scenario: 方案 1 标记最佳
- **WHEN** 渲染第一个方案卡片
- **THEN** 卡片上方显示"方案 1 ⭐ 最佳"标签，卡片边框为双倍绿色

#### Scenario: AI 优选 vs 启发式 标识
- **WHEN** 方案卡 lineup.aiUsed === true
- **THEN** 标题旁显示紫色"AI 优选"徽章
- **WHEN** lineup.aiUsed === false
- **THEN** 显示灰色"启发式"徽章
