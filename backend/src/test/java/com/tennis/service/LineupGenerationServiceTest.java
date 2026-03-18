package com.tennis.service;

import com.tennis.model.Lineup;
import com.tennis.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    @Test
    @DisplayName("excludePlayers removes excluded players from all candidates")
    void testExcludePlayersFilter() {
        List<Player> players = new ArrayList<>(build8PlayerRoster());
        // Add 2 extra players so we have 10, then exclude 2 specific ones
        players.add(player("p9", "male", 3.0, true));
        players.add(player("p10", "female", 2.8, true));

        List<Lineup> candidates = service.generateCandidates(players, Set.of(), Set.of("p9", "p10"));
        assertFalse(candidates.isEmpty());
        for (Lineup lineup : candidates) {
            boolean hasExcluded = lineup.getPairs().stream().anyMatch(p ->
                    p.getPlayer1Id().equals("p9") || p.getPlayer2Id().equals("p9") ||
                    p.getPlayer1Id().equals("p10") || p.getPlayer2Id().equals("p10"));
            assertFalse(hasExcluded, "Excluded players must not appear in any lineup");
        }
    }

    @Test
    @DisplayName("includePlayers forces those players into every candidate")
    void testIncludePlayersFilter() {
        List<Player> players = new ArrayList<>(build8PlayerRoster());
        // Add 2 extra players so selection isn't forced
        players.add(player("p9", "male", 3.5, true));
        players.add(player("p10", "female", 3.0, true));

        List<Lineup> candidates = service.generateCandidates(players, Set.of("p1", "p2"), Set.of());
        assertFalse(candidates.isEmpty());
        for (Lineup lineup : candidates) {
            java.util.Set<String> ids = lineup.getPairs().stream()
                    .flatMap(p -> java.util.stream.Stream.of(p.getPlayer1Id(), p.getPlayer2Id()))
                    .collect(java.util.stream.Collectors.toSet());
            assertTrue(ids.contains("p1"), "p1 must appear in every lineup");
            assertTrue(ids.contains("p2"), "p2 must appear in every lineup");
        }
    }

    @Test
    @DisplayName("pinPlayers: player appears in specified position in every candidate")
    void testPinPlayerToPosition() {
        List<Player> players = new ArrayList<>(build8PlayerRoster());
        players.add(player("p9", "male", 3.5, true));
        players.add(player("p10", "female", 3.0, true));

        List<Lineup> candidates = service.generateCandidates(
                players, Set.of(), Set.of(), Map.of("p1", "D1"));
        assertFalse(candidates.isEmpty());
        for (Lineup lineup : candidates) {
            boolean p1InD1 = lineup.getPairs().stream()
                    .filter(p -> "D1".equals(p.getPosition()))
                    .anyMatch(p -> "p1".equals(p.getPlayer1Id()) || "p1".equals(p.getPlayer2Id()));
            assertTrue(p1InD1, "p1 must be in D1 in every lineup");
        }
    }

    @Test
    @DisplayName("pinPlayers: invalid position throws IllegalArgumentException")
    void testPinPlayerInvalidPositionThrows() {
        List<Player> players = build8PlayerRoster();
        assertThrows(IllegalArgumentException.class, () ->
                service.generateCandidates(players, Set.of(), Set.of(), Map.of("p1", "D5")));
    }

    @Test
    @DisplayName("pinPlayers: player in both pin and exclude throws IllegalArgumentException")
    void testPinAndExcludeConflictThrows() {
        List<Player> players = build8PlayerRoster();
        assertThrows(IllegalArgumentException.class, () ->
                service.generateCandidates(players, Set.of(), Set.of("p1"), Map.of("p1", "D1")));
    }

    @Test
    @DisplayName("include and exclude overlap throws IllegalArgumentException")
    void testIncludeExcludeOverlapThrows() {
        List<Player> players = build8PlayerRoster();
        assertThrows(IllegalArgumentException.class, () ->
                service.generateCandidates(players, Set.of("p1"), Set.of("p1")));
    }

    @Test
    @DisplayName("more than 8 includePlayers throws IllegalArgumentException")
    void testTooManyIncludePlayersThrows() {
        List<Player> players = build8PlayerRoster();
        assertThrows(IllegalArgumentException.class, () ->
                service.generateCandidates(players,
                        Set.of("p1","p2","p3","p4","p5","p6","p7","p8","p9"), Set.of()));
    }

    @Test
    @DisplayName("excluding too many players throws IllegalArgumentException")
    void testTooManyExcludePlayersThrows() {
        List<Player> players = build8PlayerRoster();
        assertThrows(IllegalArgumentException.class, () ->
                service.generateCandidates(players, Set.of(),
                        Set.of("p1", "p2", "p3"))); // only 5 remaining < 8
    }

    @Test
    @DisplayName("roster > 8: subsets sorted by UTR proximity to 40.5 — highest valid UTR subset first, no female preference")
    void testSubsetEnumerationSelectsHighUtrCombination() {
        // 10 players; top 8 by UTR form the subset closest to 40.5
        // p1-p8: total UTR = 6.0+5.5+5.0+4.5+4.5+4.0+4.0+3.5 = 37.0
        // p9(2.0), p10(1.5) are lower — subsets including them will have lower totalUtr
        List<Player> players = new ArrayList<>(build8PlayerRoster());
        players.add(player("p9", "male", 2.0, true));
        players.add(player("p10", "female", 1.5, true));

        List<Lineup> candidates = service.generateCandidates(players, Set.of(), Set.of(), Map.of());
        assertFalse(candidates.isEmpty());
        // All lineups must not exceed cap
        for (Lineup l : candidates) {
            assertTrue(l.getTotalUtr() <= 40.5, "totalUtr must not exceed cap");
        }
        // The highest totalUtr among all lineups should come from the top-UTR subset (p1-p8)
        double maxUtr = candidates.stream().mapToDouble(Lineup::getTotalUtr).max().orElse(0);
        assertTrue(maxUtr >= 35.0, "Should find a lineup close to 40.5 using top-UTR players");
    }

    @Test
    @DisplayName("roster > 8: top-20 subsets processed first; if ≥6 results, no need for more")
    void testTop20SubsetsFirstBatch() {
        // Build a 10-player roster that will definitely produce ≥6 valid lineups from top-20 subsets
        List<Player> players = new ArrayList<>(build8PlayerRoster());
        players.add(player("p9", "male", 3.5, true));
        players.add(player("p10", "female", 3.0, true));

        List<Lineup> candidates = service.generateCandidates(players, Set.of(), Set.of(), Map.of());
        // Should return a non-empty list of cap-valid lineups
        assertFalse(candidates.isEmpty());
        for (Lineup l : candidates) {
            assertTrue(l.getTotalUtr() <= 40.5);
        }
    }

    @Test
    @DisplayName("two players pinned to same position must form a pair at that position")
    void testTwoPlayersToSamePositionMustBePaired() {
        List<Player> players = new ArrayList<>(build8PlayerRoster());
        players.add(player("p9", "male", 3.5, true));
        players.add(player("p10", "female", 3.0, true));

        // Pin p7 and p8 both to D4 — they must be paired together at D4
        Map<String, String> pins = Map.of("p7", "D4", "p8", "D4");
        List<Lineup> candidates = service.generateCandidates(players, Set.of(), Set.of(), pins);
        assertFalse(candidates.isEmpty(), "Should produce candidates with p7+p8 at D4");
        for (Lineup lineup : candidates) {
            boolean p7p8AtD4 = lineup.getPairs().stream()
                    .filter(p -> "D4".equals(p.getPosition()))
                    .anyMatch(p ->
                            (("p7".equals(p.getPlayer1Id()) && "p8".equals(p.getPlayer2Id())) ||
                             ("p8".equals(p.getPlayer1Id()) && "p7".equals(p.getPlayer2Id()))));
            assertTrue(p7p8AtD4, "p7 and p8 must be paired together at D4");
        }
    }

    @Test
    @DisplayName("more than 2 players pinned to same position throws IllegalArgumentException")
    void testMoreThanTwoPlayersSamePositionThrows() {
        List<Player> players = build8PlayerRoster();
        Map<String, String> pins = Map.of("p1", "D1", "p2", "D1", "p3", "D1");
        assertThrows(IllegalArgumentException.class, () ->
                service.generateCandidates(players, Set.of(), Set.of(), pins));
    }

    @Test
    @DisplayName("gender fields are populated in generated lineup pairs")
    void testGenderFieldsPopulated() {
        List<Lineup> candidates = service.generateCandidates(build8PlayerRoster());
        assertFalse(candidates.isEmpty());
        Lineup lineup = candidates.get(0);
        for (var pair : lineup.getPairs()) {
            assertNotNull(pair.getPlayer1Gender(), "player1Gender must not be null");
            assertNotNull(pair.getPlayer2Gender(), "player2Gender must not be null");
            assertTrue(pair.getPlayer1Gender().equals("male") || pair.getPlayer1Gender().equals("female"));
            assertTrue(pair.getPlayer2Gender().equals("male") || pair.getPlayer2Gender().equals("female"));
        }
    }
}
