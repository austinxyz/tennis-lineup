## ADDED Requirements

### Requirement: Position pin constraint on generation request
The system SHALL accept a `pinPlayers` map in the generate lineup request that assigns specific players to specific positions (D1–D4). All returned lineup candidates MUST honor these assignments.

#### Scenario: Single player pinned to D1
- **WHEN** client posts `{ "teamId": "t1", "strategyType": "preset", "preset": "balanced", "pinPlayers": { "p1": "D1" } }`
- **THEN** every lineup in the response has player "p1" appearing in the D1 pair

#### Scenario: Multiple players pinned to different positions
- **WHEN** client posts `pinPlayers: { "p1": "D1", "p3": "D3" }`
- **THEN** every lineup in the response places "p1" in D1 and "p3" in D3

#### Scenario: Pin conflicts with exclude list → 400
- **WHEN** a player ID appears in both `pinPlayers` and `excludePlayers`
- **THEN** system returns HTTP 400 with `{ "code": "VALIDATION_ERROR", "message": "同一球员不能同时被固定位置和排除" }`

#### Scenario: No valid lineup satisfies position pin → 400
- **WHEN** the pinned player's UTR is incompatible with the requested position (e.g., lowest-UTR player pinned to D1 violates ordering constraint)
- **THEN** system returns HTTP 400 with `{ "code": "VALIDATION_ERROR", "message": "无法生成满足位置约束的排阵" }`

#### Scenario: Invalid position value → 400
- **WHEN** a `pinPlayers` value is not one of "D1", "D2", "D3", "D4"
- **THEN** system returns HTTP 400 with `{ "code": "VALIDATION_ERROR", "message": "位置必须为 D1/D2/D3/D4" }`
