package com.tennis.service;

import com.tennis.model.Player;
import com.tennis.model.TeamData;
import com.tennis.repository.JsonRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BatchImportService Test")
class BatchImportServiceTest {

    @Mock
    private JsonRepository jsonRepository;

    @Mock
    private PlayerService playerService;

    @InjectMocks
    private BatchImportService batchImportService;

    @Test
    @DisplayName("Should import players from CSV content")
    void shouldImportPlayersFromCSVContent() {
        // Arrange
        String csvContent = "name,gender,utr,verified\nJohn Doe,male,1.5,true\nJane Smith,female,2.0,false";

        TeamData teamData = new TeamData();
        when(jsonRepository.readData()).thenReturn(teamData);
        when(playerService.addPlayer(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(new Player());

        // Act
        BatchImportService.ImportResult result = batchImportService.importFromCSV("test-team", csvContent);

        // Assert
        assertEquals(2, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertTrue(result.getErrors().isEmpty());
        verify(playerService, times(2)).addPlayer(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should skip header row when importing from CSV")
    void shouldSkipHeaderRowWhenImportingFromCSV() {
        // Arrange
        String csvContent = "name,gender,utr,verified\nJohn Doe,male,1.5,true";

        TeamData teamData = new TeamData();
        when(jsonRepository.readData()).thenReturn(teamData);
        when(playerService.addPlayer(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(new Player());

        // Act
        BatchImportService.ImportResult result = batchImportService.importFromCSV("test-team", csvContent);

        // Assert
        assertEquals(1, result.getSuccessCount());
        verify(playerService, times(1)).addPlayer(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should handle empty CSV content")
    void shouldHandleEmptyCSVContent() {
        // Arrange
        String csvContent = "name,gender,utr,verified";

        TeamData teamData = new TeamData();
        when(jsonRepository.readData()).thenReturn(teamData);

        // Act
        BatchImportService.ImportResult result = batchImportService.importFromCSV("test-team", csvContent);

        // Assert
        assertEquals(0, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    @DisplayName("Should handle CSV with missing fields")
    void shouldHandleCSVWithMissingFields() {
        // Arrange
        String csvContent = "name,gender\nJohn Doe,male"; // Missing UTR

        TeamData teamData = new TeamData();
        when(jsonRepository.readData()).thenReturn(teamData);

        // Act
        BatchImportService.ImportResult result = batchImportService.importFromCSV("test-team", csvContent);

        // Assert
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("字段数量不足"));
    }

    @Test
    @DisplayName("Should handle invalid UTR in CSV")
    void shouldHandleInvalidUTRInCSV() {
        // Arrange
        String csvContent = "name,gender,utr,verified\nJohn Doe,male,invalid,true";

        TeamData teamData = new TeamData();
        when(jsonRepository.readData()).thenReturn(teamData);

        // Act
        BatchImportService.ImportResult result = batchImportService.importFromCSV("test-team", csvContent);

        // Assert
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("UTR必须是有效的数字"));
    }

    @Test
    @DisplayName("Should handle UTR out of range in CSV")
    void shouldHandleUTROfRangeInCSV() {
        // Arrange
        String csvContent = "name,gender,utr,verified\nJohn Doe,male,20.0,true";

        TeamData teamData = new TeamData();
        when(jsonRepository.readData()).thenReturn(teamData);

        // Act
        BatchImportService.ImportResult result = batchImportService.importFromCSV("test-team", csvContent);

        // Assert
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("UTR必须在0.0到16.0之间"));
    }

    @Test
    @DisplayName("Should handle invalid gender in CSV")
    void shouldHandleInvalidGenderInCSV() {
        // Arrange
        String csvContent = "name,gender,utr,verified\nJohn Doe,unknown,1.5,true";

        TeamData teamData = new TeamData();
        when(jsonRepository.readData()).thenReturn(teamData);

        // Act
        BatchImportService.ImportResult result = batchImportService.importFromCSV("test-team", csvContent);

        // Assert
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("性别必须是male或female"));
    }

    @Test
    @DisplayName("Should handle empty name in CSV")
    void shouldHandleEmptyNameInCSV() {
        // Arrange
        String csvContent = "name,gender,utr,verified\n, male,1.5,true";

        TeamData teamData = new TeamData();
        when(jsonRepository.readData()).thenReturn(teamData);

        // Act
        BatchImportService.ImportResult result = batchImportService.importFromCSV("test-team", csvContent);

        // Assert
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("姓名不能为空"));
    }

    @Test
    @DisplayName("Should import players from JSON content")
    void shouldImportPlayersFromJSONContent() {
        // Arrange
        String jsonContent = "[{\"name\":\"John Doe\",\"gender\":\"male\",\"utr\":1.5,\"verified\":true},{\"name\":\"Jane Smith\",\"gender\":\"female\",\"utr\":2.0,\"verified\":false}]";

        TeamData teamData = new TeamData();
        when(jsonRepository.readData()).thenReturn(teamData);
        when(playerService.addPlayer(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(new Player());

        // Act
        BatchImportService.ImportResult result = batchImportService.importFromJSON("test-team", jsonContent);

        // Assert
        assertEquals(2, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertTrue(result.getErrors().isEmpty());
        verify(playerService, times(2)).addPlayer(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should import from JSON without brackets")
    void shouldImportFromJSONWithoutBrackets() {
        // Arrange
        String jsonContent = "{\"name\":\"John Doe\",\"gender\":\"male\",\"utr\":1.5,\"verified\":true}";

        TeamData teamData = new TeamData();
        when(jsonRepository.readData()).thenReturn(teamData);
        when(playerService.addPlayer(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(new Player());

        // Act
        BatchImportService.ImportResult result = batchImportService.importFromJSON("test-team", jsonContent);

        // Assert
        assertEquals(1, result.getSuccessCount());
        verify(playerService, times(1)).addPlayer(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should handle empty JSON content")
    void shouldHandleEmptyJSONContent() {
        // Arrange
        String jsonContent = "[]";

        TeamData teamData = new TeamData();
        when(jsonRepository.readData()).thenReturn(teamData);

        // Act
        BatchImportService.ImportResult result = batchImportService.importFromJSON("test-team", jsonContent);

        // Assert
        assertEquals(0, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    @DisplayName("Should handle JSON parsing errors")
    void shouldHandleJSONParsingErrors() {
        // Arrange
        String jsonContent = "invalid json";

        // Act (no stubs needed - parseJSONContent throws before calling readData)
        BatchImportService.ImportResult result = batchImportService.importFromJSON("test-team", jsonContent);

        // Assert
        assertEquals(0, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("JSON解析错误"));
    }

    @Test
    @DisplayName("Should handle partial import with some valid and some invalid rows")
    void shouldHandlePartialImportWithSomeValidAndSomeInvalidRows() {
        // Arrange
        String csvContent = "name,gender,utr,verified\n" +
                           "John Doe,male,1.5,true\n" +
                           "Invalid Name,male,20.0,true\n" +
                           "Jane Smith,female,2.0,false\n" +
                           ",female,2.0,false";

        TeamData teamData = new TeamData();
        when(jsonRepository.readData()).thenReturn(teamData);
        when(playerService.addPlayer(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(new Player());

        // Act
        BatchImportService.ImportResult result = batchImportService.importFromCSV("test-team", csvContent);

        // Assert
        assertEquals(2, result.getSuccessCount());
        assertEquals(2, result.getFailureCount());
        assertEquals(2, result.getErrors().size());
        verify(playerService, times(2)).addPlayer(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should continue import after player service throws exception")
    void shouldContinueImportAfterPlayerServiceThrowsException() {
        // Arrange
        String csvContent = "name,gender,utr,verified\n" +
                           "John Doe,male,1.5,true\n" +
                           "Jane Smith,female,2.0,false";

        TeamData teamData = new TeamData();
        when(jsonRepository.readData()).thenReturn(teamData);
        when(playerService.addPlayer("team-1", "John Doe", "male", 1.5, null, true, null, null)).thenReturn(new Player());
        doThrow(new RuntimeException("Team not found"))
            .when(playerService).addPlayer("team-1", "Jane Smith", "female", 2.0, null, false, null, null);

        // Act
        BatchImportService.ImportResult result = batchImportService.importFromCSV("test-team", csvContent);

        // Assert
        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("导入失败"));
    }

    @Test
    @DisplayName("Should handle malformed JSON with multiple players")
    void shouldHandleMalformedJSONWithMultiplePlayers() {
        // Arrange
        String jsonContent = "[{\"name\":\"John\",\"gender\":\"male\",\"utr\":1.5}," +
                           "{\"gender\":\"female\",\"utr\":2.0,\"name\":\"Jane\"}]";

        TeamData teamData = new TeamData();
        when(jsonRepository.readData()).thenReturn(teamData);
        when(playerService.addPlayer(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(new Player());

        // Act
        BatchImportService.ImportResult result = batchImportService.importFromJSON("test-team", jsonContent);

        // Assert
        // Should handle gracefully
        assertNotNull(result);
    }
}
