## MODIFIED Requirements

### Requirement: Read team data from JSON
The system SHALL read team data from the core JSON file (`tennis-data.json`).

#### Scenario: Successful data read
- **WHEN** system starts or needs to load data
- **THEN** system reads JSON file from the configured core data path (`storage.data-file`)
- **THEN** system parses JSON and returns TeamData object
- **THEN** system handles file lock for thread safety

#### Scenario: File does not exist
- **WHEN** JSON file does not exist on first run
- **THEN** system returns empty TeamData object
- **THEN** system creates file on next write

---

### Requirement: Write team data to JSON
The system SHALL write team data to the core JSON file atomically.

#### Scenario: Successful data write
- **WHEN** system needs to persist data changes
- **THEN** system acquires write lock on the core data file
- **THEN** system writes to temporary file
- **THEN** system atomically renames temp file to target file
- **THEN** system releases write lock

#### Scenario: Concurrent read during write
- **WHEN** read request arrives during write operation
- **THEN** read operation waits for write lock to release
- **THEN** read returns consistent data after write completes

---

### Requirement: Read configuration data from JSON
The system SHALL read configuration data (e.g., constraint presets) from a separate config JSON file (`tennis-config.json`). This file SHALL use an independent ReadWriteLock from the core data file.

#### Scenario: Successful config read
- **WHEN** system needs to load configuration data
- **THEN** system reads JSON file from the configured config path (`storage.config-file`)
- **THEN** system parses JSON and returns ConfigData object

#### Scenario: Config file does not exist
- **WHEN** `tennis-config.json` does not exist on first run
- **THEN** system returns empty ConfigData object
- **THEN** system creates the file with empty ConfigData on next write

---

### Requirement: Write configuration data to JSON
The system SHALL write configuration data to the config JSON file atomically, using an independent lock from the core data file.

#### Scenario: Config write does not block core data reads
- **WHEN** a config write is in progress
- **THEN** reads and writes to `tennis-data.json` SHALL proceed without waiting for the config write lock

#### Scenario: Atomic config write
- **WHEN** system writes configuration data
- **THEN** system uses temp file + atomic rename, same as core data write
- **THEN** original config file remains intact if write fails mid-way

---

### Requirement: Thread-safe file operations
The system SHALL ensure thread-safe concurrent access to each JSON file independently.

#### Scenario: Multiple readers on core data
- **WHEN** 5 users simultaneously read team data
- **THEN** all reads succeed without blocking
- **THEN** read lock allows concurrent access

#### Scenario: Multiple writers on core data
- **WHEN** 2 users simultaneously write team data
- **THEN** only one write proceeds at a time
- **THEN** second write waits for first to complete
- **THEN** data remains consistent

#### Scenario: Mixed read/write
- **WHEN** users both read and write simultaneously
- **THEN** writes have exclusive access
- **THEN** reads wait during writes
- **THEN** reads proceed concurrently when no writes active

#### Scenario: Core and config file locks are independent
- **WHEN** a write to `tennis-config.json` is in progress
- **THEN** concurrent reads/writes to `tennis-data.json` SHALL NOT be blocked

---

### Requirement: Data integrity
The system SHALL ensure data integrity during file operations for both files.

#### Scenario: Atomic write prevents corruption
- **WHEN** write operation fails mid-way
- **THEN** original data file remains intact
- **THEN** temporary file is cleaned up

#### Scenario: Power loss during write
- **WHEN** system loses power during rename operation
- **THEN** either old data or new data exists (never partial)
- **THEN** file is always in valid JSON format

---

### Requirement: Storage file paths configurable
The system SHALL read both file paths from `application.yml`. Both paths SHALL have sensible defaults.

#### Scenario: Default paths used when not configured
- **WHEN** `storage.config-file` is not set in `application.yml`
- **THEN** system SHALL use the default path `data/tennis-config.json`

#### Scenario: Custom paths override defaults
- **WHEN** `storage.data-file` and `storage.config-file` are both set
- **THEN** system SHALL use the configured paths for each respective file
