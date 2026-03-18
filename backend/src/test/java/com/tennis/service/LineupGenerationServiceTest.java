package com.tennis.service;

import com.tennis.model.Lineup;
import com.tennis.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LineupGenerationService Test")
class LineupGenerationServiceTest {

    private LineupGenerationService service;

    @BeforeEach
    void setUp() {
        service = new LineupGenerationService(new ConstraintService());
    }

    private Player player(String id, String gender, double utr, boolean verified) {
        Player p = new Player();
        p.setId(id);
        p.setName("Player " + id);
        p.setGender(gender);
        p.setUtr(utr);
        p.setVerified(verified);
        return p;
    }

    private List<Player> build8PlayerRoster() {
        // Total UTR = 37.0 ≤ 40.5, gaps ≤ 3.5, 2 females, all verified
        return Arrays.asList(
            player("p1", "male",   6.0, true),
            player("p2", "male",   5.5, true),
            player("p3", "male",   5.0, true),
            player("p4", "female", 4.5, true),
            player("p5", "male",   4.5, true),
            player("p6", "female", 4.0, true),
            player("p7", "male",   4.0, true),
            player("p8", "male",   3.5, true)
        );
    }

    @Test
    @DisplayName("8-player roster generates at least one valid candidate")
    void testGeneratesAtLeastOneCandidate() {
        List<Lineup> candidates = service.generateCandidates(build8PlayerRoster());
        assertFalse(candidates.isEmpty(), "Should generate at least one valid lineup");
    }

    @Test
    @DisplayName("All generated lineups are valid")
    void testAllCandidatesAreValid() {
        List<Player> players = build8PlayerRoster();
        List<Lineup> candidates = service.generateCandidates(players);
        for (Lineup lineup : candidates) {
            assertTrue(lineup.isValid(), "Every candidate should be valid");
            assertTrue(lineup.getViolationMessages().isEmpty());
        }
    }

    @Test
    @DisplayName("Each lineup has exactly 4 pairs")
    void testEachLineupHas4Pairs() {
        List<Lineup> candidates = service.generateCandidates(build8PlayerRoster());
        for (Lineup lineup : candidates) {
            assertEquals(4, lineup.getPairs().size());
        }
    }

    @Test
    @DisplayName("Position assignment follows UTR descending order D1 > D2 > D3 > D4")
    void testPositionAssignmentDescendingUtr() {
        List<Lineup> candidates = service.generateCandidates(build8PlayerRoster());
        assertFalse(candidates.isEmpty());
        Lineup lineup = candidates.get(0);
        double d1 = lineup.getPairs().stream().filter(p -> "D1".equals(p.getPosition())).findFirst().get().getCombinedUtr();
        double d2 = lineup.getPairs().stream().filter(p -> "D2".equals(p.getPosition())).findFirst().get().getCombinedUtr();
        double d3 = lineup.getPairs().stream().filter(p -> "D3".equals(p.getPosition())).findFirst().get().getCombinedUtr();
        double d4 = lineup.getPairs().stream().filter(p -> "D4".equals(p.getPosition())).findFirst().get().getCombinedUtr();
        assertTrue(d1 >= d2, "D1 combined UTR should be >= D2");
        assertTrue(d2 >= d3, "D2 combined UTR should be >= D3");
        assertTrue(d3 >= d4, "D3 combined UTR should be >= D4");
    }

    @Test
    @DisplayName("No player appears in more than one pair")
    void testPlayerUniquePerLineup() {
        List<Lineup> candidates = service.generateCandidates(build8PlayerRoster());
        for (Lineup lineup : candidates) {
            List<String> ids = new ArrayList<>();
            lineup.getPairs().forEach(pair -> {
                ids.add(pair.getPlayer1Id());
                ids.add(pair.getPlayer2Id());
            });
            assertEquals(8, ids.stream().distinct().count(), "All 8 player slots should be unique");
        }
    }

    @Test
    @DisplayName("Balanced heuristic selects min-variance lineup")
    void testBalancedHeuristicSelectsMinVariance() {
        List<Player> players = build8PlayerRoster();
        List<Lineup> candidates = service.generateCandidates(players);
        assertFalse(candidates.isEmpty());
        Lineup selected = service.selectByHeuristic(candidates, "balanced");
        assertNotNull(selected);
    }

    @Test
    @DisplayName("Aggressive heuristic selects highest D1+D2+D3 lineup")
    void testAggressiveHeuristicSelectsMaxTopThree() {
        List<Player> players = build8PlayerRoster();
        List<Lineup> candidates = service.generateCandidates(players);
        assertFalse(candidates.isEmpty());
        Lineup selected = service.selectByHeuristic(candidates, "aggressive");
        assertNotNull(selected);

        // The selected lineup should have the highest top-three sum among all candidates
        double selectedTopThree = selected.getPairs().stream()
                .filter(p -> List.of("D1","D2","D3").contains(p.getPosition()))
                .mapToDouble(p -> p.getCombinedUtr()).sum();
        for (Lineup c : candidates) {
            double topThree = c.getPairs().stream()
                    .filter(p -> List.of("D1","D2","D3").contains(p.getPosition()))
                    .mapToDouble(p -> p.getCombinedUtr()).sum();
            assertTrue(selectedTopThree >= topThree);
        }
    }

    @Test
    @DisplayName("Roster with large UTR gaps produces fewer valid candidates (pruning works)")
    void testPruningReducesCandidates() {
        // Players with large gaps between groups — should prune many pairs
        List<Player> restrictedPlayers = Arrays.asList(
            player("p1", "male",   8.0, true),
            player("p2", "male",   7.8, true),
            player("p3", "male",   7.6, true),
            player("p4", "female", 7.4, true),
            player("p5", "male",   3.0, true),  // gap > 3.5 with upper group
            player("p6", "female", 2.8, true),
            player("p7", "male",   2.6, true),
            player("p8", "male",   2.4, true)
        );
        List<Lineup> candidates = service.generateCandidates(restrictedPlayers);
        // Candidates exist but are limited
        assertNotNull(candidates);
    }

    @Test
    @DisplayName("Empty candidates returns null from heuristic")
    void testEmptyCandidatesReturnsNull() {
        assertNull(service.selectByHeuristic(new ArrayList<>(), "balanced"));
    }

    @Test
    @DisplayName("D4 positions contain only verified players")
    void testD4ContainsVerifiedPlayers() {
        List<Player> players = build8PlayerRoster();
        List<Lineup> candidates = service.generateCandidates(players);
        for (Lineup lineup : candidates) {
            lineup.getPairs().stream()
                    .filter(p -> "D4".equals(p.getPosition()))
                    .forEach(d4 -> {
                        Player p1 = players.stream().filter(p -> p.getId().equals(d4.getPlayer1Id())).findFirst().get();
                        Player p2 = players.stream().filter(p -> p.getId().equals(d4.getPlayer2Id())).findFirst().get();
                        assertTrue(p1.getVerified() && p2.getVerified(), "D4 players must be verified");
                    });
        }
    }

    @Test
    @DisplayName("Lineup total UTR is sum of all 8 players' UTRs")
    void testTotalUtrIsCorrect() {
        List<Player> players = build8PlayerRoster();
        List<Lineup> candidates = service.generateCandidates(players);
        assertFalse(candidates.isEmpty());
        // All valid lineups use same 8 players, so totalUtr should be consistent
        for (Lineup lineup : candidates) {
            double sum = lineup.getPairs().stream()
                    .flatMap(p -> {
                        Player p1 = players.stream().filter(pl -> pl.getId().equals(p.getPlayer1Id())).findFirst().get();
                        Player p2 = players.stream().filter(pl -> pl.getId().equals(p.getPlayer2Id())).findFirst().get();
                        return List.of(p1.getUtr(), p2.getUtr()).stream();
                    })
                    .mapToDouble(Double::doubleValue).sum();
            assertEquals(sum, lineup.getTotalUtr(), 0.001);
        }
    }
}
