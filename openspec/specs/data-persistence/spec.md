## ADDED Requirements

### Requirement: Read team data from JSON
The system SHALL read team data from JSON file.

#### Scenario: Successful data read
- **WHEN** system starts or needs to load data
- **THEN** system reads JSON file from configured path
- **THEN** system parses JSON and returns TeamData object
- **THEN** system handles file lock for thread safety

#### Scenario: File does not exist
- **WHEN** JSON file does not exist on first run
- **THEN** system returns empty TeamData object
- **THEN** system creates file on next write

---

### Requirement: Write team data to JSON
The system SHALL write team data to JSON file atomically.

#### Scenario: Successful data write
- **WHEN** system needs to persist data changes
- **THEN** system acquires write lock
- **THEN** system writes to temporary file
- **THEN** system atomically renames temp file to target file
- **THEN** system releases write lock

#### Scenario: Concurrent read during write
- **WHEN** read request arrives during write operation
- **THEN** read operation waits for write lock to release
- **THEN** read returns consistent data after write completes

---

### Requirement: Thread-safe file operations
The system SHALL ensure thread-safe concurrent access to JSON file.

#### Scenario: Multiple readers
- **WHEN** 5 users simultaneously read team data
- **THEN** all reads succeed without blocking
- **THEN** read lock allows concurrent access

#### Scenario: Multiple writers
- **WHEN** 2 users simultaneously write team data
- **THEN** only one write proceeds at a time
- **THEN** second write waits for first to complete
- **THEN** data remains consistent

#### Scenario: Mixed read/write
- **WHEN** users both read and write simultaneously
- **THEN** writes have exclusive access
- **THEN** reads wait during writes
- **THEN** reads proceed concurrently when no writes active

---

### Requirement: Data integrity
The system SHALL ensure data integrity during file operations.

#### Scenario: Atomic write prevents corruption
- **WHEN** write operation fails mid-way
- **THEN** original data file remains intact
- **THEN** temporary file is cleaned up

#### Scenario: Power loss during write
- **WHEN** system loses power during rename operation
- **THEN** either old data or new data exists (never partial)
- **THEN** file is always in valid JSON format
