## Requirements

### Requirement: Manual save of a lineup
The system SHALL expose `POST /api/teams/{id}/lineups` to persist a user-selected lineup. Generated lineups are NOT auto-saved; the user must explicitly choose to save one.

#### Scenario: Lineup saved on explicit request
- **WHEN** user clicks "保留此排阵" on a lineup card
- **THEN** the frontend calls `POST /api/teams/{id}/lineups` with the current lineup (including any manual swap adjustments)
- **AND** the system persists the lineup and returns it with a unique `id` (format: `"lineup-" + nanoTime`) and `createdAt` ISO 8601 timestamp

#### Scenario: Generated candidates not auto-saved
- **WHEN** `POST /api/lineups/generate` returns successfully
- **THEN** no lineup is automatically written to `team.lineups`; persistence only happens via explicit save

---

### Requirement: Retrieve lineup history for a team
The system SHALL expose `GET /api/teams/{id}/lineups` to return all lineups for a team, ordered by `createdAt` descending (newest first). Each pair SHALL be enriched with the player's **current** `utr` and `gender` from the team roster (always overwritten from live data, not stored values). Each lineup SHALL include `currentValid` and `currentViolations` computed at request time — these fields are NOT persisted.

#### Scenario: Returns lineups in reverse chronological order
- **WHEN** a team has multiple saved lineups
- **THEN** `GET /api/teams/{id}/lineups` returns them with most recent first

#### Scenario: Returns empty array for team with no lineups
- **WHEN** a team has no saved lineups
- **THEN** `GET /api/teams/{id}/lineups` returns `[]` with HTTP 200

#### Scenario: Team not found
- **WHEN** `GET /api/teams/{nonexistent-id}/lineups` is called
- **THEN** system returns HTTP 404

#### Scenario: Pairs always reflect current player UTR
- **WHEN** `GET /api/teams/{id}/lineups` is called after a player's UTR has changed
- **THEN** each Pair in the response has `player1Utr` and `player2Utr` set to the player's **current** UTR (not the UTR at time of save)
- **AND** each pair's `combinedUtr` is recalculated from current UTR values
- **AND** each lineup's `totalUtr` is recalculated as the sum of all current pair UTRs

#### Scenario: Valid lineup after UTR change
- **WHEN** a saved lineup still satisfies all hard constraints with current player UTRs
- **THEN** the lineup SHALL have `currentValid: true` and `currentViolations: []`

#### Scenario: Invalid lineup after UTR change
- **WHEN** a saved lineup violates one or more hard constraints with current player UTRs (e.g., total UTR now exceeds 40.5)
- **THEN** the lineup SHALL have `currentValid: false` and `currentViolations` SHALL list the specific violated constraints

---

### Requirement: Delete a lineup
The system SHALL expose `DELETE /api/lineups/{id}` to remove a lineup from storage.

#### Scenario: Successful deletion
- **WHEN** `DELETE /api/lineups/{id}` is called with an existing lineup ID
- **THEN** system removes the lineup from the owning team's `lineups` array, returns HTTP 204 No Content

#### Scenario: Lineup not found
- **WHEN** `DELETE /api/lineups/{id}` is called with a non-existent ID
- **THEN** system returns HTTP 404 with `{ "code": "NOT_FOUND", "message": "排阵不存在" }`

---

### Requirement: Lineup history frontend page
The system SHALL provide a frontend page at `/teams/:id/lineups` listing all saved lineups for a team with delete capability, full player detail display, and validity indicators.

#### Scenario: History page shows saved lineups with enriched detail
- **WHEN** user navigates to `/teams/:id/lineups`
- **THEN** each saved lineup is rendered as a card showing: strategy, total UTR (current), creation timestamp, and all 4 pairs with position, player names, individual UTRs (current), gender badges, and combined UTR (current)

#### Scenario: History page shows validity status
- **WHEN** user navigates to `/teams/:id/lineups`
- **THEN** each lineup card SHALL display a visual indicator: green badge "合法" when `currentValid: true`, red badge "已失效" when `currentValid: false`
- **AND** when `currentValid: false`, the card SHALL show the violation reasons from `currentViolations`

#### Scenario: History page empty state
- **WHEN** the team has no saved lineups
- **THEN** page displays "暂无保存的排阵"

#### Scenario: Delete from history page
- **WHEN** user clicks delete on a lineup card and confirms
- **THEN** system calls `DELETE /api/lineups/{id}` and removes the card from the list without a full page reload

#### Scenario: Delete error shown inline
- **WHEN** delete call returns an error (404 or network failure)
- **THEN** an error message is shown; the lineup list remains unchanged

---

### Requirement: Navigation link to lineup history
The lineup generation page SHALL display a link to the lineup history page when a team is selected.

#### Scenario: Link visible after team selection
- **WHEN** user selects a team on the lineup generation page
- **THEN** a "查看已保存排阵 →" link is shown, pointing to `/teams/:id/lineups`

#### Scenario: Link hidden when no team selected
- **WHEN** no team is selected
- **THEN** the link is not visible

---

### Requirement: Saved lineup history page supports inline editing
已保存排阵页面 SHALL 在每张排阵 card 上提供内联编辑入口，包含：替换球员、swap 配对、命名/备注、上移/下移。所有操作均在 card 内完成，无需跳转新页面。

#### Scenario: 各编辑功能入口可见
- **WHEN** 用户打开已保存排阵页面
- **THEN** 每张排阵 card 显示：编辑按钮（铅笔）、「调整配对」展开区、备注入口、上移/下移箭头、删除按钮

#### Scenario: 编辑操作保存后刷新校验状态
- **WHEN** 任意编辑操作（替换、swap、命名）保存完成
- **THEN** 列表重新获取该排阵数据，`currentValid`/`currentViolations` 反映最新球员 UTR

---

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

---

### Requirement: Lineup history mobile single column
系统 SHALL 在 Mobile 下以单列方式（`grid-cols-1`）堆叠已保存排阵卡片。桌面端保持 `lg:grid-cols-2`。

#### Scenario: Mobile 单列
- **WHEN** 视口 `< 1024px`
- **THEN** 已保存排阵页面所有卡片垂直单列堆叠，每张卡占满宽

#### Scenario: 桌面双列
- **WHEN** 视口 `>= 1024px`
- **THEN** 双列网格（现有行为）
