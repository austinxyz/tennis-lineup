## Why

Team management currently has no UI for editing player notes, even though the `Player` model has a `notes` field. There is also no way to record chemistry or history between specific partners (e.g., who played together in a team training session, a USTA match, what level they played at, how the pairing worked, win rate, etc.). Coaches and team captains need this information to make informed lineup decisions.

Additionally, the AI lineup advisor has no awareness of which player pairs work well together or have a proven track record. Partner notes should feed into the AI prompt so recommendations are grounded in real observed chemistry, not just UTR scores. The same data need applies to opponent teams — understanding an opponent's known strong or weak pairings can inform strategy just as much as own-team data.

The initial implementation added notes in a separate collapsible "队员笔记" section below the player table, which users found inconvenient — requiring a scroll and an expand to access. This change redesigns the UX to integrate notes directly into the player list table.

## What Changes

1. **Personal notes inline in player table**: 个人备注 is shown as a column directly in the player list row — always visible without expanding anything. A "批量编辑 Notes" button at the top enables editing all players' notes at once.
2. **Partner notes chips in player table**: Partner notes are shown as inline chips (「搭档名: 笔记内容」) in a dedicated column, visible without expanding. Clicking ▶ or a chip opens an expandable row editor for that player's partner notes.
3. **Partner notes (opponent teams)**: The `PartnerNotesEditor.vue` component (used on the opponent analysis page) is unchanged — scouting notes for opponents remain accessible there.
4. **AI integration**: When the AI lineup advisor is invoked, own-team partner notes and relevant opponent partner notes are included in the prompt (already implemented in the backend; frontend fetches and passes them).

## Capabilities

### New Capabilities
- `partner-notes`: Data model and CRUD UI for recording notes about a specific pair of players — available for both own and opponent teams.

### Modified Capabilities
- `team-management`: Player table gains 个人备注 and 搭档笔记 columns; bulk Notes edit mode; expandable row for per-player partner notes editing. Replaces the previous "队员笔记" collapsible section.
- `opponent-analysis`: AI advisor call includes own-team and opponent partner notes in the prompt; opponent partner notes editable via `PartnerNotesEditor` in the opponent analysis page.

## Impact

- **Backend**: `PartnerNote` entity (embedded in `Team` JSON), REST endpoints (`GET/POST/PUT/DELETE /api/teams/{id}/partner-notes`, `PATCH /api/teams/{id}/players/notes`), `ZhipuAiService` extended with partner notes context in AI prompt. *(Already implemented — no further backend changes.)*
- **Frontend**: Remove separate "队员笔记" section from `TeamDetail.vue`. Add 个人备注 column and 搭档笔记 chips column to the player table. New `PlayerPartnerNotesRow.vue` component for the expandable partner notes editor. Delete `PlayerNotesEditor.vue` (replaced by inline bulk edit). Keep `PartnerNotesEditor.vue` unchanged for `OpponentAnalysis.vue`.
- **Data model**: No changes — `PartnerNote` and `Player.notes` already exist.
- No breaking changes to existing APIs.
