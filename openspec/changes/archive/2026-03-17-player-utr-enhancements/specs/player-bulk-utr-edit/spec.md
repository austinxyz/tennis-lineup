## ADDED Requirements

### Requirement: Bulk UTR edit mode in player list
The system SHALL provide a bulk edit mode on the team player list page, allowing users to edit all players' UTR values inline and save all changes in one action.

#### Scenario: Enter bulk edit mode
- **WHEN** user clicks the「批量编辑 UTR」button on the player list page
- **THEN** each player row shows their UTR as an editable number input (step=0.01, min=0, max=16)
- **THEN** the button changes to「保存」and a「取消」button appears

#### Scenario: Edit UTR values inline
- **WHEN** user is in bulk edit mode and modifies one or more UTR inputs
- **THEN** modified inputs are visually highlighted (e.g., border color change)
- **THEN** no API calls are made until user clicks「保存」

#### Scenario: Save bulk changes — all succeed
- **WHEN** user clicks「保存」after modifying UTR values for N players
- **THEN** system sends concurrent PUT requests only for players whose UTR actually changed
- **THEN** on all success, bulk edit mode exits and player list reflects new UTR values

#### Scenario: Save bulk changes — partial failure
- **WHEN** some PUT requests fail during bulk save
- **THEN** system displays a list of failed player names with their error messages
- **THEN** successfully updated players show new UTR values
- **THEN** failed players remain highlighted in edit mode for user to retry

#### Scenario: Cancel bulk edit mode
- **WHEN** user clicks「取消」without saving
- **THEN** all UTR inputs revert to original values
- **THEN** bulk edit mode exits, list returns to normal view

#### Scenario: Navigate away with unsaved changes
- **WHEN** user attempts to navigate to another route while in bulk edit mode with unsaved changes
- **THEN** system shows a confirmation dialog: "有未保存的 UTR 修改，确定离开吗？"
- **THEN** if user confirms, navigation proceeds and changes are discarded
- **THEN** if user cancels, navigation is blocked and user stays on the page

#### Scenario: No changes made — save button behavior
- **WHEN** user enters bulk edit mode but does not modify any UTR
- **THEN** clicking「保存」exits edit mode immediately without making any API calls
