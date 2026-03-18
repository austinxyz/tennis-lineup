package com.tennis.service;

import com.tennis.model.Lineup;
import com.tennis.model.Pair;
import com.tennis.model.Player;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
            pair.setPlayer2Id(p2.getId());
            pair.setPlayer2Name(p2.getName());
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
