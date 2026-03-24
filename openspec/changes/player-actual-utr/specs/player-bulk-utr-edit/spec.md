## MODIFIED Requirements

### Requirement: Bulk UTR edit mode in player list
The system SHALL provide a bulk edit mode on the team player list page, allowing users to edit all players' UTR and actualUtr values inline and save all changes in one action. Each player row SHALL show two editable inputs: one for `utr` and one for `actualUtr` (optional, placeholder "默认同UTR").

#### Scenario: Enter bulk edit mode
- **WHEN** user clicks the「批量编辑 UTR」button on the player list page
- **THEN** each player row shows two editable number inputs: one for UTR (step=0.01, min=0, max=16) and one for 实际UTR (step=0.01, min=0, max=16, optional)
- **THEN** the button changes to「保存」and a「取消」button appears

#### Scenario: Edit UTR values inline
- **WHEN** user is in bulk edit mode and modifies one or more UTR or actualUtr inputs
- **THEN** modified inputs are visually highlighted (e.g., border color change)
- **THEN** no API calls are made until user clicks「保存」

#### Scenario: Save bulk changes — all succeed
- **WHEN** user clicks「保存」after modifying UTR and/or actualUtr values for N players
- **THEN** system sends concurrent PUT requests only for players where at least one of `utr` or `actualUtr` actually changed
- **THEN** on all success, bulk edit mode exits and player list reflects new values

#### Scenario: Save bulk changes — partial failure
- **WHEN** some PUT requests fail during bulk save
- **THEN** system displays a list of failed player names with their error messages
- **THEN** successfully updated players show new values
- **THEN** failed players remain highlighted in edit mode for user to retry

#### Scenario: Cancel bulk edit mode
- **WHEN** user clicks「取消」without saving
- **THEN** all UTR and actualUtr inputs revert to original values
- **THEN** bulk edit mode exits, list returns to normal view

#### Scenario: Navigate away with unsaved changes
- **WHEN** user attempts to navigate to another route while in bulk edit mode with unsaved changes
- **THEN** system shows a confirmation dialog: "有未保存的 UTR 修改，确定离开吗？"
- **THEN** if user confirms, navigation proceeds and changes are discarded
- **THEN** if user cancels, navigation is blocked and user stays on the page

#### Scenario: No changes made — save button behavior
- **WHEN** user enters bulk edit mode but does not modify any UTR or actualUtr
- **THEN** clicking「保存」exits edit mode immediately without making any API calls

#### Scenario: actualUtr input blank means null
- **WHEN** user leaves the actualUtr input empty for a player and saves
- **THEN** system sends `actualUtr: null` for that player
- **THEN** effective actualUtr falls back to official `utr`
