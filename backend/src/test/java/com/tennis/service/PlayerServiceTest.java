package com.tennis.service;

import com.tennis.model.Player;
import com.tennis.model.Team;
import com.tennis.model.TeamData;
import com.tennis.repository.JsonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerService Test")
class PlayerServiceTest {

    @Mock
    private JsonRepository jsonRepository;

    @InjectMocks
    private PlayerService playerService;

    private TeamData mockTeamData;
    private Team mockTeam;

    @BeforeEach
    void setUp() {
        mockTeamData = new TeamData();
        mockTeam = new Team();
        mockTeam.setId("team-1");
        mockTeam.setName("Test Team");
        mockTeam.setCreatedAt(Instant.now());
        mockTeamData.getTeams().add(mockTeam);
    }

    @Test
    @DisplayName("Should add player to team with valid data")
    void shouldAddPlayerToTeamWithValidData() {
        // Arrange
        String playerName = "John Doe";
        String gender = "male";
        double utr = 1.5;
        boolean verified = true;

        when(jsonRepository.readData()).thenReturn(mockTeamData);

        // Act
        Player result = playerService.addPlayer("team-1", playerName, gender, utr, null, verified, null, null, null);

        // Assert
        assertNotNull(result);
        assertEquals(playerName, result.getName());
        assertEquals(gender, result.getGender());
        assertEquals(utr, result.getUtr());
        assertEquals(verified, result.getVerified());
        assertEquals(1, mockTeam.getPlayers().size());
        assertEquals(result, mockTeam.getPlayers().get(0));
    }

    @Test
    @DisplayName("Should throw exception when adding player with empty name")
    void shouldThrowExceptionWhenAddingPlayerWithEmptyName() {
        // Arrange
        when(jsonRepository.readData()).thenReturn(mockTeamData);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            playerService.addPlayer("team-1", "", "male", 1.5, null, true, null, null, null);
        });
    }

    @Test
    @DisplayName("Should throw exception when adding player with null name")
    void shouldThrowExceptionWhenAddingPlayerWithNullName() {
        // Arrange
        when(jsonRepository.readData()).thenReturn(mockTeamData);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            playerService.addPlayer("team-1", null, "male", 1.5, null, true, null, null, null);
        });
    }

    @Test
    @DisplayName("Should throw exception when adding player with invalid gender")
    void shouldThrowExceptionWhenAddingPlayerWithInvalidGender() {
        // Arrange
        when(jsonRepository.readData()).thenReturn(mockTeamData);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            playerService.addPlayer("team-1", "John Doe", "unknown", 1.5, null, true, null, null, null);
        });
    }

    @Test
    @DisplayName("Should throw exception when adding player with invalid UTR")
    void shouldThrowExceptionWhenAddingPlayerWithInvalidUTR() {
        // Arrange
        when(jsonRepository.readData()).thenReturn(mockTeamData);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            playerService.addPlayer("team-1", "John Doe", "male", -1.0, null, true, null, null, null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            playerService.addPlayer("team-1", "John Doe", "male", 16.1, null, true, null, null, null);
        });
    }

    @Test
    @DisplayName("Should throw exception when adding player to non-existent team")
    void shouldThrowExceptionWhenAddingPlayerToNonExistentTeam() {
        // Arrange
        when(jsonRepository.readData()).thenReturn(mockTeamData);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            playerService.addPlayer("non-existent-team", "John Doe", "male", 1.5, null, true, null, null, null);
        });
    }

    @Test
    @DisplayName("Should update player with valid data")
    void shouldUpdatePlayerWithValidData() {
        // Arrange
        Player originalPlayer = new Player();
        originalPlayer.setId("player-1");
        originalPlayer.setName("Original Name");
        originalPlayer.setGender("male");
        originalPlayer.setUtr(1.0);
        originalPlayer.setVerified(false);
        mockTeam.getPlayers().add(originalPlayer);

        when(jsonRepository.readData()).thenReturn(mockTeamData);

        // Act
        Player result = playerService.updatePlayer("team-1", "player-1",
            "Updated Name", "female", 2.0, null, true, null, null, null);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Name", result.getName());
        assertEquals("female", result.getGender());
        assertEquals(2.0, result.getUtr());
        assertTrue(result.getVerified());
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent player")
    void shouldThrowExceptionWhenUpdatingNonExistentPlayer() {
        // Arrange
        when(jsonRepository.readData()).thenReturn(mockTeamData);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            playerService.updatePlayer("team-1", "non-existent-player",
                "Name", "male", 1.5, null, true, null, null, null);
        });
    }

    @Test
    @DisplayName("Should delete player from team")
    void shouldDeletePlayerFromTeam() {
        // Arrange
        Player player = new Player();
        player.setId("player-1");
        player.setName("John Doe");
        player.setGender("male");
        player.setUtr(1.5);
        player.setVerified(true);
        mockTeam.getPlayers().add(player);

        when(jsonRepository.readData()).thenReturn(mockTeamData);

        // Act
        playerService.deletePlayer("team-1", "player-1");

        // Assert
        assertEquals(0, mockTeam.getPlayers().size());
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent player")
    void shouldThrowExceptionWhenDeletingNonExistentPlayer() {
        // Arrange
        when(jsonRepository.readData()).thenReturn(mockTeamData);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            playerService.deletePlayer("team-1", "non-existent-player");
        });
    }

    @Test
    @DisplayName("Should get players by team ID")
    void shouldGetPlayersByTeamId() {
        // Arrange
        Player player1 = new Player();
        player1.setId("player-1");
        player1.setName("Player 1");
        player1.setGender("male");
        player1.setUtr(1.0);

        Player player2 = new Player();
        player2.setId("player-2");
        player2.setName("Player 2");
        player2.setGender("female");
        player2.setUtr(2.0);

        mockTeam.getPlayers().add(player1);
        mockTeam.getPlayers().add(player2);

        when(jsonRepository.readData()).thenReturn(mockTeamData);

        // Act
        List<Player> result = playerService.getPlayersByTeamId("team-1");

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.contains(player1));
        assertTrue(result.contains(player2));
    }

    @Test
    @DisplayName("Should return empty list when getting players from team with no players")
    void shouldReturnEmptyListWhenGettingPlayersFromTeamWithNoPlayers() {
        // Arrange
        when(jsonRepository.readData()).thenReturn(mockTeamData);

        // Act
        List<Player> result = playerService.getPlayersByTeamId("team-1");

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should throw exception when getting players from non-existent team")
    void shouldThrowExceptionWhenGettingPlayersFromNonExistentTeam() {
        // Arrange
        when(jsonRepository.readData()).thenReturn(mockTeamData);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            playerService.getPlayersByTeamId("non-existent-team");
        });
    }

    @Test
    @DisplayName("Should convert gender to lowercase")
    void shouldConvertGenderToLowercase() {
        // Arrange
        when(jsonRepository.readData()).thenReturn(mockTeamData);

        // Act
        Player result = playerService.addPlayer("team-1", "John Doe", "MALE", 1.5, null, true, null, null, null);

        // Assert
        assertEquals("male", result.getGender());
    }

    @Test
    @DisplayName("Should trim player name")
    void shouldTrimPlayerName() {
        // Arrange
        when(jsonRepository.readData()).thenReturn(mockTeamData);

        // Act
        Player result = playerService.addPlayer("team-1", "  John Doe  ", "male", 1.5, null, true, null, null, null);

        // Assert
        assertEquals("John Doe", result.getName());
    }

    @Test
    @DisplayName("Should store profileUrl when provided")
    void shouldStoreProfileUrlWhenProvided() {
        // Arrange
        when(jsonRepository.readData()).thenReturn(mockTeamData);
        String url = "https://app.utrsports.net/profiles/12345";

        // Act
        Player result = playerService.addPlayer("team-1", "John Doe", "male", 1.5, null, true, url, null, null);

        // Assert
        assertEquals(url, result.getProfileUrl());
    }

    @Test
    @DisplayName("Should store null when profileUrl is blank")
    void shouldStoreNullWhenProfileUrlIsBlank() {
        // Arrange
        when(jsonRepository.readData()).thenReturn(mockTeamData);

        // Act
        Player result = playerService.addPlayer("team-1", "John Doe", "male", 1.5, null, true, "   ", null, null);

        // Assert
        assertNull(result.getProfileUrl());
    }

    @Test
    @DisplayName("Should store null when profileUrl is null")
    void shouldStoreNullWhenProfileUrlIsNull() {
        // Arrange
        when(jsonRepository.readData()).thenReturn(mockTeamData);

        // Act
        Player result = playerService.addPlayer("team-1", "John Doe", "male", 1.5, null, true, null, null, null);

        // Assert
        assertNull(result.getProfileUrl());
    }

    @Test
    @DisplayName("Should update profileUrl when updating player")
    void shouldUpdateProfileUrlWhenUpdatingPlayer() {
        // Arrange
        Player originalPlayer = new Player();
        originalPlayer.setId("player-1");
        originalPlayer.setName("Original Name");
        originalPlayer.setGender("male");
        originalPlayer.setUtr(1.0);
        originalPlayer.setVerified(false);
        mockTeam.getPlayers().add(originalPlayer);

        when(jsonRepository.readData()).thenReturn(mockTeamData);
        String url = "https://app.utrsports.net/profiles/99999";

        // Act
        Player result = playerService.updatePlayer("team-1", "player-1",
            "Original Name", "male", 1.0, null, false, url, null, null);

        // Assert
        assertEquals(url, result.getProfileUrl());
    }

    @Test
    @DisplayName("Should generate unique player IDs")
    void shouldGenerateUniquePlayerIds() {
        // Arrange
        when(jsonRepository.readData()).thenReturn(mockTeamData);

        // Act
        Player player1 = playerService.addPlayer("team-1", "Player 1", "male", 1.0, null, true, null, null, null);
        Player player2 = playerService.addPlayer("team-1", "Player 2", "female", 2.0, null, false, null, null, null);

        // Assert
        assertNotEquals(player1.getId(), player2.getId());
        assertTrue(player1.getId().startsWith("player-"));
        assertTrue(player2.getId().startsWith("player-"));
    }

    @Test
    @DisplayName("Should save notes when adding player")
    void shouldSaveNotesWhenAddingPlayer() {
        when(jsonRepository.readData()).thenReturn(mockTeamData);

        Player result = playerService.addPlayer("team-1", "John Doe", "male", 1.5, null, true, null, "正手强，发球好", null);

        assertEquals("正手强，发球好", result.getNotes());
    }

    @Test
    @DisplayName("Should save null notes when notes is blank")
    void shouldSaveNullWhenNotesIsBlank() {
        when(jsonRepository.readData()).thenReturn(mockTeamData);

        Player result = playerService.addPlayer("team-1", "John Doe", "male", 1.5, null, true, null, "   ", null);

        assertNull(result.getNotes());
    }

    @Test
    @DisplayName("Should update notes when updating player")
    void shouldUpdateNotesWhenUpdatingPlayer() {
        Player original = new Player();
        original.setId("player-1");
        original.setName("John Doe");
        original.setGender("male");
        original.setUtr(1.5);
        original.setVerified(false);
        mockTeam.getPlayers().add(original);

        when(jsonRepository.readData()).thenReturn(mockTeamData);

        Player result = playerService.updatePlayer("team-1", "player-1",
            "John Doe", "male", 1.5, null, false, null, "反手相对弱", null);

        assertEquals("反手相对弱", result.getNotes());
    }

    // ---- Task 4.1 — actualUtr tests ----

    @Test
    @DisplayName("addPlayer with actualUtr stores the value")
    void addPlayer_withActualUtr_storesValue() {
        when(jsonRepository.readData()).thenReturn(mockTeamData);

        Player result = playerService.addPlayer("team-1", "John Doe", "male", 5.0, null, true, null, null, 7.5);

        assertNotNull(result);
        assertEquals(7.5, result.getActualUtr());
    }

    @Test
    @DisplayName("addPlayer with null actualUtr stores null")
    void addPlayer_withNullActualUtr_storesNull() {
        when(jsonRepository.readData()).thenReturn(mockTeamData);

        Player result = playerService.addPlayer("team-1", "John Doe", "male", 5.0, null, true, null, null, null);

        assertNotNull(result);
        assertNull(result.getActualUtr());
    }

    @Test
    @DisplayName("addPlayer with actualUtr below 0.0 throws IllegalArgumentException")
    void addPlayer_withActualUtrBelowZero_throwsException() {
        when(jsonRepository.readData()).thenReturn(mockTeamData);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            playerService.addPlayer("team-1", "John Doe", "male", 5.0, null, true, null, null, -0.1)
        );
        assertEquals("实际UTR必须在0.0到16.0之间", ex.getMessage());
    }

    @Test
    @DisplayName("addPlayer with actualUtr above 16.0 throws IllegalArgumentException")
    void addPlayer_withActualUtrAbove16_throwsException() {
        when(jsonRepository.readData()).thenReturn(mockTeamData);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            playerService.addPlayer("team-1", "John Doe", "male", 5.0, null, true, null, null, 16.1)
        );
        assertEquals("实际UTR必须在0.0到16.0之间", ex.getMessage());
    }

    @Test
    @DisplayName("addPlayer with actualUtr at boundary values 0.0 and 16.0 succeeds")
    void addPlayer_withActualUtrAtBoundary_succeeds() {
        when(jsonRepository.readData()).thenReturn(mockTeamData);

        Player resultLow = playerService.addPlayer("team-1", "Player Low", "male", 5.0, null, true, null, null, 0.0);
        assertEquals(0.0, resultLow.getActualUtr());

        Player resultHigh = playerService.addPlayer("team-1", "Player High", "male", 5.0, null, true, null, null, 16.0);
        assertEquals(16.0, resultHigh.getActualUtr());
    }

    @Test
    @DisplayName("updatePlayer with actualUtr updates the value")
    void updatePlayer_withActualUtr_updatesValue() {
        Player original = new Player();
        original.setId("player-1");
        original.setName("John Doe");
        original.setGender("male");
        original.setUtr(5.0);
        original.setVerified(false);
        mockTeam.getPlayers().add(original);

        when(jsonRepository.readData()).thenReturn(mockTeamData);

        Player result = playerService.updatePlayer("team-1", "player-1",
            "John Doe", "male", 5.0, null, false, null, null, 8.0);

        assertNotNull(result);
        assertEquals(8.0, result.getActualUtr());
    }

    // ---- Task 4.3 — getEffectiveActualUtr unit tests ----

    @Test
    @DisplayName("getEffectiveActualUtr returns utr when actualUtr is null")
    void getEffectiveActualUtr_whenActualUtrNull_returnsUtr() {
        Player player = new Player();
        player.setUtr(5.0);
        player.setActualUtr(null);

        assertEquals(5.0, player.getEffectiveActualUtr());
    }

    @Test
    @DisplayName("getEffectiveActualUtr returns actualUtr when set")
    void getEffectiveActualUtr_whenActualUtrSet_returnsActualUtr() {
        Player player = new Player();
        player.setUtr(5.0);
        player.setActualUtr(7.0);

        assertEquals(7.0, player.getEffectiveActualUtr());
    }
}