package com.tennis.service;

import com.tennis.exception.TeamNotEmptyException;
import com.tennis.model.Lineup;
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
@DisplayName("TeamService Test")
class TeamServiceTest {

    @Mock
    private JsonRepository jsonRepository;

    @InjectMocks
    private TeamService teamService;

    private TeamData mockTeamData;

    @BeforeEach
    void setUp() {
        mockTeamData = new TeamData();
    }

    @Test
    @DisplayName("Should create new team with valid name")
    void shouldCreateTeamWithValidName() {
        // Arrange
        String teamName = "Test Team";
        when(jsonRepository.readData()).thenReturn(mockTeamData);

        // Act
        Team result = teamService.createTeam(teamName);

        // Assert
        assertNotNull(result);
        assertEquals(teamName, result.getName());
        assertNotNull(result.getId());
        assertNotNull(result.getCreatedAt());
        assertTrue(mockTeamData.getTeams().contains(result));
    }

    @Test
    @DisplayName("Should throw exception when creating team with empty name")
    void shouldThrowExceptionWhenCreatingTeamWithEmptyName() {
        // Arrange & Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            teamService.createTeam("");
        });
    }

    @Test
    @DisplayName("Should throw exception when creating team with null name")
    void shouldThrowExceptionWhenCreatingTeamWithNullName() {
        // Arrange & Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            teamService.createTeam(null);
        });
    }

    @Test
    @DisplayName("Should throw exception when creating team with duplicate name")
    void shouldThrowExceptionWhenCreatingTeamWithDuplicateName() {
        // Arrange
        String teamName = "Existing Team";
        Team existingTeam = new Team();
        existingTeam.setId("team-1");
        existingTeam.setName(teamName);
        existingTeam.setCreatedAt(Instant.now());
        mockTeamData.getTeams().add(existingTeam);

        when(jsonRepository.readData()).thenReturn(mockTeamData);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            teamService.createTeam(teamName);
        });
    }

    @Test
    @DisplayName("Should throw exception when creating team with name exceeding 50 characters")
    void shouldThrowExceptionWhenCreatingTeamWithLongName() {
        // Arrange
        String longName = "a".repeat(51);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            teamService.createTeam(longName);
        });
    }

    @Test
    @DisplayName("Should get all teams sorted by creation date (newest first)")
    void shouldGetAllTeamsSortedByCreationDate() {
        // Arrange
        Team team1 = new Team();
        team1.setId("team-1");
        team1.setName("First Team");
        team1.setCreatedAt(Instant.now().minusSeconds(3600));

        Team team2 = new Team();
        team2.setId("team-2");
        team2.setName("Second Team");
        team2.setCreatedAt(Instant.now());

        mockTeamData.getTeams().add(team1);
        mockTeamData.getTeams().add(team2);

        when(jsonRepository.readData()).thenReturn(mockTeamData);

        // Act
        List<Team> result = teamService.getAllTeams();

        // Assert
        assertEquals(2, result.size());
        assertEquals("Second Team", result.get(0).getName());
        assertEquals("First Team", result.get(1).getName());
    }

    @Test
    @DisplayName("Should get team by ID")
    void shouldGetTeamById() {
        // Arrange
        String teamId = "team-1";
        Team team = new Team();
        team.setId(teamId);
        team.setName("Test Team");
        team.setCreatedAt(Instant.now());
        mockTeamData.getTeams().add(team);

        when(jsonRepository.readData()).thenReturn(mockTeamData);

        // Act
        Team result = teamService.getTeamById(teamId);

        // Assert
        assertNotNull(result);
        assertEquals(teamId, result.getId());
        assertEquals("Test Team", result.getName());
    }

    @Test
    @DisplayName("Should throw exception when getting team with non-existent ID")
    void shouldThrowExceptionWhenGettingTeamWithNonExistentId() {
        // Arrange
        when(jsonRepository.readData()).thenReturn(mockTeamData);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            teamService.getTeamById("non-existent-id");
        });
    }

    @Test
    @DisplayName("Should update team name")
    void shouldUpdateTeamName() {
        // Arrange
        String teamId = "team-1";
        String oldName = "Old Name";
        String newName = "New Name";

        Team team = new Team();
        team.setId(teamId);
        team.setName(oldName);
        team.setCreatedAt(Instant.now());
        mockTeamData.getTeams().add(team);

        when(jsonRepository.readData()).thenReturn(mockTeamData);

        // Act
        Team result = teamService.updateTeamName(teamId, newName);

        // Assert
        assertNotNull(result);
        assertEquals(newName, result.getName());
        assertEquals(1, mockTeamData.getTeams().size());
    }

    @Test
    @DisplayName("Should throw exception when updating team name to duplicate")
    void shouldThrowExceptionWhenUpdatingTeamNameToDuplicate() {
        // Arrange
        String teamId = "team-1";
        String originalName = "Original Team";
        String duplicateName = "Duplicate Team";

        Team team1 = new Team();
        team1.setId(teamId);
        team1.setName(originalName);
        team1.setCreatedAt(Instant.now());

        Team team2 = new Team();
        team2.setId("team-2");
        team2.setName(duplicateName);
        team2.setCreatedAt(Instant.now());

        mockTeamData.getTeams().add(team1);
        mockTeamData.getTeams().add(team2);

        when(jsonRepository.readData()).thenReturn(mockTeamData);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            teamService.updateTeamName(teamId, duplicateName);
        });
    }

    @Test
    @DisplayName("Should delete team")
    void shouldDeleteTeam() {
        // Arrange
        String teamId = "team-1";
        Team team = new Team();
        team.setId(teamId);
        team.setName("Test Team");
        team.setCreatedAt(Instant.now());
        mockTeamData.getTeams().add(team);

        when(jsonRepository.readData()).thenReturn(mockTeamData);

        // Act
        teamService.deleteTeam(teamId);

        // Assert
        assertEquals(0, mockTeamData.getTeams().size());
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent team")
    void shouldThrowExceptionWhenDeletingNonExistentTeam() {
        // Arrange
        when(jsonRepository.readData()).thenReturn(mockTeamData);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            teamService.deleteTeam("non-existent-id");
        });
    }

    @Test
    @DisplayName("deleteTeam throws TeamNotEmptyException when team has players")
    void deleteTeam_whenTeamHasPlayers_throwsTeamNotEmptyException() {
        // Arrange — team with one player, no lineups
        String teamId = "team-with-players";
        Team team = new Team();
        team.setId(teamId);
        team.setName("Team With Players");
        team.setCreatedAt(Instant.now());
        Player player = new Player();
        player.setId("player-1");
        player.setName("Alice");
        team.setPlayers(new java.util.ArrayList<>(List.of(player)));
        team.setLineups(new java.util.ArrayList<>());
        mockTeamData.getTeams().add(team);

        when(jsonRepository.readData()).thenReturn(mockTeamData);

        // Act & Assert
        TeamNotEmptyException ex = assertThrows(
                TeamNotEmptyException.class,
                () -> teamService.deleteTeam(teamId));
        assertEquals(1, ex.getPlayerCount());
        assertEquals(0, ex.getLineupCount());
        assertEquals(1, mockTeamData.getTeams().size(), "team must NOT be removed");
        verify(jsonRepository, never()).writeData(any());
    }

    @Test
    @DisplayName("deleteTeam throws TeamNotEmptyException when team has only saved lineups")
    void deleteTeam_whenTeamHasOnlyLineups_throwsTeamNotEmptyException() {
        // Arrange — team with no players, but one saved lineup
        String teamId = "team-with-lineups";
        Team team = new Team();
        team.setId(teamId);
        team.setName("Team With Lineups");
        team.setCreatedAt(Instant.now());
        team.setPlayers(new java.util.ArrayList<>());
        Lineup lineup = new Lineup();
        lineup.setId("lineup-1");
        team.setLineups(new java.util.ArrayList<>(List.of(lineup)));
        mockTeamData.getTeams().add(team);

        when(jsonRepository.readData()).thenReturn(mockTeamData);

        // Act & Assert
        TeamNotEmptyException ex = assertThrows(
                TeamNotEmptyException.class,
                () -> teamService.deleteTeam(teamId));
        assertEquals(0, ex.getPlayerCount());
        assertEquals(1, ex.getLineupCount());
        assertEquals(1, mockTeamData.getTeams().size());
        verify(jsonRepository, never()).writeData(any());
    }

    @Test
    @DisplayName("deleteTeam throws TeamNotEmptyException when both players and lineups exist")
    void deleteTeam_whenBothPlayersAndLineups_throwsTeamNotEmptyException() {
        // Arrange — team with 2 players and 3 lineups
        String teamId = "team-both";
        Team team = new Team();
        team.setId(teamId);
        team.setName("Team Both");
        team.setCreatedAt(Instant.now());
        Player p1 = new Player(); p1.setId("p-1"); p1.setName("Alice");
        Player p2 = new Player(); p2.setId("p-2"); p2.setName("Bob");
        team.setPlayers(new java.util.ArrayList<>(List.of(p1, p2)));
        Lineup l1 = new Lineup(); l1.setId("l-1");
        Lineup l2 = new Lineup(); l2.setId("l-2");
        Lineup l3 = new Lineup(); l3.setId("l-3");
        team.setLineups(new java.util.ArrayList<>(List.of(l1, l2, l3)));
        mockTeamData.getTeams().add(team);

        when(jsonRepository.readData()).thenReturn(mockTeamData);

        // Act & Assert
        TeamNotEmptyException ex = assertThrows(
                TeamNotEmptyException.class,
                () -> teamService.deleteTeam(teamId));
        assertEquals(2, ex.getPlayerCount());
        assertEquals(3, ex.getLineupCount());
        assertEquals(1, mockTeamData.getTeams().size());
        verify(jsonRepository, never()).writeData(any());
    }

    @Test
    @DisplayName("Should generate unique team IDs")
    void shouldGenerateUniqueTeamIds() {
        // Arrange
        when(jsonRepository.readData()).thenReturn(mockTeamData);

        // Act
        Team team1 = teamService.createTeam("Team 1");
        Team team2 = teamService.createTeam("Team 2");

        // Assert
        assertNotEquals(team1.getId(), team2.getId());
        assertTrue(team1.getId().startsWith("team-"));
        assertTrue(team2.getId().startsWith("team-"));
    }
}