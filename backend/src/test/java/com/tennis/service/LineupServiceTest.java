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
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
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
    @Mock
    private ConstraintService constraintService;

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

        // Default: validateLineup returns valid with no violations
        lenient().when(constraintService.validateLineup(any(), any()))
                .thenReturn(new ConstraintService.ValidationResult(true, List.of()));
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

    // ======================== UTR re-validation tests ========================

    @Test
    @DisplayName("getLineupsByTeam sets currentValid=true when lineup passes re-validation")
    void testGetLineupsByTeamSetsCurrentValidTrue() {
        Lineup lineup = buildLineup("lineup-valid");
        lineup.setCreatedAt(Instant.now());
        team.setLineups(new ArrayList<>(List.of(lineup)));
        when(jsonRepository.readData()).thenReturn(teamData);
        when(constraintService.validateLineup(any(), any()))
                .thenReturn(new ConstraintService.ValidationResult(true, List.of()));

        List<Lineup> result = lineupService.getLineupsByTeam("team-1");

        assertEquals(1, result.size());
        assertTrue(result.get(0).isCurrentValid());
        assertTrue(result.get(0).getCurrentViolations().isEmpty());
    }

    @Test
    @DisplayName("getLineupsByTeam sets currentValid=false with violations when UTR changed")
    void testGetLineupsByTeamSetsCurrentValidFalseOnViolation() {
        Lineup lineup = buildLineup("lineup-invalid");
        lineup.setCreatedAt(Instant.now());
        team.setLineups(new ArrayList<>(List.of(lineup)));
        when(jsonRepository.readData()).thenReturn(teamData);

        List<String> violations = List.of("总UTR超过40.5: 41.0", "搭档UTR差超过3.5: D1");
        when(constraintService.validateLineup(any(), any()))
                .thenReturn(new ConstraintService.ValidationResult(false, violations));

        List<Lineup> result = lineupService.getLineupsByTeam("team-1");

        assertEquals(1, result.size());
        assertFalse(result.get(0).isCurrentValid());
        assertEquals(2, result.get(0).getCurrentViolations().size());
        assertTrue(result.get(0).getCurrentViolations().contains("总UTR超过40.5: 41.0"));
    }

    @Test
    @DisplayName("getLineupsByTeam re-validates all lineups with current player list")
    void testGetLineupsByTeamReValidatesAllLineups() {
        Lineup l1 = buildLineup("lineup-1");
        l1.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        Lineup l2 = buildLineup("lineup-2");
        l2.setCreatedAt(Instant.parse("2026-02-01T00:00:00Z"));
        team.setLineups(new ArrayList<>(List.of(l1, l2)));
        when(jsonRepository.readData()).thenReturn(teamData);

        lineupService.getLineupsByTeam("team-1");

        verify(constraintService, times(2)).validateLineup(any(), eq(team.getPlayers()));
    }

    // ======================== updateLineup tests ========================

    /** Sets up updateData mock to execute the transform lambda against teamData. */
    @SuppressWarnings("unchecked")
    private void mockUpdateData() {
        doAnswer(inv -> {
            UnaryOperator<TeamData> transform = inv.getArgument(0);
            transform.apply(teamData);
            return null;
        }).when(jsonRepository).updateData(any());
    }

    @Test
    @DisplayName("updateLineup: update label only leaves other fields unchanged")
    void testUpdateLineupLabelOnly() {
        Lineup existing = buildLineup("lineup-upd-1");
        existing.setLabel(null);
        existing.setComment("original comment");
        existing.setSortOrder(0);
        team.setLineups(new ArrayList<>(List.of(existing)));
        mockUpdateData();

        com.tennis.controller.LineupUpdateRequest req = new com.tennis.controller.LineupUpdateRequest();
        req.setLabel("My Label");

        Lineup updated = lineupService.updateLineup("team-1", "lineup-upd-1", req);

        assertEquals("My Label", updated.getLabel());
        assertEquals("original comment", updated.getComment());
        assertEquals(0, updated.getSortOrder());
        verify(jsonRepository).updateData(any());
    }

    @Test
    @DisplayName("updateLineup: blank string clears label to null")
    void testUpdateLineupBlankLabelClears() {
        Lineup existing = buildLineup("lineup-upd-blank");
        existing.setLabel("existing label");
        team.setLineups(new ArrayList<>(List.of(existing)));
        mockUpdateData();

        com.tennis.controller.LineupUpdateRequest req = new com.tennis.controller.LineupUpdateRequest();
        req.setLabel("   "); // blank = explicit clear

        Lineup updated = lineupService.updateLineup("team-1", "lineup-upd-blank", req);

        assertNull(updated.getLabel());
        verify(jsonRepository).updateData(any());
    }

    @Test
    @DisplayName("updateLineup: blank string clears comment to null")
    void testUpdateLineupBlankCommentClears() {
        Lineup existing = buildLineup("lineup-upd-blank2");
        existing.setComment("existing comment");
        team.setLineups(new ArrayList<>(List.of(existing)));
        mockUpdateData();

        com.tennis.controller.LineupUpdateRequest req = new com.tennis.controller.LineupUpdateRequest();
        req.setComment("");

        Lineup updated = lineupService.updateLineup("team-1", "lineup-upd-blank2", req);

        assertNull(updated.getComment());
        verify(jsonRepository).updateData(any());
    }

    @Test
    @DisplayName("updateLineup: update comment only leaves other fields unchanged")
    void testUpdateLineupCommentOnly() {
        Lineup existing = buildLineup("lineup-upd-2");
        existing.setLabel("existing label");
        existing.setComment(null);
        existing.setSortOrder(2);
        team.setLineups(new ArrayList<>(List.of(existing)));
        mockUpdateData();

        com.tennis.controller.LineupUpdateRequest req = new com.tennis.controller.LineupUpdateRequest();
        req.setComment("new note");

        Lineup updated = lineupService.updateLineup("team-1", "lineup-upd-2", req);

        assertEquals("existing label", updated.getLabel());
        assertEquals("new note", updated.getComment());
        assertEquals(2, updated.getSortOrder());
        verify(jsonRepository).updateData(any());
    }

    @Test
    @DisplayName("updateLineup: update sortOrder only leaves other fields unchanged")
    void testUpdateLineupSortOrderOnly() {
        Lineup existing = buildLineup("lineup-upd-3");
        existing.setLabel("lbl");
        existing.setComment("cmt");
        existing.setSortOrder(0);
        team.setLineups(new ArrayList<>(List.of(existing)));
        mockUpdateData();

        com.tennis.controller.LineupUpdateRequest req = new com.tennis.controller.LineupUpdateRequest();
        req.setSortOrder(5);

        Lineup updated = lineupService.updateLineup("team-1", "lineup-upd-3", req);

        assertEquals("lbl", updated.getLabel());
        assertEquals("cmt", updated.getComment());
        assertEquals(5, updated.getSortOrder());
        verify(jsonRepository).updateData(any());
    }

    @Test
    @DisplayName("updateLineup: update pairs stores defensive copy")
    void testUpdateLineupPairs() {
        Lineup existing = buildLineupWithFixedPlayers("old-p1", "old-p2");
        existing.setId("lineup-upd-4");
        existing.setCreatedAt(Instant.now());
        team.setLineups(new ArrayList<>(List.of(existing)));
        when(jsonRepository.readData()).thenReturn(teamData); // for validatePairsNotDuplicate
        mockUpdateData();

        Pair newPair = new Pair();
        newPair.setPosition("D1");
        newPair.setPlayer1Id("new-p1");
        newPair.setPlayer1Name("New Player 1");
        newPair.setPlayer2Id("new-p2");
        newPair.setPlayer2Name("New Player 2");
        newPair.setCombinedUtr(10.0);

        com.tennis.controller.LineupUpdateRequest req = new com.tennis.controller.LineupUpdateRequest();
        req.setPairs(List.of(newPair));

        Lineup updated = lineupService.updateLineup("team-1", "lineup-upd-4", req);

        assertEquals(1, updated.getPairs().size());
        assertEquals("new-p1", updated.getPairs().get(0).getPlayer1Id());
        verify(jsonRepository).updateData(any());
    }

    @Test
    @DisplayName("updateLineup: throws NotFoundException when lineup not found")
    void testUpdateLineupNotFound() {
        team.setLineups(new ArrayList<>());
        mockUpdateData();

        com.tennis.controller.LineupUpdateRequest req = new com.tennis.controller.LineupUpdateRequest();
        req.setLabel("label");

        assertThrows(NotFoundException.class, () ->
                lineupService.updateLineup("team-1", "nonexistent-lineup", req));
    }

    @Test
    @DisplayName("updateLineup: throws NotFoundException when team not found")
    void testUpdateLineupTeamNotFound() {
        mockUpdateData();

        com.tennis.controller.LineupUpdateRequest req = new com.tennis.controller.LineupUpdateRequest();
        req.setLabel("label");

        assertThrows(NotFoundException.class, () ->
                lineupService.updateLineup("unknown-team", "lineup-1", req));
    }

    @Test
    @DisplayName("updateLineup: throws when updated pairs have SAME pairings as another lineup")
    void testUpdateLineupDuplicatePairsThrows() {
        // lineup1 uses pairing p1+p2 for every position
        Lineup lineup1 = buildLineupWithFixedPlayers("dup-x1", "dup-x2");
        lineup1.setId("lineup-dup-a");
        lineup1.setCreatedAt(Instant.now());
        Lineup lineup2 = buildLineupWithFixedPlayers("dup-y1", "dup-y2");
        lineup2.setId("lineup-dup-b");
        lineup2.setCreatedAt(Instant.now());
        team.setLineups(new ArrayList<>(Arrays.asList(lineup1, lineup2)));
        when(jsonRepository.readData()).thenReturn(teamData);

        // Update lineup2 with the SAME pairing as lineup1 (4 identical pairs of dup-x1,dup-x2)
        com.tennis.controller.LineupUpdateRequest req = new com.tennis.controller.LineupUpdateRequest();
        req.setPairs(new ArrayList<>(lineup1.getPairs()));

        assertThrows(IllegalArgumentException.class, () ->
                lineupService.updateLineup("team-1", "lineup-dup-b", req));
    }

    @Test
    @DisplayName("updateLineup: allows same 8 players in different pairings (different lineup)")
    void testUpdateLineupSamePlayersDifferentPairingsAllowed() {
        // lineup1 has pairs: D1(p1,p2), D2(p3,p4)
        Lineup lineup1 = new Lineup();
        lineup1.setId("lineup-pairA");
        lineup1.setCreatedAt(Instant.now());
        lineup1.setStrategy("balanced");
        lineup1.setValid(true);
        lineup1.setTotalUtr(28.0);
        lineup1.setViolationMessages(new ArrayList<>());
        Pair p1a = new Pair(); p1a.setPosition("D1"); p1a.setPlayer1Id("p1"); p1a.setPlayer2Id("p2"); p1a.setCombinedUtr(10.0);
        Pair p1b = new Pair(); p1b.setPosition("D2"); p1b.setPlayer1Id("p3"); p1b.setPlayer2Id("p4"); p1b.setCombinedUtr(8.0);
        lineup1.setPairs(new ArrayList<>(Arrays.asList(p1a, p1b)));

        // lineup2 will be updated with same 4 players but REGROUPED: D1(p1,p3), D2(p2,p4)
        Lineup lineup2 = new Lineup();
        lineup2.setId("lineup-pairB");
        lineup2.setCreatedAt(Instant.now());
        lineup2.setStrategy("balanced");
        lineup2.setValid(true);
        lineup2.setTotalUtr(28.0);
        lineup2.setViolationMessages(new ArrayList<>());
        Pair p2a = new Pair(); p2a.setPosition("D1"); p2a.setPlayer1Id("px"); p2a.setPlayer2Id("py"); p2a.setCombinedUtr(10.0);
        lineup2.setPairs(new ArrayList<>(List.of(p2a)));

        team.setLineups(new ArrayList<>(Arrays.asList(lineup1, lineup2)));
        when(jsonRepository.readData()).thenReturn(teamData);
        mockUpdateData();

        // Update lineup2 with same 4 players but different pairings
        Pair newPairA = new Pair();
        newPairA.setPosition("D1"); newPairA.setPlayer1Id("p1"); newPairA.setPlayer2Id("p3"); newPairA.setCombinedUtr(9.0);
        Pair newPairB = new Pair();
        newPairB.setPosition("D2"); newPairB.setPlayer1Id("p2"); newPairB.setPlayer2Id("p4"); newPairB.setCombinedUtr(9.0);

        com.tennis.controller.LineupUpdateRequest req = new com.tennis.controller.LineupUpdateRequest();
        req.setPairs(List.of(newPairA, newPairB));

        // Should succeed because pairings differ (p1+p3, p2+p4 vs lineup1's p1+p2, p3+p4)
        assertDoesNotThrow(() -> lineupService.updateLineup("team-1", "lineup-pairB", req));
    }

    @Test
    @DisplayName("updateLineup: null fields are ignored and no write occurs")
    void testUpdateLineupNullFieldsIgnored() {
        Lineup existing = buildLineup("lineup-upd-5");
        existing.setLabel("keep-label");
        existing.setComment("keep-comment");
        existing.setSortOrder(3);
        team.setLineups(new ArrayList<>(List.of(existing)));
        mockUpdateData();

        com.tennis.controller.LineupUpdateRequest req = new com.tennis.controller.LineupUpdateRequest();

        Lineup updated = lineupService.updateLineup("team-1", "lineup-upd-5", req);

        assertEquals("keep-label", updated.getLabel());
        assertEquals("keep-comment", updated.getComment());
        assertEquals(3, updated.getSortOrder());
        // updateData is still called (lambda invoked), but internally skips writeDataUnlocked
        verify(jsonRepository).updateData(any());
    }

    // ======================== getLineupsByTeam sort order tests ========================

    @Test
    @DisplayName("getLineupsByTeam: lineups with all sortOrder=0 remain in createdAt desc order")
    void testGetLineupsByTeamDefaultSortCreatedAtDesc() {
        Lineup old = buildLineup("lineup-sort-1");
        old.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        old.setSortOrder(0);
        Lineup newer = buildLineup("lineup-sort-2");
        newer.setCreatedAt(Instant.parse("2026-02-01T00:00:00Z"));
        newer.setSortOrder(0);
        team.setLineups(new ArrayList<>(Arrays.asList(old, newer)));
        when(jsonRepository.readData()).thenReturn(teamData);

        List<Lineup> result = lineupService.getLineupsByTeam("team-1");

        // Both have sortOrder=0; tie-break by createdAt desc → newer first
        assertEquals("lineup-sort-2", result.get(0).getId());
        assertEquals("lineup-sort-1", result.get(1).getId());
    }

    @Test
    @DisplayName("getLineupsByTeam: explicit sortOrder values are respected ascending")
    void testGetLineupsByTeamExplicitSortOrder() {
        Lineup l1 = buildLineup("lineup-ord-1");
        l1.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        l1.setSortOrder(2);
        Lineup l2 = buildLineup("lineup-ord-2");
        l2.setCreatedAt(Instant.parse("2026-02-01T00:00:00Z"));
        l2.setSortOrder(1);
        team.setLineups(new ArrayList<>(Arrays.asList(l1, l2)));
        when(jsonRepository.readData()).thenReturn(teamData);

        List<Lineup> result = lineupService.getLineupsByTeam("team-1");

        // sortOrder 1 comes before sortOrder 2
        assertEquals("lineup-ord-2", result.get(0).getId());
        assertEquals("lineup-ord-1", result.get(1).getId());
    }

    @Test
    @DisplayName("getLineupsByTeam: same sortOrder breaks tie by createdAt desc")
    void testGetLineupsByTeamSameSortOrderTieBreakByCreatedAt() {
        Lineup l1 = buildLineup("lineup-tie-1");
        l1.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        l1.setSortOrder(1);
        Lineup l2 = buildLineup("lineup-tie-2");
        l2.setCreatedAt(Instant.parse("2026-03-01T00:00:00Z"));
        l2.setSortOrder(1);
        team.setLineups(new ArrayList<>(Arrays.asList(l1, l2)));
        when(jsonRepository.readData()).thenReturn(teamData);

        List<Lineup> result = lineupService.getLineupsByTeam("team-1");

        // Same sortOrder=1; newer createdAt first
        assertEquals("lineup-tie-2", result.get(0).getId());
        assertEquals("lineup-tie-1", result.get(1).getId());
    }

    // ======================== import/export label/comment/sortOrder tests ========================

    @Test
    @DisplayName("exportLineups: includes label, comment, sortOrder in envelope")
    void testExportIncludesAllFields() {
        Lineup lineup = buildLineup("lineup-exp-1");
        lineup.setLabel("赛前主选");
        lineup.setComment("D3稳健");
        lineup.setSortOrder(2);
        team.setLineups(new ArrayList<>(List.of(lineup)));
        when(jsonRepository.readData()).thenReturn(teamData);

        Map<String, Object> envelope = lineupService.exportLineups("team-1");

        @SuppressWarnings("unchecked")
        List<Lineup> exported = (List<Lineup>) envelope.get("lineups");
        assertEquals(1, exported.size());
        assertEquals("赛前主选", exported.get(0).getLabel());
        assertEquals("D3稳健", exported.get(0).getComment());
        assertEquals(2, exported.get(0).getSortOrder());
    }

    @Test
    @DisplayName("importLineups: preserves label and comment from source")
    void testImportPreservesLabelAndComment() {
        team.setLineups(new ArrayList<>());
        when(jsonRepository.readData()).thenReturn(teamData);

        Lineup incoming = buildLineupWithFixedPlayers("p1", "p2");
        incoming.setId("src-lineup-1");
        incoming.setLabel("从源环境导入");
        incoming.setComment("备注内容");
        incoming.setSortOrder(5);

        Map<String, Integer> result = lineupService.importLineups("team-1", List.of(incoming));

        assertEquals(1, result.get("imported"));
        assertEquals(1, team.getLineups().size());
        Lineup stored = team.getLineups().get(0);
        assertEquals("从源环境导入", stored.getLabel());
        assertEquals("备注内容", stored.getComment());
        // ID is reassigned
        assertNotEquals("src-lineup-1", stored.getId());
    }

    @Test
    @DisplayName("importLineups: assigns sortOrder after existing lineups, preserving relative order")
    void testImportSortOrderNormalization() {
        // Existing: 2 lineups with sortOrder 0, 1
        Lineup existing1 = buildLineupWithFixedPlayers("ex1a", "ex1b");
        existing1.setId("existing-1");
        existing1.setSortOrder(0);
        Lineup existing2 = buildLineupWithFixedPlayers("ex2a", "ex2b");
        existing2.setId("existing-2");
        existing2.setSortOrder(1);
        team.setLineups(new ArrayList<>(List.of(existing1, existing2)));
        when(jsonRepository.readData()).thenReturn(teamData);

        // Incoming: 2 lineups with sortOrder 3, 10 (non-sequential)
        Lineup imp1 = buildLineupWithFixedPlayers("imp1a", "imp1b");
        imp1.setId("src-1");
        imp1.setLabel("first-imp");
        imp1.setSortOrder(3);
        Lineup imp2 = buildLineupWithFixedPlayers("imp2a", "imp2b");
        imp2.setId("src-2");
        imp2.setLabel("second-imp");
        imp2.setSortOrder(10);

        Map<String, Integer> result = lineupService.importLineups("team-1", List.of(imp2, imp1));

        assertEquals(2, result.get("imported"));
        assertEquals(4, team.getLineups().size());
        // Imported appended after existing (sortOrder 2, 3), relative order preserved (imp1 before imp2)
        Lineup stored1 = team.getLineups().get(2);
        Lineup stored2 = team.getLineups().get(3);
        assertEquals("first-imp", stored1.getLabel());
        assertEquals("second-imp", stored2.getLabel());
        assertEquals(2, stored1.getSortOrder());
        assertEquals(3, stored2.getSortOrder());
    }
}
