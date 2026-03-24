## Context

The backend for partner notes is fully implemented: `PartnerNote` model, `PartnerNoteService`, `PartnerNoteController`, bulk personal notes API, and AI prompt integration in `ZhipuAiService`. The `usePartnerNotes.js` composable and the original UI components (`PlayerNotesEditor.vue`, `PartnerNotesEditor.vue`) also exist.

The original UI placed all notes in a collapsible "队员笔记" section below the player table. After user feedback, the design was revised: notes should live inside the player list table itself, always visible, without requiring any scroll or expand just to see them.

## Goals / Non-Goals

**Goals:**
- Show 个人备注 inline in the player table (read-only, always visible)
- Show partner note chips inline in the player table (full text, no truncation, wrap on multiple lines)
- Bulk edit personal notes with a single button + save
- Expand a player row to edit that player's partner notes (add, edit, delete)
- Retain `PartnerNotesEditor.vue` unchanged for the opponent analysis page

**Non-Goals:**
- Backend changes (all APIs already implemented)
- Changes to `OpponentAnalysis.vue` or `PartnerNotesEditor.vue`
- AI integration changes (already implemented)

## Decisions

**Decision 1: Notes integrated into table, not a separate section**
The "队员笔记" collapsible section is removed entirely. Two new columns are added to the player table: 个人备注 (text, inline) and 搭档笔记 (chips). This puts notes at a glance without any interaction cost.

**Decision 2: Partner notes as chips — full text, row height grows**
Each chip shows `搭档名: 笔记内容` with no truncation. If a player has many partners, the row height grows. This is acceptable given typical team sizes (8–16 players) and note lengths.

**Decision 3: Expandable row for partner notes editing**
Only one row can be expanded at a time. Clicking ▶ or the `+ 添加搭档笔记` chip opens the editor inline below the row. The editor manages add/edit/delete of partner notes and emits `saved` on success; the parent re-fetches and refreshes the chips.

**Decision 4: Bulk Notes mode is mutually exclusive with Bulk UTR mode**
Entering one bulk edit mode while the other is active is prevented. Both share the same top-bar button area and a single-line save/cancel flow.

**Decision 5: New component `PlayerPartnerNotesRow.vue`**
The expandable editor for a single player's partner notes is extracted into its own component. It receives `notes` as a prop and watches it to re-initialize on parent refresh. It handles all partner notes API calls internally and emits `saved` when all succeed.

**Decision 6: `PlayerNotesEditor.vue` deleted**
Its functionality (bulk personal notes editing) is absorbed directly into `TeamDetail.vue` as a bulk edit mode on the table — simpler, and eliminates a full-page component just for text inputs.

**Decision 7: Partner notes fetched once on mount, keyed by both player IDs**
`GET /api/teams/{id}/partner-notes` is called once on mount. The result is stored in `partnerNotesMap` where each note is indexed under both `player1Id` and `player2Id`, so looking up a player's chips is O(1).

**Decision 8: Partial save failure is non-blocking**
If DELETE/PUT/POST calls partially fail during a partner notes save, successful operations are not rolled back. An inline error is shown and the panel stays open. The user can correct and re-save.

## Risks / Trade-offs

- **Wide table**: Adding two columns makes the table wider. On narrow screens it may require horizontal scroll. Acceptable for this app (team captain on desktop/tablet).
- **Row height variance**: Players with many partner notes will have taller rows, making the table uneven. Acceptable given typical usage (most players have 0–3 partners).
- **Stale chips after partial failure**: If a save partially succeeds, chips won't update until a full success. Chips may appear stale temporarily. Low severity.
