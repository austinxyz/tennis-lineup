## ADDED Requirements

### Requirement: Multi-candidate lineup generation is preserved
The system SHALL continue to return a JSON array of 1–6 ranked lineup candidates from `POST /api/lineups/generate`. The response type for this endpoint SHALL NOT be changed to a single lineup object, and the frontend SHALL continue to render all returned candidates simultaneously so users can compare options before saving. This requirement is a protective guarantee — any future change that would reduce the response to a single candidate or hide secondary candidates SHALL be treated as a breaking change and require its own proposal.

#### Scenario: Generate endpoint returns a JSON array
- **WHEN** the client posts a valid generate request to `POST /api/lineups/generate`
- **THEN** the response body is a JSON array (`[...]`), not a single lineup object

#### Scenario: Up to 6 candidates returned when available
- **WHEN** the roster and strategy produce 6 or more valid candidate lineups after server-side deduplication
- **THEN** the array length is exactly 6

#### Scenario: Frontend renders all returned candidates
- **WHEN** the generate response contains N candidates (1 ≤ N ≤ 6)
- **THEN** the lineup generator page renders N lineup cards, each independently inspectable by the user, without tab navigation or other UI that hides secondary candidates
