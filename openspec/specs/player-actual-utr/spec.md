## Requirements

### Requirement: Player model includes actualUtr field
The Player data model SHALL include an optional `actualUtr` field (Double, nullable) representing the captain's assessment of a player's real doubles ability. When `actualUtr` is null, the system SHALL treat it as equal to `utr` for all downstream computations. The API SHALL accept and return `actualUtr` on all player endpoints.

#### Scenario: Add player with actualUtr
- **WHEN** user submits player data with `actualUtr: 7.0` and `utr: 6.48`
- **THEN** system stores player with both fields set
- **THEN** response includes `actualUtr: 7.0`

#### Scenario: Add player without actualUtr
- **WHEN** user submits player data without `actualUtr` field
- **THEN** system stores player with `actualUtr: null`
- **THEN** system treats effective actualUtr as equal to `utr` in all computations

#### Scenario: Update player actualUtr
- **WHEN** user submits PUT request with `actualUtr: 5.5` for an existing player
- **THEN** system updates `actualUtr` to 5.5
- **THEN** all other fields remain unchanged

#### Scenario: Clear actualUtr
- **WHEN** user submits PUT request with `actualUtr: null`
- **THEN** system stores `actualUtr` as null
- **THEN** system reverts to using `utr` as effective actualUtr

#### Scenario: actualUtr range validation
- **WHEN** user submits `actualUtr: 17.0`
- **THEN** system returns 400 with message "实际UTR必须在0.0到16.0之间"
- **WHEN** user submits `actualUtr: -1.0`
- **THEN** system returns 400 with message "实际UTR必须在0.0到16.0之间"

#### Scenario: Existing player data without actualUtr field is backward compatible
- **WHEN** `tennis-data.json` contains player records without `actualUtr` field
- **THEN** system reads them successfully with `actualUtr: null`
- **THEN** no error or data migration is required

---

### Requirement: PlayerForm supports actualUtr input
The add/edit player form SHALL include an optional `actualUtr` number input field. It SHALL appear below the `utr` field with a placeholder indicating that leaving it blank defaults to the official UTR.

#### Scenario: Add player form shows actualUtr input
- **WHEN** user opens the add player modal
- **THEN** an optional "实际UTR" input field is shown below "UTR" with placeholder "默认同UTR（选填）"

#### Scenario: Submit form without actualUtr
- **WHEN** user leaves the actualUtr input empty and submits
- **THEN** the request is sent with `actualUtr: null`
- **THEN** form submits successfully without validation error

#### Scenario: Edit player pre-fills actualUtr
- **WHEN** user opens the edit player modal for a player with `actualUtr: 6.5`
- **THEN** the actualUtr input is pre-filled with 6.5

#### Scenario: Edit player clears actualUtr
- **WHEN** user opens the edit player modal for a player with `actualUtr: 6.5` and clears the input
- **THEN** the request is sent with `actualUtr: null`
