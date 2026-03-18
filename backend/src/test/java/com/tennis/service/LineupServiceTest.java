package com.tennis.service;

import com.tennis.exception.NotFoundException;
import com.tennis.model.Lineup;
import com.tennis.model.Pair;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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

    private int lineupCounter = 0;

    private Lineup buildLineup(String id) {
        return buildLineupWithTotalUtr(30.0, id);
    }

    private Lineup buildLineupWithTotalUtr(double totalUtr) {
        return buildLineupWithTotalUtr(totalUtr, null);
    }

    private Lineup buildLineupWithTotalUtr(double totalUtr, String id) {
        int slot = lineupCounter++;
        Lineup l = new Lineup();
        l.setId(id);
        l.setCreatedAt(Instant.now());
        l.setStrategy("balanced");
        l.setValid(true);
        l.setTotalUtr(totalUtr);
        double perPair = totalUtr / 4.0;
        List<Pair> pairs = new ArrayList<>();
        String[] positions = {"D1", "D2", "D3", "D4"};
        for (int i = 0; i < positions.length; i++) {
            Pair p = new Pair();
            p.setPosition(positions[i]);
            p.setCombinedUtr(perPair);
            // Use unique player IDs per lineup so dedup treats each lineup as distinct
            p.setPlayer1Id("p" + (slot * 8 + i * 2 + 1));
            p.setPlayer2Id("p" + (slot * 8 + i * 2 + 2));
            pairs.add(p);
        }
        l.setPairs(pairs);
        l.setViolationMessages(new ArrayList<>());
        return l;
    }

    @Test
    @DisplayName("generateMultiple returns list of lineups with id and timestamp")
    void testGenerateMultipleSuccess() {
        when(jsonRepository.readData()).thenReturn(teamData);
        Lineup candidate = buildLineup(null);
        when(generationService.generateCandidates(any(), any(), any(), any())).thenReturn(List.of(candidate));

        List<Lineup> results = lineupService.generateMultiple(
                "team-1", "preset", "balanced", null, List.of(), List.of());

        assertEquals(1, results.size());
        assertNotNull(results.get(0).getId());
        assertTrue(results.get(0).getId().startsWith("lineup-"));
        assertNotNull(results.get(0).getCreatedAt());
        assertEquals("balanced", results.get(0).getStrategy());
        // generateMultiple must NOT write to storage
        verify(jsonRepository, never()).writeData(any());
    }

    @Test
    @DisplayName("generateMultiple returns up to 6 candidates")
    void testGenerateMultipleReturnsSix() {
        when(jsonRepository.readData()).thenReturn(teamData);
        List<Lineup> manyCandidates = new ArrayList<>();
        for (int i = 0; i < 10; i++) manyCandidates.add(buildLineup(null));
        when(generationService.generateCandidates(any(), any(), any(), any())).thenReturn(manyCandidates);

        List<Lineup> results = lineupService.generateMultiple(
                "team-1", "preset", "balanced", null, List.of(), List.of());

        assertEquals(6, results.size());
    }

    @Test
    @DisplayName("generateMultiple does NOT persist any lineup to team")
    void testGenerateMultipleDoesNotPersist() {
        when(jsonRepository.readData()).thenReturn(teamData);
        Lineup c1 = buildLineup(null);
        Lineup c2 = buildLineup(null);
        when(generationService.generateCandidates(any(), any(), any(), any())).thenReturn(List.of(c1, c2));

        lineupService.generateMultiple(
                "team-1", "preset", "balanced", null, List.of(), List.of());

        // team.lineups must remain empty — nothing auto-saved
        assertTrue(team.getLineups().isEmpty());
        verify(jsonRepository, never()).writeData(any());
    }

    @Test
    @DisplayName("generateMultiple throws when team has fewer than 8 players")
    void testGenerateThrowsForInsufficientPlayers() {
        team.setPlayers(buildPlayers(7));
        when(jsonRepository.readData()).thenReturn(teamData);

        assertThrows(IllegalArgumentException.class, () ->
                lineupService.generateMultiple("team-1", "preset", "balanced", null, List.of(), List.of()));
    }

    @Test
    @DisplayName("generateMultiple throws NotFoundException for unknown team")
    void testGenerateThrowsForUnknownTeam() {
        when(jsonRepository.readData()).thenReturn(teamData);

        assertThrows(NotFoundException.class, () ->
                lineupService.generateMultiple("unknown-team", "preset", "balanced", null, List.of(), List.of()));
    }

    @Test
    @DisplayName("generateMultiple throws when no valid lineup exists")
    void testGenerateThrowsWhenNoValidLineup() {
        when(jsonRepository.readData()).thenReturn(teamData);
        when(generationService.generateCandidates(any(), any(), any(), any())).thenReturn(new ArrayList<>());

        assertThrows(IllegalArgumentException.class, () ->
                lineupService.generateMultiple("team-1", "preset", "balanced", null, List.of(), List.of()));
    }

    @Test
    @DisplayName("balanced strategy: lineup closest to 40.5 total UTR ranked first")
    void testBalancedPrimaryRankByUtrProximity() {
        when(jsonRepository.readData()).thenReturn(teamData);

        Lineup c1 = buildLineupWithTotalUtr(39.0);
        Lineup c2 = buildLineupWithTotalUtr(38.0);
        when(generationService.generateCandidates(any(), any(), any(), any())).thenReturn(List.of(c2, c1));

        List<Lineup> results = lineupService.generateMultiple(
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

        lineupService.generateMultiple("team-1", "preset", "balanced", null,
                List.of(), List.of(), Map.of("p1", "D1"));

        verify(generationService).generateCandidates(any(), any(), any(),
                argThat(pins -> pins.containsKey("p1") && "D1".equals(pins.get("p1"))));
    }

    @Test
    @DisplayName("includePlayers passed through to generation service")
    void testIncludePlayersPassedToGenerationService() {
        when(jsonRepository.readData()).thenReturn(teamData);
        Lineup candidate = buildLineup(null);
        when(generationService.generateCandidates(any(), any(), any(), any())).thenReturn(List.of(candidate));

        lineupService.generateMultiple("team-1", "preset", "balanced", null,
                List.of("p1", "p2"), List.of());

        verify(generationService).generateCandidates(any(),
                argThat(inc -> inc.contains("p1") && inc.contains("p2")),
                any(), any());
    }

    @Test
    @DisplayName("saveLineup persists lineup to team and returns it with id")
    void testSaveLineupPersistsAndReturns() {
        when(jsonRepository.readData()).thenReturn(teamData);

        Lineup lineup = buildLineup(null); // no id
        lineup.setId(null);
        lineup.setCreatedAt(null);

        Lineup saved = lineupService.saveLineup("team-1", lineup);

        assertNotNull(saved.getId());
        assertTrue(saved.getId().startsWith("lineup-"));
        assertNotNull(saved.getCreatedAt());
        assertEquals(1, team.getLineups().size());
        verify(jsonRepository).writeData(any());
    }

    @Test
    @DisplayName("saveLineup preserves existing id if already set")
    void testSaveLineupPreservesExistingId() {
        when(jsonRepository.readData()).thenReturn(teamData);

        Lineup lineup = buildLineup("existing-id-123");
        lineup.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));

        Lineup saved = lineupService.saveLineup("team-1", lineup);

        assertEquals("existing-id-123", saved.getId());
        verify(jsonRepository).writeData(any());
    }

    @Test
    @DisplayName("saveLineup throws NotFoundException for unknown team")
    void testSaveLineupUnknownTeam() {
        when(jsonRepository.readData()).thenReturn(teamData);

        assertThrows(NotFoundException.class, () ->
                lineupService.saveLineup("unknown-team", buildLineup(null)));
    }

    @Test
    @DisplayName("saveLineup rejects duplicate: same player set already saved")
    void testSaveLineupRejectsDuplicate() {
        when(jsonRepository.readData()).thenReturn(teamData);

        // Build two lineups explicitly with the same 8 player IDs
        Lineup first = buildLineupWithFixedPlayers("dup-a", "dup-b");
        lineupService.saveLineup("team-1", first);

        Lineup duplicate = buildLineupWithFixedPlayers("dup-a", "dup-b");
        assertThrows(IllegalArgumentException.class, () ->
                lineupService.saveLineup("team-1", duplicate));
    }

    private Lineup buildLineupWithFixedPlayers(String p1, String p2) {
        Lineup l = new Lineup();
        l.setStrategy("balanced");
        l.setValid(true);
        l.setTotalUtr(28.0);
        l.setViolationMessages(new ArrayList<>());
        List<Pair> pairs = new ArrayList<>();
        for (String pos : new String[]{"D1", "D2", "D3", "D4"}) {
            Pair p = new Pair();
            p.setPosition(pos);
            p.setPlayer1Id(p1);
            p.setPlayer2Id(p2);
            p.setCombinedUtr(7.0);
            pairs.add(p);
        }
        l.setPairs(pairs);
        return l;
    }

    @Test
    @DisplayName("saveLineup accepts non-duplicate: different player set")
    void testSaveLineupAcceptsNonDuplicate() {
        when(jsonRepository.readData()).thenReturn(teamData);

        // Save first lineup (uses p1/p2)
        Lineup first = buildLineup(null);
        lineupService.saveLineup("team-1", first);

        // Build a second lineup with different player IDs
        Lineup different = new Lineup();
        different.setStrategy("balanced");
        different.setValid(true);
        different.setTotalUtr(28.0);
        different.setViolationMessages(new ArrayList<>());
        String[] positions = {"D1", "D2", "D3", "D4"};
        List<Pair> pairs = new ArrayList<>();
        for (String pos : positions) {
            Pair p = new Pair();
            p.setPosition(pos);
            p.setPlayer1Id("p3");
            p.setPlayer2Id("p4");
            p.setCombinedUtr(7.0);
            pairs.add(p);
        }
        different.setPairs(pairs);

        Lineup saved = lineupService.saveLineup("team-1", different);
        assertNotNull(saved.getId());
        assertEquals(2, team.getLineups().size());
    }

    @Test
    @DisplayName("saveLineup first save always accepted when team has no lineups")
    void testSaveLineupFirstSaveAlwaysAccepted() {
        // team.lineups is empty by default from setUp()
        when(jsonRepository.readData()).thenReturn(teamData);

        Lineup lineup = buildLineup(null);
        Lineup saved = lineupService.saveLineup("team-1", lineup);

        assertNotNull(saved.getId());
        assertEquals(1, team.getLineups().size());
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
    @DisplayName("getLineupsByTeam enriches pairs missing UTR and gender fields")
    void testGetLineupsByTeamEnrichment() {
        // Set up a player in the team
        Player p1 = new Player();
        p1.setId("player-a");
        p1.setName("Alice");
        p1.setGender("female");
        p1.setUtr(5.5);
        Player p2 = new Player();
        p2.setId("player-b");
        p2.setName("Bob");
        p2.setGender("male");
        p2.setUtr(6.0);
        team.setPlayers(List.of(p1, p2));

        // Build a lineup with a pair missing UTR/gender (old format)
        Lineup lineup = new Lineup();
        lineup.setId("lineup-old");
        lineup.setCreatedAt(Instant.now());
        lineup.setStrategy("balanced");
        lineup.setViolationMessages(new ArrayList<>());

        Pair pair = new Pair();
        pair.setPosition("D1");
        pair.setPlayer1Id("player-a");
        pair.setPlayer2Id("player-b");
        pair.setCombinedUtr(11.5);
        // player1Utr, player2Utr, player1Gender, player2Gender intentionally null (old format)
        lineup.setPairs(List.of(pair));

        team.setLineups(new ArrayList<>(List.of(lineup)));
        when(jsonRepository.readData()).thenReturn(teamData);

        List<Lineup> result = lineupService.getLineupsByTeam("team-1");

        assertEquals(1, result.size());
        Pair enriched = result.get(0).getPairs().get(0);
        assertEquals(5.5, enriched.getPlayer1Utr(), 0.001);
        assertEquals("female", enriched.getPlayer1Gender());
        assertEquals(6.0, enriched.getPlayer2Utr(), 0.001);
        assertEquals("male", enriched.getPlayer2Gender());
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
}
