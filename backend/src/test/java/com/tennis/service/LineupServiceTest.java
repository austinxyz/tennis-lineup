package com.tennis.service;

import com.tennis.exception.NotFoundException;
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

import com.tennis.model.Pair;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LineupService Test")
class LineupServiceTest {

    @Mock
    private JsonRepository jsonRepository;
    @Mock
    private LineupGenerationService generationService;
    @Mock
    private ZhipuAiService aiService;

    @InjectMocks
    private LineupService lineupService;

    private Team team;
    private TeamData teamData;

    @BeforeEach
    void setUp() {
        team = new Team();
        team.setId("team-1");
        team.setName("Test Team");
        team.setPlayers(buildPlayers(8));
        team.setLineups(new ArrayList<>());

        teamData = new TeamData();
        teamData.setTeams(new ArrayList<>(List.of(team)));
    }

    private List<Player> buildPlayers(int count) {
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Player p = new Player();
            p.setId("p" + (i + 1));
            p.setName("Player " + (i + 1));
            p.setGender(i % 4 == 0 ? "female" : "male");
            p.setUtr(6.0 - i * 0.3);
            p.setVerified(true);
            players.add(p);
        }
        return players;
    }

    private Lineup buildLineup(String id) {
        return buildLineupWithTotalUtr(30.0, id);
    }

    private Lineup buildLineupWithTotalUtr(double totalUtr) {
        return buildLineupWithTotalUtr(totalUtr, null);
    }

    private Lineup buildLineupWithTotalUtr(double totalUtr, String id) {
        Lineup l = new Lineup();
        l.setId(id);
        l.setCreatedAt(Instant.now());
        l.setStrategy("balanced");
        l.setValid(true);
        l.setTotalUtr(totalUtr);
        // Add 4 pairs so sorting can work (combinedUtr = totalUtr/4 each)
        double perPair = totalUtr / 4.0;
        List<Pair> pairs = new ArrayList<>();
        String[] positions = {"D1", "D2", "D3", "D4"};
        for (String pos : positions) {
            Pair p = new Pair();
            p.setPosition(pos);
            p.setCombinedUtr(perPair);
            p.setPlayer1Id("p1");
            p.setPlayer2Id("p2");
            pairs.add(p);
        }
        l.setPairs(pairs);
        l.setViolationMessages(new ArrayList<>());
        return l;
    }

    @Test
    @DisplayName("generateMultipleAndSave returns list of lineups with id and timestamp")
    void testGenerateMultipleSuccess() {
        when(jsonRepository.readData()).thenReturn(teamData);
        Lineup candidate = buildLineup(null);
        when(generationService.generateCandidates(any(), any(), any(), any())).thenReturn(List.of(candidate));
        when(aiService.selectBestLineup(any(), any())).thenReturn(-1);

        List<Lineup> results = lineupService.generateMultipleAndSave(
                "team-1", "preset", "balanced", null, List.of(), List.of());

        assertEquals(1, results.size());
        assertNotNull(results.get(0).getId());
        assertTrue(results.get(0).getId().startsWith("lineup-"));
        assertNotNull(results.get(0).getCreatedAt());
        assertEquals("balanced", results.get(0).getStrategy());
        verify(jsonRepository).writeData(any());
    }

    @Test
    @DisplayName("generateMultipleAndSave returns up to 6 candidates")
    void testGenerateMultipleReturnsSix() {
        when(jsonRepository.readData()).thenReturn(teamData);
        List<Lineup> manyCandidates = new ArrayList<>();
        for (int i = 0; i < 10; i++) manyCandidates.add(buildLineup(null));
        when(generationService.generateCandidates(any(), any(), any(), any())).thenReturn(manyCandidates);
        when(aiService.selectBestLineup(any(), any())).thenReturn(-1);

        List<Lineup> results = lineupService.generateMultipleAndSave(
                "team-1", "preset", "balanced", null, List.of(), List.of());

        assertTrue(results.size() <= 6);
        assertEquals(6, results.size());
    }

    @Test
    @DisplayName("generateMultipleAndSave saves only the first lineup to team")
    void testOnlyFirstLineupPersisted() {
        when(jsonRepository.readData()).thenReturn(teamData);
        Lineup c1 = buildLineup(null);
        Lineup c2 = buildLineup(null);
        when(generationService.generateCandidates(any(), any(), any(), any())).thenReturn(List.of(c1, c2));
        when(aiService.selectBestLineup(any(), any())).thenReturn(-1);

        List<Lineup> results = lineupService.generateMultipleAndSave(
                "team-1", "preset", "balanced", null, List.of(), List.of());

        assertEquals(1, team.getLineups().size());
        assertEquals(results.get(0).getId(), team.getLineups().get(0).getId());
    }

    @Test
    @DisplayName("generateMultipleAndSave throws when team has fewer than 8 players")
    void testGenerateThrowsForInsufficientPlayers() {
        team.setPlayers(buildPlayers(7));
        when(jsonRepository.readData()).thenReturn(teamData);

        assertThrows(IllegalArgumentException.class, () ->
                lineupService.generateMultipleAndSave("team-1", "preset", "balanced", null, List.of(), List.of()));
    }

    @Test
    @DisplayName("generateMultipleAndSave throws NotFoundException for unknown team")
    void testGenerateThrowsForUnknownTeam() {
        when(jsonRepository.readData()).thenReturn(teamData);

        assertThrows(NotFoundException.class, () ->
                lineupService.generateMultipleAndSave("unknown-team", "preset", "balanced", null, List.of(), List.of()));
    }

    @Test
    @DisplayName("generateMultipleAndSave throws when no valid lineup exists")
    void testGenerateThrowsWhenNoValidLineup() {
        when(jsonRepository.readData()).thenReturn(teamData);
        when(generationService.generateCandidates(any(), any(), any(), any())).thenReturn(new ArrayList<>());

        assertThrows(IllegalArgumentException.class, () ->
                lineupService.generateMultipleAndSave("team-1", "preset", "balanced", null, List.of(), List.of()));
    }

    @Test
    @DisplayName("balanced strategy: lineup closest to 40.5 total UTR ranked first")
    void testBalancedPrimaryRankByUtRProximity() {
        when(jsonRepository.readData()).thenReturn(teamData);

        // c1 has totalUtr=39.0 (distance 1.5), c2 has totalUtr=38.0 (distance 2.5)
        // c1 should rank first (closer to cap)
        Lineup c1 = buildLineupWithTotalUtr(39.0);
        Lineup c2 = buildLineupWithTotalUtr(38.0);
        when(generationService.generateCandidates(any(), any(), any(), any())).thenReturn(List.of(c2, c1));
        when(aiService.selectBestLineup(any(), any())).thenReturn(-1);

        List<Lineup> results = lineupService.generateMultipleAndSave(
                "team-1", "preset", "balanced", null, List.of(), List.of());

        assertEquals(2, results.size());
        assertEquals(39.0, results.get(0).getTotalUtr(), 0.001);
        assertEquals(38.0, results.get(1).getTotalUtr(), 0.001);
    }

    @Test
    @DisplayName("pinPlayers passed through to generation service")
    void testPinPlayersPassedToGenerationService() {
        when(jsonRepository.readData()).thenReturn(teamData);
        Lineup candidate = buildLineup(null);
        when(generationService.generateCandidates(any(), any(), any(), any())).thenReturn(List.of(candidate));
        when(aiService.selectBestLineup(any(), any())).thenReturn(-1);

        lineupService.generateMultipleAndSave("team-1", "preset", "balanced", null,
                List.of(), List.of(), Map.of("p1", "D1"));

        verify(generationService).generateCandidates(any(), any(), any(),
                argThat(pins -> pins.containsKey("p1") && "D1".equals(pins.get("p1"))));
    }

    @Test
    @DisplayName("getLineupsByTeam returns lineups in reverse chronological order")
    void testGetLineupsByTeamReturnsReverseChronological() {
        Lineup old = buildLineup("lineup-1");
        old.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        Lineup newer = buildLineup("lineup-2");
        newer.setCreatedAt(Instant.parse("2026-02-01T00:00:00Z"));
        team.setLineups(new ArrayList<>(Arrays.asList(old, newer)));
        when(jsonRepository.readData()).thenReturn(teamData);

        List<Lineup> result = lineupService.getLineupsByTeam("team-1");

        assertEquals(2, result.size());
        assertEquals("lineup-2", result.get(0).getId());
        assertEquals("lineup-1", result.get(1).getId());
    }

    @Test
    @DisplayName("deleteLineup removes lineup from team")
    void testDeleteLineupSuccess() {
        Lineup lineup = buildLineup("lineup-to-delete");
        team.setLineups(new ArrayList<>(List.of(lineup)));
        when(jsonRepository.readData()).thenReturn(teamData);

        lineupService.deleteLineup("lineup-to-delete");

        verify(jsonRepository).writeData(any());
        assertTrue(team.getLineups().isEmpty());
    }

    @Test
    @DisplayName("deleteLineup throws NotFoundException for non-existent lineup")
    void testDeleteLineupThrowsNotFound() {
        when(jsonRepository.readData()).thenReturn(teamData);

        assertThrows(NotFoundException.class, () -> lineupService.deleteLineup("nonexistent"));
    }

    @Test
    @DisplayName("includePlayers passed through to generation service")
    void testIncludePlayersPassedToGenerationService() {
        when(jsonRepository.readData()).thenReturn(teamData);
        Lineup candidate = buildLineup(null);
        when(generationService.generateCandidates(any(), any(), any(), any())).thenReturn(List.of(candidate));
        when(aiService.selectBestLineup(any(), any())).thenReturn(-1);

        lineupService.generateMultipleAndSave("team-1", "preset", "balanced", null,
                List.of("p1", "p2"), List.of());

        verify(generationService).generateCandidates(any(),
                argThat(inc -> inc.contains("p1") && inc.contains("p2")),
                any(), any());
    }
}
