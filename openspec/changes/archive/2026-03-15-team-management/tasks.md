## 1. Backend Setup

- [x] 1.1 Create Spring Boot project structure
- [x] 1.2 Add Maven dependencies (Spring Boot Web, Jackson, Lombok)
- [x] 1.3 Configure JSON storage path in application.yml
- [x] 1.4 Configure UTF-8 encoding in application.yml (server.servlet.encoding.force: true)
- [x] 1.5 Start with JVM flag -Dfile.encoding=UTF-8

## 2. Data Model Implementation

- [x] 2.1 Create Player model class with all fields
- [x] 2.2 Create Team model class with players and lineups arrays
- [x] 2.3 Create TeamData wrapper class for JSON serialization
- [x] 2.4 Add Jackson annotations for JSON mapping
- [x] 2.5 Remove @JsonFormat on Team.createdAt (incompatible with Instant type)

## 3. JSON Repository Layer

- [x] 3.1 Create JsonRepository class with ReadWriteLock
- [x] 3.2 Implement readData() method with read lock
- [x] 3.3 Implement writeData() method with write lock
- [x] 3.4 Add atomic write (temp file + rename)
- [x] 3.5 Handle file not found exception (return empty data)
- [x] 3.6 Register JavaTimeModule in ObjectMapper (fix Instant serialization)

## 4. Team Service Layer

- [x] 4.1 Create TeamService with CRUD operations
- [x] 4.2 Implement createTeam() with duplicate name validation
- [x] 4.3 Implement getAllTeams() with date sorting
- [x] 4.4 Implement getTeamById() with 404 handling
- [x] 4.5 Implement updateTeamName() with validation
- [x] 4.6 Implement deleteTeam() with confirmation check

## 5. Player Service Layer

- [x] 5.1 Create PlayerService with player operations
- [x] 5.2 Implement addPlayer() with input validation
- [x] 5.3 Implement updatePlayer() with existence check
- [x] 5.4 Implement deletePlayer() with lineup reference check
- [x] 5.5 Implement getPlayersByTeamId()
- [x] 5.6 Add validation: gender enum, UTR range, name non-empty

## 6. Batch Import Service

- [x] 6.1 Create BatchImportService
- [x] 6.2 Implement parseCSV() with UTF-8 support
- [x] 6.3 Implement parseJSON() with error handling
- [x] 6.4 Implement validatePlayerData() utility
- [x] 6.5 Implement importPlayers() with error summary
- [x] 6.6 Handle partial import (skip invalid rows)

## 7. REST API Controllers

- [x] 7.1 Create TeamController with all team endpoints
- [x] 7.2 Add GET /api/teams endpoint
- [x] 7.3 Add GET /api/teams/{id} endpoint
- [x] 7.4 Add POST /api/teams endpoint with validation
- [x] 7.5 Add PUT /api/teams/{id} endpoint
- [x] 7.6 Add DELETE /api/teams/{id} endpoint
- [x] 7.7 Add POST /api/teams/{id}/players endpoint
- [x] 7.8 Add PUT /api/teams/{id}/players/{playerId} endpoint
- [x] 7.9 Add DELETE /api/teams/{id}/players/{playerId} endpoint
- [x] 7.10 Add POST /api/teams/import endpoint with file upload
- [x] 7.11 Add GET /api/teams/{id}/players endpoint (needed by usePlayers composable)

## 8. Exception Handling

- [x] 8.1 Create ErrorResponse model
- [x] 8.2 Create GlobalExceptionHandler
- [x] 8.3 Handle NOT_FOUND exception (404)
- [x] 8.4 Handle validation exceptions (400)
- [x] 8.5 Handle internal errors (500)

## 9. Frontend Project Setup

- [x] 9.1 Create Vue 3 + Vite project
- [x] 9.2 Install Tailwind CSS and configure
- [x] 9.3 Add Vue Router 4
- [x] 9.4 Configure build and dev scripts
- [x] 9.5 Configure Vite dev server proxy /api → localhost:8080

## 10. Frontend Layout Components

- [x] 10.1 Create MainLayout.vue（三栏容器：NavSidebar + 嵌套 router-view）
- [x] 10.2 Create NavSidebar.vue（功能导航，队伍管理为第一项，参考 finance 风格）
- [x] 10.3 Implement mobile responsive menu toggle
- [x] 10.4 Configure nested router（MainLayout → TeamManagerView → HomeView/TeamDetail）
- [x] 10.5 Create TeamManagerView.vue（两栏：TeamListPanel + 嵌套 router-view）
- [x] 10.6 Create TeamListPanel.vue（队伍列表 + 创建队伍 modal + 批量导入 modal）
- [x] 10.7 Create HomeView.vue（右栏空状态提示）

## 11. Frontend API Layer

- [x] 11.1 Create useApi composable for base API calls
- [x] 11.2 Implement request/response formatting
- [x] 11.3 Add error handling with user-friendly messages
- [ ] 11.4 Add TypeScript types for API responses

## 12. Frontend Composables

- [x] 12.1 Create useTeams composable
- [x] 12.2 Implement fetchTeams() function
- [x] 12.3 Implement createTeam() function
- [x] 12.4 Implement updateTeam() function
- [x] 12.5 Implement deleteTeam() with confirmation
- [x] 12.6 Create usePlayers composable
- [x] 12.7 Implement addPlayer() function
- [x] 12.8 Implement updatePlayer() function
- [x] 12.9 Implement deletePlayer() function
- [x] 12.10 Create useBatchImport composable
- [x] 12.11 Implement importFromCSV() function
- [x] 12.12 Implement importFromJSON() function

## 13. Frontend Pages

- [x] 13.1 Create TeamList.vue page（保留，已被 TeamListPanel 取代为左侧面板）
- [x] 13.2 Add "Create Team" button（集成在 TeamListPanel modal 中）
- [x] 13.3 Render team list with delete action
- [x] 13.4 Create TeamDetail.vue page
- [x] 13.5 Display player list in table
- [x] 13.6 Add "Add Player" modal/form
- [x] 13.7 Implement player edit functionality
- [x] 13.8 Implement delete player with confirmation

## 14. Frontend Components

- [x] 14.1 Create PlayerForm.vue component
- [x] 14.2 Add form fields: name, gender, UTR, verified
- [x] 14.3 Implement client-side validation
- [x] 14.4 Create BatchImport.vue component（独立组件，已集成为 TeamListPanel 内的 modal）
- [x] 14.5 Add file upload for CSV/JSON with drag-and-drop
- [x] 14.6 Display import summary (success/failure count)
- [x] 14.7 Show error messages for invalid rows

## 15. Backend Unit Tests

- [x] 15.1 Write JsonRepository unit tests
- [x] 15.2 Test read/write thread safety
- [x] 15.3 Write TeamService unit tests
- [x] 15.4 Write PlayerService unit tests
- [x] 15.5 Write BatchImportService unit tests
- [x] 15.6 Test validation rules (UTR, gender, name)
- [x] 15.7 Test CSV parsing with edge cases

## 16. Frontend Unit Tests

- [x] 16.1 Write useTeams composable tests
- [x] 16.2 Write usePlayers composable tests
- [x] 16.3 Write useApi composable tests
- [x] 16.4 Write TeamListPanel.vue component tests
- [x] 16.5 Write TeamDetail.vue component tests
- [x] 16.6 Write PlayerForm.vue component tests
- [x] 16.7 Write NavSidebar.vue component tests

## 17. Integration Testing

- [x] 17.1 Test TeamController endpoints
- [x] 17.2 Test GET /api/teams/{id}/players endpoint
- [x] 17.3 Test batch import endpoint
- [x] 17.4 Test error response format
- [x] 17.5 Test concurrent file operations

## 18. Documentation

- [x] 18.1 Update README with project description
- [x] 18.2 Document API endpoints
- [x] 18.3 Document data import format (CSV/JSON)
- [x] 18.4 Add development setup instructions
