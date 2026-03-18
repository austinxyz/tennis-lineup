## Why

The current lineup generator is a single-column page that shows only one result and gives no control over which players must or cannot participate. Teams need to see multiple lineup options simultaneously to compare and pick the best one, and coaches need to handle real-world constraints (player injuries, availability, tactical choices) by locking in or sitting out specific players.

## What Changes

- **Layout redesign**: Two-column layout — left column contains strategy selection + player constraint controls; right column displays up to 6 lineup results in a 3×2 tab grid
- **Multiple lineup results**: Backend returns top-N (up to 6) candidate lineups sorted by the selected strategy heuristic; frontend shows each as a tab in the result panel
- **Player inclusion constraints**: User can mark specific players as "必须上场" (must play); the algorithm only generates lineups that include all pinned players
- **Player exclusion constraints**: User can mark specific players as "排除" (sit out); the algorithm skips those players entirely when generating lineups

## Capabilities

### New Capabilities
- `lineup-multi-result`: Return up to 6 ranked lineup candidates from a single generate request; frontend displays them in a tabbed 3×2 grid
- `lineup-player-constraints`: Accept `includePlayers` and `excludePlayers` lists in the generate request; enforce inclusion/exclusion before candidate enumeration

### Modified Capabilities
- `lineup-generation`: Generate request gains `includePlayers: string[]` and `excludePlayers: string[]` fields; response changes from a single Lineup to a list of up to 6 Lineups

## Impact

- **Backend**: `GenerateLineupRequest` gains two new optional fields; `LineupService.generateAndSave` → new `generateMultiple` method returning `List<Lineup>` (up to 6); `LineupGenerationService` gains pre-filtering logic for include/exclude sets; `LineupController` returns `List<Lineup>` instead of single `Lineup`
- **Frontend**: `LineupGenerator.vue` refactored to two-column layout; `useLineup.js` updated to handle array response; `LineupCard.vue` remains unchanged; new `PlayerConstraintSelector.vue` component for inclusion/exclusion toggles per player; `LineupResultTabs.vue` for the 3×2 tab grid
- **API**: `POST /api/lineups/generate` response body changes from `Lineup` object to `Lineup[]` array — **BREAKING** change for existing callers
- **Tests**: All existing lineup tests need updating for multi-result response; new tests for constraint filtering
