package com.tennis.service;

import com.tennis.model.Lineup;
import com.tennis.model.Pair;
import com.tennis.model.Player;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LineupGenerationService {

    private static final double UTR_CAP = 40.5;
    private static final Set<String> VALID_POSITIONS = new HashSet<>(Arrays.asList("D1", "D2", "D3", "D4"));

    private final ConstraintService constraintService;

    @Autowired
    public LineupGenerationService(ConstraintService constraintService) {
        this.constraintService = constraintService;
    }

    /**
     * Generate all valid lineups using backtracking with partner UTR gap pruning.
     */
    public List<Lineup> generateCandidates(List<Player> players) {
        return generateCandidates(players, Set.of(), Set.of(), Map.of());
    }

    public List<Lineup> generateCandidates(List<Player> players, Set<String> include, Set<String> exclude) {
        return generateCandidates(players, include, exclude, Map.of());
    }

    /**
     * Generate valid lineups with include/exclude/pin constraints.
     * Algorithm:
     *   1. Validate constraints.
     *   2. Enumerate all 8-player subsets from eligible roster, ordered by:
     *      a. totalUtr ≤ 40.5
     *      b. Prefer exactly 2 females
     *      c. Highest totalUtr first
     *   3. For each subset, run backtracking to find valid pair arrangements.
     *   4. Post-filter by include and pair-level pin constraints.
     */
    public List<Lineup> generateCandidates(List<Player> players, Set<String> include, Set<String> exclude,
                                            Map<String, String> pinPlayers) {
        // --- Validate constraints ---
        Set<String> overlap = new HashSet<>(include);
        overlap.retainAll(exclude);
        if (!overlap.isEmpty()) {
            throw new IllegalArgumentException("同一球员不能同时被包含和排除");
        }

        for (Map.Entry<String, String> pin : pinPlayers.entrySet()) {
            if (!VALID_POSITIONS.contains(pin.getValue())) {
                throw new IllegalArgumentException("位置必须为 D1/D2/D3/D4");
            }
            if (exclude.contains(pin.getKey())) {
                throw new IllegalArgumentException("同一球员不能同时被固定位置和排除");
            }
        }

        // Validate same-position pin counts
        Map<String, List<String>> playersByPin = new HashMap<>();
        for (Map.Entry<String, String> pin : pinPlayers.entrySet()) {
            playersByPin.computeIfAbsent(pin.getValue(), k -> new ArrayList<>()).add(pin.getKey());
        }
        for (Map.Entry<String, List<String>> entry : playersByPin.entrySet()) {
            if (entry.getValue().size() > 2) {
                throw new IllegalArgumentException("不能将超过2名球员固定到同一位置: " + entry.getKey());
            }
        }

        // Build effective include set (pins implicitly included)
        Set<String> effectiveInclude = new HashSet<>(include);
        effectiveInclude.addAll(pinPlayers.keySet());

        if (effectiveInclude.size() > 8) {
            throw new IllegalArgumentException("必须上场球员超过8人");
        }

        // Build eligible roster
        List<Player> roster = players.stream()
                .filter(p -> !exclude.contains(p.getId()))
                .collect(Collectors.toList());

        if (roster.size() < 8) {
            throw new IllegalArgumentException("排除球员后可用球员不足8人");
        }

        // All candidates across all valid 8-player subsets
        List<Lineup> allCandidates = new ArrayList<>();

        if (roster.size() == 8) {
            // Only one possible subset
            backtrack(roster, new ArrayList<>(), new boolean[roster.size()], allCandidates);
        } else {
            // Enumerate subsets ordered to maximize totalUtr ≤ 40.5, prefer 2 females
            List<List<Player>> subsets = enumerateSubsets(roster, 8, effectiveInclude);
            for (List<Player> subset : subsets) {
                // Stop early once we have enough candidates
                if (allCandidates.size() >= 100) break;
                backtrack(subset, new ArrayList<>(), new boolean[subset.size()], allCandidates);
            }
        }

        // Post-filter: keep only lineups where all effective include players appear
        if (!effectiveInclude.isEmpty()) {
            allCandidates = allCandidates.stream()
                    .filter(lineup -> {
                        Set<String> usedIds = lineup.getPairs().stream()
                                .flatMap(p -> java.util.stream.Stream.of(p.getPlayer1Id(), p.getPlayer2Id()))
                                .collect(Collectors.toSet());
                        return usedIds.containsAll(effectiveInclude);
                    })
                    .collect(Collectors.toList());
        }

        // Post-filter: pair-level pin — players pinned to same position must be paired together
        if (!pinPlayers.isEmpty()) {
            allCandidates = allCandidates.stream()
                    .filter(lineup -> satisfiesPinConstraints(lineup, pinPlayers, playersByPin))
                    .collect(Collectors.toList());
            if (allCandidates.isEmpty()) {
                throw new IllegalArgumentException("无法生成满足位置约束的排阵");
            }
        }

        return allCandidates;
    }

    /**
     * Check that all pin constraints are satisfied:
     * - For positions with 2 pinned players: both must form a pair at that position.
     * - For positions with 1 pinned player: that player must appear at that position.
     */
    private boolean satisfiesPinConstraints(Lineup lineup, Map<String, String> pinPlayers,
                                            Map<String, List<String>> playersByPin) {
        for (Map.Entry<String, List<String>> entry : playersByPin.entrySet()) {
            String position = entry.getKey();
            List<String> pinnedIds = entry.getValue();

            if (pinnedIds.size() == 2) {
                // Both players must form the pair at this position
                String id1 = pinnedIds.get(0);
                String id2 = pinnedIds.get(1);
                boolean found = lineup.getPairs().stream()
                        .filter(pair -> position.equals(pair.getPosition()))
                        .anyMatch(pair ->
                                (id1.equals(pair.getPlayer1Id()) && id2.equals(pair.getPlayer2Id())) ||
                                (id2.equals(pair.getPlayer1Id()) && id1.equals(pair.getPlayer2Id())));
                if (!found) return false;
            } else {
                // Single pin: player must appear at that position
                String playerId = pinnedIds.get(0);
                boolean found = lineup.getPairs().stream()
                        .filter(pair -> position.equals(pair.getPosition()))
                        .anyMatch(pair -> playerId.equals(pair.getPlayer1Id())
                                || playerId.equals(pair.getPlayer2Id()));
                if (!found) return false;
            }
        }
        return true;
    }

    /**
     * Enumerate all C(n, 8) subsets of `roster`, ordered by:
     * 1. Subsets with totalUtr ≤ 40.5 first (cap satisfied)
     * 2. Among valid subsets: those with exactly 2 females first
     * 3. Within same female-count group: highest totalUtr first
     *
     * Required players (mustInclude) are always in every subset.
     */
    private List<List<Player>> enumerateSubsets(List<Player> roster, int size, Set<String> mustInclude) {
        // Separate must-include players from optional players
        List<Player> required = roster.stream()
                .filter(p -> mustInclude.contains(p.getId()))
                .collect(Collectors.toList());
        List<Player> optional = roster.stream()
                .filter(p -> !mustInclude.contains(p.getId()))
                .collect(Collectors.toList());

        if (required.size() > size) {
            return List.of(); // impossible
        }

        int remaining = size - required.size();

        // Generate all combinations of `remaining` optional players
        List<List<Player>> subsets = new ArrayList<>();
        generateCombinations(optional, remaining, 0, new ArrayList<>(), required, subsets);

        // Sort: cap-valid first, then prefer 2 females, then highest totalUtr
        subsets.sort((a, b) -> {
            double utrA = totalUtr(a);
            double utrB = totalUtr(b);
            boolean validA = utrA <= UTR_CAP;
            boolean validB = utrB <= UTR_CAP;

            // Cap-valid subsets come first
            if (validA != validB) return validA ? -1 : 1;

            if (validA) {
                // Both valid: prefer exactly 2 females, then higher UTR
                int femA = femaleCount(a);
                int femB = femaleCount(b);
                boolean prefA = femA == 2;
                boolean prefB = femB == 2;
                if (prefA != prefB) return prefA ? -1 : 1;
                // Same female preference group: higher UTR first
                return Double.compare(utrB, utrA);
            }
            // Both invalid (over cap): sort by UTR desc (closest to cap)
            return Double.compare(utrB, utrA);
        });

        return subsets;
    }

    private void generateCombinations(List<Player> optional, int remaining, int start,
                                       List<Player> current, List<Player> required,
                                       List<List<Player>> result) {
        if (current.size() == remaining) {
            List<Player> subset = new ArrayList<>(required);
            subset.addAll(current);
            result.add(subset);
            return;
        }
        for (int i = start; i < optional.size(); i++) {
            current.add(optional.get(i));
            generateCombinations(optional, remaining, i + 1, current, required, result);
            current.remove(current.size() - 1);
        }
    }

    private double totalUtr(List<Player> subset) {
        return subset.stream().mapToDouble(Player::getUtr).sum();
    }

    private int femaleCount(List<Player> subset) {
        return (int) subset.stream().filter(p -> "female".equals(p.getGender())).count();
    }

    private void backtrack(List<Player> roster, List<int[]> currentPairs,
                           boolean[] used, List<Lineup> result) {
        if (currentPairs.size() == 4) {
            Lineup lineup = buildLineup(roster, currentPairs);
            ConstraintService.ValidationResult validation =
                    constraintService.validateLineup(lineup, roster);
            if (validation.isValid()) {
                result.add(lineup);
            }
            return;
        }

        // Find the first available player to anchor the next pair
        int first = -1;
        for (int i = 0; i < roster.size(); i++) {
            if (!used[i]) { first = i; break; }
        }
        if (first == -1) return;

        used[first] = true;
        for (int second = first + 1; second < roster.size(); second++) {
            if (used[second]) continue;

            Player p1 = roster.get(first);
            Player p2 = roster.get(second);

            // Prune: partner UTR gap > 3.5
            if (Math.abs(p1.getUtr() - p2.getUtr()) > 3.5) continue;

            used[second] = true;
            currentPairs.add(new int[]{first, second});
            backtrack(roster, currentPairs, used, result);
            currentPairs.remove(currentPairs.size() - 1);
            used[second] = false;
        }
        used[first] = false;
    }

    /**
     * Builds a Lineup from player index pairs, assigning positions D1-D4
     * in descending combined UTR order.
     */
    private Lineup buildLineup(List<Player> roster, List<int[]> pairIndices) {
        List<int[]> sorted = new ArrayList<>(pairIndices);
        sorted.sort((a, b) -> Double.compare(
                roster.get(b[0]).getUtr() + roster.get(b[1]).getUtr(),
                roster.get(a[0]).getUtr() + roster.get(a[1]).getUtr()
        ));

        String[] positions = {"D1", "D2", "D3", "D4"};
        List<Pair> pairs = new ArrayList<>();
        double total = 0;

        for (int i = 0; i < sorted.size(); i++) {
            Player p1 = roster.get(sorted.get(i)[0]);
            Player p2 = roster.get(sorted.get(i)[1]);
            double combined = p1.getUtr() + p2.getUtr();
            total += combined;

            Pair pair = new Pair();
            pair.setPosition(positions[i]);
            pair.setPlayer1Id(p1.getId());
            pair.setPlayer1Name(p1.getName());
            pair.setPlayer1Utr(p1.getUtr());
            pair.setPlayer1Gender(p1.getGender());
            pair.setPlayer2Id(p2.getId());
            pair.setPlayer2Name(p2.getName());
            pair.setPlayer2Utr(p2.getUtr());
            pair.setPlayer2Gender(p2.getGender());
            pair.setCombinedUtr(combined);
            pairs.add(pair);
        }

        Lineup lineup = new Lineup();
        lineup.setPairs(pairs);
        lineup.setTotalUtr(total);
        lineup.setValid(true);
        lineup.setViolationMessages(new ArrayList<>());
        return lineup;
    }

    /**
     * Fallback heuristic: "balanced" selects min variance, "aggressive" maximizes D1+D2+D3.
     */
    public Lineup selectByHeuristic(List<Lineup> candidates, String strategy) {
        if (candidates.isEmpty()) return null;

        if ("aggressive".equals(strategy)) {
            return candidates.stream()
                    .max(Comparator.comparingDouble(this::topThreeUtr))
                    .orElse(candidates.get(0));
        }

        return candidates.stream()
                .min(Comparator.comparingDouble(this::combinedUtrVariance))
                .orElse(candidates.get(0));
    }

    private double topThreeUtr(Lineup lineup) {
        return lineup.getPairs().stream()
                .filter(p -> "D1".equals(p.getPosition()) || "D2".equals(p.getPosition()) || "D3".equals(p.getPosition()))
                .mapToDouble(Pair::getCombinedUtr)
                .sum();
    }

    private double combinedUtrVariance(Lineup lineup) {
        List<Double> utrs = lineup.getPairs().stream()
                .map(Pair::getCombinedUtr)
                .toList();
        double mean = utrs.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        return utrs.stream().mapToDouble(u -> (u - mean) * (u - mean)).average().orElse(0);
    }
}
