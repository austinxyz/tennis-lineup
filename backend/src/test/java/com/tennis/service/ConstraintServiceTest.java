package com.tennis.service;

import com.tennis.model.Lineup;
import com.tennis.model.Pair;
import com.tennis.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ConstraintService Test")
class ConstraintServiceTest {

    private ConstraintService constraintService;

    @BeforeEach
    void setUp() {
        constraintService = new ConstraintService();
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

    private Pair pair(String pos, Player p1, Player p2) {
        Pair pair = new Pair();
        pair.setPosition(pos);
        pair.setPlayer1Id(p1.getId());
        pair.setPlayer1Name(p1.getName());
        pair.setPlayer2Id(p2.getId());
        pair.setPlayer2Name(p2.getName());
        pair.setCombinedUtr(p1.getUtr() + p2.getUtr());
        return pair;
    }

    private List<Player> buildValidPlayers() {
        // 8 players: 2 female, 6 male, all verified, total UTR = 37.0 ≤ 40.5
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

    private Lineup buildValidLineup(List<Player> players) {
        // D1: p1+p2=15.5, D2: p3+p4=13.5, D3: p5+p6=11.5, D4: p7+p8=9.5
        Lineup lineup = new Lineup();
        lineup.setPairs(Arrays.asList(
            pair("D1", players.get(0), players.get(1)),
            pair("D2", players.get(2), players.get(3)),
            pair("D3", players.get(4), players.get(5)),
            pair("D4", players.get(6), players.get(7))
        ));
        return lineup;
    }

    @Test
    @DisplayName("Valid lineup passes all constraints")
    void testValidLineup() {
        List<Player> players = buildValidPlayers();
        Lineup lineup = buildValidLineup(players);
        ConstraintService.ValidationResult result = constraintService.validateLineup(lineup, players);
        assertTrue(result.isValid());
        assertTrue(result.getViolations().isEmpty());
    }

    @Test
    @DisplayName("UTR ordering constraint: D2 higher than D1 is rejected")
    void testUtrOrderingViolation() {
        List<Player> players = buildValidPlayers();
        // Swap D1 and D2 combined UTR by swapping pairs
        Lineup lineup = new Lineup();
        lineup.setPairs(Arrays.asList(
            pair("D1", players.get(2), players.get(3)), // 13.5
            pair("D2", players.get(0), players.get(1)), // 15.5 — violation
            pair("D3", players.get(4), players.get(5)),
            pair("D4", players.get(6), players.get(7))
        ));
        ConstraintService.ValidationResult result = constraintService.validateLineup(lineup, players);
        assertFalse(result.isValid());
        assertTrue(result.getViolations().stream().anyMatch(v -> v.contains("UTR排序违反")));
    }

    @Test
    @DisplayName("Total UTR cap: sum > 40.5 is rejected")
    void testTotalUtrCapViolation() {
        // Total = 6.5+6.0+5.5+5.0+5.0+5.0+4.5+4.5 = 42.0 > 40.5
        List<Player> players = Arrays.asList(
            player("p1", "male",   6.5, true),
            player("p2", "male",   6.0, true),
            player("p3", "male",   5.5, true),
            player("p4", "female", 5.0, true),
            player("p5", "male",   5.0, true),
            player("p6", "female", 5.0, true),
            player("p7", "male",   4.5, true),
            player("p8", "male",   4.5, true)
        );
        Lineup lineup = buildValidLineup(players);
        ConstraintService.ValidationResult result = constraintService.validateLineup(lineup, players);
        assertFalse(result.isValid());
        assertTrue(result.getViolations().stream().anyMatch(v -> v.contains("总UTR超过40.5")));
    }

    @Test
    @DisplayName("Minimum female constraint: fewer than 2 females rejected")
    void testMinFemaleViolation() {
        // Total UTR = 37.0 ≤ 40.5, but only 1 female
        List<Player> players = Arrays.asList(
            player("p1", "male",   6.0, true),
            player("p2", "male",   5.5, true),
            player("p3", "male",   5.0, true),
            player("p4", "male",   4.5, true),  // was female, now male
            player("p5", "male",   4.5, true),
            player("p6", "female", 4.0, true),  // only 1 female
            player("p7", "male",   4.0, true),
            player("p8", "male",   3.5, true)
        );
        Lineup lineup = buildValidLineup(players);
        ConstraintService.ValidationResult result = constraintService.validateLineup(lineup, players);
        assertFalse(result.isValid());
        assertTrue(result.getViolations().stream().anyMatch(v -> v.contains("女性球员少于2人")));
    }

    @Test
    @DisplayName("Partner UTR gap constraint: gap > 3.5 is rejected")
    void testPartnerUtrGapViolation() {
        // p1(6.0) + p2(2.0): gap = 4.0 > 3.5; total = 6+2+5+4.5+4.5+4+4+3.5=33.5 ≤ 40.5
        List<Player> players = Arrays.asList(
            player("p1", "male",   6.0, true),
            player("p2", "male",   2.0, true),  // gap with p1 = 4.0 > 3.5
            player("p3", "male",   5.0, true),
            player("p4", "female", 4.5, true),
            player("p5", "male",   4.5, true),
            player("p6", "female", 4.0, true),
            player("p7", "male",   4.0, true),
            player("p8", "male",   3.5, true)
        );
        Lineup lineup = buildValidLineup(players);
        ConstraintService.ValidationResult result = constraintService.validateLineup(lineup, players);
        assertFalse(result.isValid());
        assertTrue(result.getViolations().stream().anyMatch(v -> v.contains("搭档UTR差超过3.5")));
    }

    @Test
    @DisplayName("D4 verified constraint: unverified player in D4 rejected")
    void testD4VerifiedViolation() {
        List<Player> players = buildValidPlayers();
        players = new ArrayList<>(players);
        players.set(6, player("p7", "male", 5.0, false)); // p7 unverified in D4
        Lineup lineup = buildValidLineup(players);
        ConstraintService.ValidationResult result = constraintService.validateLineup(lineup, players);
        assertFalse(result.isValid());
        assertTrue(result.getViolations().stream().anyMatch(v -> v.contains("D4球员必须有Verified")));
    }

    @Test
    @DisplayName("Player uniqueness: duplicate player rejected")
    void testPlayerUniquenessViolation() {
        List<Player> players = buildValidPlayers();
        Lineup lineup = new Lineup();
        // p1 appears in both D1 and D2
        lineup.setPairs(Arrays.asList(
            pair("D1", players.get(0), players.get(1)),
            pair("D2", players.get(0), players.get(3)), // p1 repeated
            pair("D3", players.get(4), players.get(5)),
            pair("D4", players.get(6), players.get(7))
        ));
        ConstraintService.ValidationResult result = constraintService.validateLineup(lineup, players);
        assertFalse(result.isValid());
        assertTrue(result.getViolations().stream().anyMatch(v -> v.contains("球员重复")));
    }

    @Test
    @DisplayName("Exactly 2 females passes minimum female constraint")
    void testExactlyTwoFemalesPasses() {
        List<Player> players = buildValidPlayers(); // has exactly 2 females
        Lineup lineup = buildValidLineup(players);
        ConstraintService.ValidationResult result = constraintService.validateLineup(lineup, players);
        assertTrue(result.getViolations().stream().noneMatch(v -> v.contains("女性球员少于2人")));
    }

    @Test
    @DisplayName("Total UTR exactly 40.5 passes cap constraint")
    void testTotalUtrExactlyAtCapPasses() {
        // Total = 6.5+5.5+5.0+5.0+5.0+5.0+4.5+4.0 = 40.5
        List<Player> players = Arrays.asList(
            player("p1", "male",   6.5, true),
            player("p2", "male",   5.5, true),
            player("p3", "male",   5.0, true),
            player("p4", "female", 5.0, true),
            player("p5", "male",   5.0, true),
            player("p6", "female", 5.0, true),
            player("p7", "male",   4.5, true),
            player("p8", "male",   4.0, true)  // total = 40.5
        );
        Lineup lineup = buildValidLineup(players);
        ConstraintService.ValidationResult result = constraintService.validateLineup(lineup, players);
        assertTrue(result.getViolations().stream().noneMatch(v -> v.contains("总UTR超过40.5")));
    }

    @Test
    @DisplayName("Partner UTR gap exactly 3.5 passes constraint")
    void testPartnerUtrGapExactly35Passes() {
        // p1(5.5) + p2(2.0): gap = 3.5 exactly OK; total = 5.5+2+4.5+4+4+3.5+3.5+3=30 ≤ 40.5
        List<Player> players = Arrays.asList(
            player("p1", "male",   5.5, true),
            player("p2", "male",   2.0, true), // gap = 3.5, OK
            player("p3", "male",   4.5, true),
            player("p4", "female", 4.0, true),
            player("p5", "male",   4.0, true),
            player("p6", "female", 3.5, true),
            player("p7", "male",   3.5, true),
            player("p8", "male",   3.0, true)
        );
        Lineup lineup = buildValidLineup(players);
        ConstraintService.ValidationResult result = constraintService.validateLineup(lineup, players);
        assertTrue(result.getViolations().stream().noneMatch(v -> v.contains("搭档UTR差超过3.5")));
    }

    @Test
    @DisplayName("Both D4 players verified passes D4 constraint")
    void testBothD4VerifiedPasses() {
        List<Player> players = buildValidPlayers();
        Lineup lineup = buildValidLineup(players);
        ConstraintService.ValidationResult result = constraintService.validateLineup(lineup, players);
        assertTrue(result.getViolations().stream().noneMatch(v -> v.contains("D4球员必须有Verified")));
    }

    @Test
    @DisplayName("Multiple violations are all reported")
    void testMultipleViolationsReported() {
        // Violations: gap > 3.5 (p1 vs p2), no females, D4 unverified; total = 6+2+5+4.5+4+4+4+3.5=33 ≤ 40.5
        List<Player> players = Arrays.asList(
            player("p1", "male",   6.0, true),
            player("p2", "male",   2.0, true),  // gap 4.0 > 3.5
            player("p3", "male",   5.0, true),
            player("p4", "male",   4.5, true),  // no females at all
            player("p5", "male",   4.0, true),
            player("p6", "male",   4.0, true),
            player("p7", "male",   4.0, false), // D4 unverified
            player("p8", "male",   3.5, true)
        );
        Lineup lineup = buildValidLineup(players);
        ConstraintService.ValidationResult result = constraintService.validateLineup(lineup, players);
        assertFalse(result.isValid());
        assertTrue(result.getViolations().size() >= 2);
    }
}
