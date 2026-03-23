# Player Notes UX Redesign

**Date:** 2026-03-23
**Status:** Ready for implementation

## Goal

Integrate player notes (personal notes and partner notes) directly into the player list table, removing the separate collapsible "队员笔记" section.

## Current State

- "队员笔记" is a separate collapsible section below the player table
- Personal notes and partner notes are in two separate sub-panels (`PlayerNotesEditor.vue`, `PartnerNotesEditor.vue`)
- Users must scroll down and expand the section to access any notes

## New Design

### Player Table Changes

Add two new visual elements to the player table:

1. **个人备注 column** — new column between "已验证" and "操作", shows note text inline. If no note, shows `—`.

2. **搭档笔记 chips column** — new column showing all partner notes as chips, one chip per partner. The chip label always shows the *other* player's name (not the current player): `搭档名: 笔记内容` (full text, no truncation). Multiple chips wrap to multiple lines; row height grows naturally.
   - No partner notes → shows a `+ 添加搭档笔记` chip (gray, clickable — opens the expandable row for that player)
   - While loading on mount → subtle gray skeleton placeholder
   - Fetch failure → `加载失败 重试` link; clicking it retries the fetch

Table column order:
```
[▶] 姓名 | 性别 | UTR | 已验证 | 个人备注 | 搭档笔记 chips | 操作
```

### Bulk Edit Notes Mode

A new "批量编辑 Notes" button (indigo) in the top button bar.

When clicked:
- Top bar switches to show "正在批量编辑个人备注" + "保存全部" + "取消"
- The 个人备注 column becomes an `<input>` in every row
- On "保存全部": calls `bulkUpdatePersonalNotes`, then exits mode on success. On failure: shows an error banner below the top bar ("保存失败，请重试"), stays in bulk edit mode so the user can retry
- On "取消": discards changes, exits mode
- UTR bulk edit and Notes bulk edit are mutually exclusive

### Expandable Row — Partner Notes Editor

Clicking ▶ (or the `+ 添加搭档笔记` chip) expands the row. Only one row open at a time — opening another auto-closes the current one.

**Panel contents:**
- Header: "搭档笔记"
- List of existing partner note rows, each: `[搭档 dropdown] [笔记 input] [✕]`
- One blank "add" row: `[选搭档… dropdown] [添加笔记… input]` (dashed border)
- "保存" + "取消" buttons

**Dropdown exclusion rules (same for all rows including the blank row):**
Exclude: (a) the current player themselves, and (b) any player already assigned as a partner in another row — including unsaved rows that have a partner selected. This is computed from the current local state of all rows, not from saved data.

**Save behavior:** On "保存":
1. Send DELETE for rows the user ✕-ed
2. Send PUT for rows with changed note text
3. Send POST for the blank row if both partner and note are filled
4. All calls are made; if *any* fail, show inline error ("保存部分失败，请检查") and keep the panel open — successful operations are not rolled back. The next retry will re-attempt only the remaining failed operations if the user edits and re-saves.
5. On full success: emit `saved`, parent re-fetches partner notes and rebuilds `partnerNotesMap`, then passes fresh `notes` prop to the component — component watches `notes` prop and re-initializes local state when it changes.
6. While saving: spinner on "保存" button, inputs disabled

On "取消": discard local edits, collapse row.

### Removed

- The "队员笔记" collapsible section in `TeamDetail.vue`
- `PlayerNotesEditor.vue` (functionality absorbed into inline bulk edit mode)

> **Note:** `PartnerNotesEditor.vue` is **NOT deleted** — it remains unchanged, still used by `OpponentAnalysis.vue`.

## Components

### Modified: `TeamDetail.vue`

- Add `个人备注` and `搭档笔记` columns
- `bulkNotesMode` + `bulkNoteValues` refs for Notes bulk edit
- `bulkNotesError` ref for save failure banner
- `expandedPlayerId` ref (only one row open at a time)
- `partnerNotesMap` ref: `{ [playerId]: PartnerNote[] }` — stores raw `PartnerNote` objects; each player appears in the map entry for *both* player1Id and player2Id
- `partnerNotesLoading` + `partnerNotesError` refs for mount fetch
- On `saved` from child: re-fetch and rebuild `partnerNotesMap`
- Remove `PlayerNotesEditor` import and the notes section

### New: `PlayerPartnerNotesRow.vue`

**Props:** `teamId`, `playerId`, `playerName`, `players` (full roster), `notes` (PartnerNote[] for this player)

**Emits:** `saved`

**Internal behavior:**
- `watch(() => props.notes, reinitLocalState, { immediate: true })` — re-initializes editable copy whenever parent passes fresh data
- Dropdown options computed from current local row state (excludes self + already-used partners across all rows including blank row)
- Tracks deletions locally; sends all API calls on save
- Partial failure: show inline error, keep panel open
- Full success: emit `saved`

### Keep unchanged: `PartnerNotesEditor.vue`

Used by `OpponentAnalysis.vue`. No changes.

### Delete: `PlayerNotesEditor.vue`

## Data Flow

- Mount: `GET /api/teams/{id}/partner-notes` → for each note, push into `partnerNotesMap[note.player1Id]` and `partnerNotesMap[note.player2Id]`
- Bulk notes: `PATCH /api/teams/{id}/players/notes`
- Partner CRUD: `POST / PUT / DELETE /api/teams/{id}/partner-notes[/{noteId}]`
- After `saved` emitted: parent re-fetches and rebuilds map, passes new `notes` prop to the open row component

## Tests

**Unit (Vitest):**
- Delete `PlayerNotesEditor.test.js`
- Add `PlayerPartnerNotesRow.test.js`: renders existing notes, dropdown excludes self + existing partners, ✕ marks row for deletion on save, blank row included in save when filled, `saved` emitted on success, error shown on failure, `notes` prop change re-initializes state

**E2E (Playwright) — update `player-partner-notes.spec.js`:**
- Remove tests referencing the "队员笔记" toggle section
- Add: 个人备注 column visible in table row; chips column visible; bulk Notes edit → save persists; expand row → add partner note → chip appears; fetch failure → retry link works
