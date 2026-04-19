## MODIFIED Requirements

### Requirement: Lineup card player row layout
系统 SHALL 在 `LineupCard` 组件内对每名球员独立一行显示：`[性别徽章] 姓名(加粗) UTR 实:实际UTR`。每对（D1-D4）包含 2 行（上下堆叠 player1 / player2）。性别用粉色 `女` / 蓝色 `男` 小徽章。姓名加粗 `flex-1`，UTR 灰色小字，实际 UTR 橙色加粗（仅在 actualUtr 存在且与 utr 不同时显示）。

#### Scenario: 每对显示两行球员
- **WHEN** 渲染 LineupCard 中的任意 pair
- **THEN** D1 行：pos 标签 + player1（性别/姓名/UTR/实际UTR）
         紧接下一行：player2（性别/姓名/UTR/实际UTR）

#### Scenario: 实际UTR 只在不同时显示
- **WHEN** player.actualUtr == player.utr 或 player.actualUtr 为 null
- **THEN** 不显示 `实:` 小字段

#### Scenario: 性别徽章颜色
- **WHEN** player.gender == 'female'
- **THEN** 徽章背景粉色 `bg-pink-100`，文字深粉 `text-pink-700`，内容"女"
- **WHEN** player.gender == 'male'
- **THEN** 徽章背景蓝色 `bg-blue-100`，文字深蓝 `text-blue-700`，内容"男"

### Requirement: Lineup history mobile single column
系统 SHALL 在 Mobile 下以单列方式（`grid-cols-1`）堆叠已保存排阵卡片。桌面端保持 `lg:grid-cols-2`。

#### Scenario: Mobile 单列
- **WHEN** 视口 `< 1024px`
- **THEN** 已保存排阵页面所有卡片垂直单列堆叠，每张卡占满宽

#### Scenario: 桌面双列
- **WHEN** 视口 `>= 1024px`
- **THEN** 双列网格（现有行为）
