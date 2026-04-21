## Why

今天的「删除队伍」操作只做了一次简单 `confirm()` 提示，然后立即从 `tennis-data.json` 移除整条队伍记录。这会同时抹掉队伍下的所有球员、搭档笔记、约束预设以及保存的排阵方案 — 一次误触就会丢失大量难以恢复的工作数据（球员 UTR、Verified 标记、历史排阵等）。近期多次出现用户点到后需要从 JSON 备份里恢复的情况，风险已经实锤。

与此同时，「排阵生成返回多候选（最多 6 个方案）」这个能力必须保留 — 这是目前排阵页面的核心交互，用户会横向对比多个方案后再保存。改动 team 删除时不能顺带砍掉多候选。

## What Changes

- **BREAKING**（相对当前行为）：禁止删除非空队伍。只有当队伍同时满足 `players.length === 0` 且 `lineups.length === 0` 时才允许删除。任何一方非空 → 返回 `409 CONFLICT` / 前端弹窗阻止，并告知用户需先清空球员和保存的排阵。
- 后端 `TeamService.deleteTeam` 新增前置检查，返回 `TEAM_NOT_EMPTY` 错误码（message：`队伍中还有球员或已保存的排阵，无法删除`），HTTP 409。
- 前端 `TeamList.vue` 与 `TeamListPanel.vue` 的删除按钮行为调整：
  - 对非空队伍禁用或隐藏删除按钮（附 tooltip 说明原因）。
  - 若后端仍返回 409，前端 toast/alert 提示用户，不破坏当前视图。
- 明确保留 `lineup-multi-result` 当前「最多返回 6 个候选方案并并排展示」的能力 — 本次改动不触碰排阵生成流程，仅在 spec 中加一条显式条款阻止回退。

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

- `team-crud`: 修改 `Delete team` 需求 — 新增「拒绝删除非空队伍」约束及对应场景。
- `lineup-multi-result`: 新增一条保护性需求，显式声明多候选生成（1–6 个方案）必须保留，防止后续改动意外回退。

## Impact

- **Backend**
  - `backend/src/main/java/com/tennis/service/TeamService.java` — `deleteTeam` 增加空队伍校验。
  - `backend/src/main/java/com/tennis/exception/` — 新增 `TeamNotEmptyException` 或复用 `ValidationException`，并在 `GlobalExceptionHandler` 映射为 409 + `TEAM_NOT_EMPTY`。
  - `backend/src/test/java/com/tennis/service/TeamServiceTest.java` & `TeamControllerTest.java` — 覆盖新场景。
- **Frontend**
  - `frontend/src/views/TeamList.vue`、`frontend/src/components/TeamListPanel.vue` — 删除按钮禁用/提示逻辑。
  - `frontend/src/composables/useTeams.js` — 捕获 409 并透传错误信息。
  - `frontend/src/composables/__tests__/useTeams.test.js`、`frontend/src/components/__tests__/TeamListPanel.test.js` — 覆盖 409 路径和按钮禁用。
  - `frontend/e2e/tests/team-management.spec.js` — E2E 覆盖 "非空队伍无法删除" 与 "空队伍可删除"。
- **API contract**
  - `DELETE /api/teams/{id}` 行为变化：成功仍 204；非空 → 409 `{"code":"TEAM_NOT_EMPTY","message":"..."}`。
  - `docs/api.md` 需同步更新。
- **Dev log**
  - 实施完成后追加条目到 `docs/log/YYYY-MM-DD.md`。
- **不在范围内**
  - 不引入「软删除 / 归档」机制（后续如需可单开 proposal）。
  - 不改动 `lineup-generation` / `lineup-multi-result` 的实际生成逻辑，仅加 spec 防护条款。
