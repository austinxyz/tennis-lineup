package com.tennis.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tennis.model.Team;
import com.tennis.service.BatchImportService;
import com.tennis.service.TeamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import com.tennis.exception.NotFoundException;
import com.tennis.model.Player;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(TeamController.class)
@DisplayName("TeamController Test")
class TeamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TeamService teamService;

    @MockBean
    private BatchImportService batchImportService;

    @Autowired
    private ObjectMapper objectMapper;

    private Team testTeam;
    private Player testPlayer;

    @BeforeEach
    void setUp() {
        testTeam = new Team();
        testTeam.setId("team-1");
        testTeam.setName("Test Team");
        testTeam.setCreatedAt(Instant.now());
        testTeam.setLineups(List.of());

        testPlayer = new Player();
        testPlayer.setId("player-1");
        testPlayer.setName("John Doe");
        testPlayer.setGender("male");
        testPlayer.setUtr(1.5);
        testPlayer.setVerified(true);

        testTeam.setPlayers(List.of(testPlayer));
    }

    @Test
    @DisplayName("Should get all teams")
    void shouldGetAllTeams() throws Exception {
        // Arrange
        when(teamService.getAllTeams()).thenReturn(List.of(testTeam));

        // Act & Assert
        mockMvc.perform(get("/api/teams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("team-1"))
                .andExpect(jsonPath("$[0].name").value("Test Team"));
    }

    @Test
    @DisplayName("Should get team by ID")
    void shouldGetTeamById() throws Exception {
        // Arrange
        when(teamService.getTeamById("team-1")).thenReturn(testTeam);

        // Act & Assert
        mockMvc.perform(get("/api/teams/team-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("team-1"))
                .andExpect(jsonPath("$.name").value("Test Team"));
    }

    @Test
    @DisplayName("Should get players by team ID")
    void shouldGetPlayersByTeamId() throws Exception {
        // Arrange
        when(teamService.getTeamById("team-1")).thenReturn(testTeam);

        // Act & Assert
        mockMvc.perform(get("/api/teams/team-1/players"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("player-1"))
                .andExpect(jsonPath("$[0].name").value("John Doe"))
                .andExpect(jsonPath("$[0].gender").value("male"))
                .andExpect(jsonPath("$[0].utr").value(1.5));
    }

    @Test
    @DisplayName("Should return 404 when getting non-existent team")
    void shouldReturn404WhenGettingNonExistentTeam() throws Exception {
        // Arrange
        when(teamService.getTeamById("non-existent"))
                .thenThrow(new NotFoundException("队伍不存在"));

        // Act & Assert
        mockMvc.perform(get("/api/teams/non-existent"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should create new team")
    void shouldCreateNewTeam() throws Exception {
        // Arrange
        TeamRequest request = new TeamRequest();
        request.setName("New Team");

        when(teamService.createTeam("New Team")).thenReturn(testTeam);

        // Act & Assert
        mockMvc.perform(post("/api/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("team-1"))
                .andExpect(jsonPath("$.name").value("Test Team"));
    }

    @Test
    @DisplayName("Should return 400 when creating team with empty name")
    void shouldReturn400WhenCreatingTeamWithEmptyName() throws Exception {
        // Arrange
        TeamRequest request = new TeamRequest();
        request.setName("");

        when(teamService.createTeam(""))
                .thenThrow(new IllegalArgumentException("队名不能为空"));

        // Act & Assert
        mockMvc.perform(post("/api/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should update team name")
    void shouldUpdateTeamName() throws Exception {
        // Arrange
        TeamRequest request = new TeamRequest();
        request.setName("Updated Team");

        when(teamService.updateTeamName("team-1", "Updated Team")).thenReturn(testTeam);

        // Act & Assert
        mockMvc.perform(put("/api/teams/team-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("team-1"))
                .andExpect(jsonPath("$.name").value("Test Team"));
    }

    @Test
    @DisplayName("Should delete team")
    void shouldDeleteTeam() throws Exception {
        // Arrange
        doNothing().when(teamService).deleteTeam("team-1");

        // Act & Assert
        mockMvc.perform(delete("/api/teams/team-1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should add player to team")
    void shouldAddPlayerToTeam() throws Exception {
        // Arrange
        PlayerRequest playerRequest = new PlayerRequest();
        playerRequest.setName("John Doe");
        playerRequest.setGender("male");
        playerRequest.setUtr(1.5);
        playerRequest.setVerified(true);

        when(teamService.addPlayer(eq("team-1"), eq("John Doe"), eq("male"), eq(1.5), eq(null), eq(true), eq(null), eq(null)))
                .thenReturn(testPlayer);

        // Act & Assert
        mockMvc.perform(post("/api/teams/team-1/players")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(playerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("player-1"))
                .andExpect(jsonPath("$.name").value("John Doe"));
    }

    @Test
    @DisplayName("Should update player")
    void shouldUpdatePlayer() throws Exception {
        // Arrange
        PlayerRequest playerRequest = new PlayerRequest();
        playerRequest.setName("Jane Smith");
        playerRequest.setGender("female");
        playerRequest.setUtr(2.0);
        playerRequest.setVerified(false);

        when(teamService.updatePlayer(eq("team-1"), eq("player-1"), eq("Jane Smith"), eq("female"), eq(2.0), eq(null), eq(false), eq(null), eq(null)))
                .thenReturn(testPlayer);

        // Act & Assert
        mockMvc.perform(put("/api/teams/team-1/players/player-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(playerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("player-1"));
    }

    @Test
    @DisplayName("Should delete player from team")
    void shouldDeletePlayerFromTeam() throws Exception {
        // Arrange
        doNothing().when(teamService).deletePlayer("team-1", "player-1");

        // Act & Assert
        mockMvc.perform(delete("/api/teams/team-1/players/player-1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should import players from CSV file")
    void shouldImportPlayersFromCSVFile() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "players.csv",
                "text/csv",
                "name,gender,utr,verified\nJohn Doe,male,1.5,true\nJane Smith,female,2.0,false"
                        .getBytes());

        BatchImportService.ImportResult result = new BatchImportService.ImportResult(2, 0, List.of());
        when(batchImportService.importFromCSV(any(String.class))).thenReturn(result);

        // Act & Assert
        mockMvc.perform(multipart("/api/teams/import")
                .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successCount").value(2))
                .andExpect(jsonPath("$.failureCount").value(0));
    }

    @Test
    @DisplayName("Should return 400 when importing empty file")
    void shouldReturn400WhenImportingEmptyFile() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.csv",
                "text/csv",
                "".getBytes());

        when(batchImportService.importFromCSV(any(String.class)))
                .thenThrow(new RuntimeException("请选择要导入的文件"));

        // Act & Assert
        mockMvc.perform(multipart("/api/teams/import")
                .file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when importing unsupported file type")
    void shouldReturn400WhenImportingUnsupportedFileType() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "players.txt",
                "text/plain",
                "name,gender,utr,verified\nJohn Doe,male,1.5,true"
                        .getBytes());

        when(batchImportService.importFromCSV(any(String.class)))
                .thenThrow(new RuntimeException("不支持的文件格式，请上传 CSV 或 JSON 文件"));

        // Act & Assert
        mockMvc.perform(multipart("/api/teams/import")
                .file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should add player with profileUrl")
    void shouldAddPlayerWithProfileUrl() throws Exception {
        // Arrange
        String url = "https://app.utrsports.net/profiles/12345";
        Player playerWithUrl = new Player();
        playerWithUrl.setId("player-1");
        playerWithUrl.setName("John Doe");
        playerWithUrl.setGender("male");
        playerWithUrl.setUtr(1.5);
        playerWithUrl.setVerified(true);
        playerWithUrl.setProfileUrl(url);

        PlayerRequest playerRequest = new PlayerRequest();
        playerRequest.setName("John Doe");
        playerRequest.setGender("male");
        playerRequest.setUtr(1.5);
        playerRequest.setVerified(true);
        playerRequest.setProfileUrl(url);

        when(teamService.addPlayer(eq("team-1"), eq("John Doe"), eq("male"), eq(1.5), eq(null), eq(true), eq(url), eq(null)))
                .thenReturn(playerWithUrl);

        // Act & Assert
        mockMvc.perform(post("/api/teams/team-1/players")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(playerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileUrl").value(url));
    }

    @Test
    @DisplayName("Should include profileUrl in get players response")
    void shouldIncludeProfileUrlInGetPlayersResponse() throws Exception {
        // Arrange
        String url = "https://app.utrsports.net/profiles/99999";
        testPlayer.setProfileUrl(url);
        when(teamService.getTeamById("team-1")).thenReturn(testTeam);

        // Act & Assert
        mockMvc.perform(get("/api/teams/team-1/players"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].profileUrl").value(url));
    }

    @Test
    @DisplayName("Should handle import errors gracefully")
    void shouldHandleImportErrorsGracefully() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "players.csv",
                "text/csv",
                "name,gender,utr,invalid\nJohn Doe,male,1.5,true"
                        .getBytes());

        BatchImportService.ImportResult result = new BatchImportService.ImportResult(
                0, 1, List.of("导入失败: 无效数据"));

        when(batchImportService.importFromCSV(any(String.class))).thenReturn(result);

        // Act & Assert
        mockMvc.perform(multipart("/api/teams/import")
                .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successCount").value(0))
                .andExpect(jsonPath("$.failureCount").value(1))
                .andExpect(jsonPath("$.errors[0]").value("导入失败: 无效数据"));
    }
}