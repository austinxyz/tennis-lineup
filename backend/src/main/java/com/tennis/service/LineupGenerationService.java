package com.tennis.service;

import com.tennis.model.Lineup;
import com.tennis.model.Pair;
import com.tennis.model.Player;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LineupGenerationService {

    private final ConstraintService constraintService;

    @Autowired
    public LineupGenerationService(ConstraintService constraintService) {
        this.constraintService = constraintService;
    }

    /**
     * Generate all valid lineups using backtracking with partner UTR gap pruning.
     * Returns all candidate lineups sorted by D1 combined UTR descending.
     */
    public List<Lineup> generateCandidates(List<Player> players) {
        List<Lineup> candidates = new ArrayList<>();
        List<Player> roster = new ArrayList<>(players);
        backtrack(roster, new ArrayList<>(), new boolean[roster.size()], candidates);
        return candidates;
    }

    private static final Set<String> VALID_POSITIONS = new HashSet<>(Arrays.asList("D1", "D2", "D3", "D4"));

    /**
     * Generate valid lineups with include/exclude player constraints.
     * @param players  full team roster
     * @param include  player IDs that MUST appear in every lineup (may be empty)
     * @param exclude  player IDs that MUST NOT appear in any lineup (may be empty)
     */
    public List<Lineup> generateCandidates(List<Player> players, Set<String> include, Set<String> exclude) {
        return generateCandidates(players, include, exclude, Map.of());
    }

    /**
     * Generate valid lineups with include/exclude/pin player constraints.
     * @param players    full team roster
     * @param include    player IDs that MUST appear in every lineup (may be empty)
     * @param exclude    player IDs that MUST NOT appear in any lineup (may be empty)
     * @param pinPlayers map of playerId → position ("D1"-"D4") that player MUST play
     */
    public List<Lineup> generateCandidates(List<Player> players, Set<String> include, Set<String> exclude,
                                            Map<String, String> pinPlayers) {
        // Validate constraints
        Set<String> overlap = new HashSet<>(include);
        overlap.retainAll(exclude);
        if (!overlap.isEmpty()) {
            throw new IllegalArgumentException("同一球员不能同时被包含和排除");
        }
        if (include.size() > 8) {
            throw new IllegalArgumentException("必须上场球员超过8人");
        }

        // Validate pin positions
        for (Map.Entry<String, String> pin : pinPlayers.entrySet()) {
            if (!VALID_POSITIONS.contains(pin.getValue())) {
                throw new IllegalArgumentException("位置必须为 D1/D2/D3/D4");
            }
            if (exclude.contains(pin.getKey())) {
                throw new IllegalArgumentException("同一球员不能同时被固定位置和排除");
            }
        }

        // Build eligible roster (exclude removed players)
        List<Player> roster = players.stream()
                .filter(p -> !exclude.contains(p.getId()))
                .collect(Collectors.toList());

        if (roster.size() < 8) {
            throw new IllegalArgumentException("排除球员后可用球员不足8人");
        }

        List<Lineup> candidates = new ArrayList<>();
        backtrack(roster, new ArrayList<>(), new boolean[roster.size()], candidates);

        // Post-filter: keep only lineups where all include players appear
        if (!include.isEmpty()) {
            candidates = candidates.stream()
                    .filter(lineup -> {
                        Set<String> usedIds = lineup.getPairs().stream()
                                .flatMap(p -> java.util.stream.Stream.of(p.getPlayer1Id(), p.getPlayer2Id()))
                                .collect(Collectors.toSet());
                        return usedIds.containsAll(include);
                    })
                    .collect(Collectors.toList());
        }

        // Post-filter: keep only lineups where pinned players appear in the specified position
        if (!pinPlayers.isEmpty()) {
            candidates = candidates.stream()
                    .filter(lineup -> pinPlayers.entrySet().stream().allMatch(pin -> {
                        String playerId = pin.getKey();
                        String position = pin.getValue();
                        return lineup.getPairs().stream()
                                .filter(pair -> position.equals(pair.getPosition()))
                                .anyMatch(pair -> playerId.equals(pair.getPlayer1Id())
                                        || playerId.equals(pair.getPlayer2Id()));
                    }))
                    .collect(Collectors.toList());
            if (candidates.isEmpty()) {
                throw new IllegalArgumentException("无法生成满足位置约束的排阵");
            }
        }

        return candidates;
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
        double totalUtr = 0;

        for (int i = 0; i < sorted.size(); i++) {
            Player p1 = roster.get(sorted.get(i)[0]);
            Player p2 = roster.get(sorted.get(i)[1]);
            double combined = p1.getUtr() + p2.getUtr();
            totalUtr += p1.getUtr() + p2.getUtr();

            Pair pair = new Pair();
            pair.setPosition(positions[i]);
            pair.setPlayer1Id(p1.getId());
            pair.setPlayer1Name(p1.getName());
            pair.setPlayer1Utr(p1.getUtr());
            pair.setPlayer2Id(p2.getId());
            pair.setPlayer2Name(p2.getName());
            pair.setPlayer2Utr(p2.getUtr());
            pair.setCombinedUtr(combined);
            pairs.add(pair);
        }

        Lineup lineup = new Lineup();
        lineup.setPairs(pairs);
        lineup.setTotalUtr(totalUtr);
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

        // default: balanced — minimize variance of D1-D4 combined UTRs
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
