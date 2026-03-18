## ADDED Requirements

### Requirement: Prevent duplicate lineup saves
The system SHALL reject a save request if an identical lineup already exists for the same team. Two lineups are considered identical if the set of 8 player IDs across all pairs is the same (order-insensitive).

#### Scenario: Duplicate lineup rejected
- **WHEN** `POST /api/teams/{id}/lineups` is called with a lineup whose 8 player IDs match an already-saved lineup for that team
- **THEN** system returns HTTP 400 with `{ "code": "VALIDATION_ERROR", "message": "该排阵已保存，请勿重复保存" }`

#### Scenario: Non-duplicate lineup accepted
- **WHEN** `POST /api/teams/{id}/lineups` is called with a lineup whose player set differs from all existing saved lineups
- **THEN** system saves the lineup and returns HTTP 200 with the saved lineup object

#### Scenario: First save always accepted
- **WHEN** a team has no saved lineups and a save request is made
- **THEN** system saves the lineup normally (no duplicate possible)
