## Why

The current opponent analysis page generates new lineup candidates on demand and compares them against the opponent. But users often already have saved lineups that have been carefully reviewed. They want to know: "of my saved lineups, which ones can beat this opponent lineup?" — without generating anything new.

## What Changes

- New API endpoint `POST /api/lineups/matchup` accepts own team ID + own lineup ID + opponent lineup ID, runs the UTR comparison algorithm, and returns the full per-line analysis and expected score for that specific matchup
- New API endpoint `GET /api/lineups/matchup/all?teamId=X&opponentLineupId=Y` runs the matchup for **all saved lineups** of the own team against the opponent lineup, returning all results sorted by expected score descending
- Opponent analysis page gains a second tab/mode: **"已保存排阵比较"** — shows own team lineup selector + opponent lineup selector, and displays all own saved lineups ranked by how well they match up against the opponent
- Each saved lineup in the result shows: lineup creation date, per-line win labels, total expected score, and a "能赢" / "势均力敌" / "劣势" summary badge

## Capabilities

### New Capabilities
- `saved-lineup-matchup`: Compare saved lineups from own team against an opponent's saved lineup using UTR win probability scoring

### Modified Capabilities
- `opponent-analysis`: Page gains a second mode showing saved lineup comparison results alongside the existing generate-and-recommend mode

## Impact

- **Backend**: New `LineupMatchupService`, new endpoint in `LineupController`
- **Frontend**: `OpponentAnalysis.vue` gains a mode toggle and saved lineup comparison panel; new `useSavedLineupMatchup.js` composable
- **No model changes**: Reuses `LineAnalysis`, `UtrRecommendation`, existing `Lineup` model
- **Risk**: Own team may have no saved lineups — handled with empty state message
