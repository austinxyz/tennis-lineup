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

    public List<Lineup> generateCandidates(List<Player> players) {
        return generateCandidates(players, Set.of(), Set.of(), Map.of());
    }

    public List<Lineup> generateCandidates(List<Player> players, Set<String> include, Set<String> exclude) {
        return generateCandidates(players, include, exclude, Map.of());
    }

    /**
     * Generate valid lineups with include/exclude/pin constraints.
     *
     * Algorithm:
     *   1. Validate constraints.
     *   2. Phase 1: Remove excludePlayers from eligible roster.
     *   3. Phase 2: Enumerate 8-player subsets where locked players (include ∪ pin)
     *      are always included. Sort subsets by totalUtr proximity to 40.5
     *      (cap-valid first, then highest totalUtr).
     *   4. Backtrack on top-20 subsets using pre-sorted pair candidates:
     *      - No pin constraints: top-20 pairs for D1/D2 rounds, all pairs for D3/D4.
     *      - With pin constraints: all pairs for every round (ensures pinned-pair
     *        combinations aren't missed regardless of their combined UTR rank).
     *   5. Extend to top-40 subsets if fewer than 6 results after pin filter.
     *   6. Final filter by include and pin constraints.
     *
     * Locked players (include/pin) are present in every subset, so they are never
     * excluded from consideration regardless of how many subsets are tried.
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

        // Locked players: include ∪ pin (always present in every subset)
        Set<String> effectiveInclude = new HashSet<>(include);
        effectiveInclude.addAll(pinPlayers.keySet());

        if (effectiveInclude.size() > 8) {
            throw new IllegalArgumentException("固定参赛球员超过8人");
        }

        // Phase 1: Build eligible roster (remove excluded players)
        List<Player> eligible = players.stream()
                .filter(p -> !exclude.contains(p.getId()))
                .collect(Collectors.toList());

        if (eligible.size() < 8) {
            throw new IllegalArgumentException("排除球员后可用球员不足8人");
        }

        // Phase 2: Enumerate 8-player subsets with locked players always included.
        // Subsets are sorted by totalUtr proximity to 40.5 (cap-valid first, then highest).
        // Each subset is a unique 8-player combination — no dedup needed at this level.
        List<List<Player>> subsets = enumerateSubsets(eligible, 8, effectiveInclude);

        // Backtrack on top-20 subsets
        List<Lineup> rawCandidates = new ArrayList<>();
        int firstEnd = Math.min(20, subsets.size());
        for (int i = 0; i < firstEnd; i++) {
            backtrackSubset(subsets.get(i), pinPlayers, rawCandidates);
        }

        // Extend to top-40 if pin constraints and fewer than 6 pinned results
        List<Lineup> filtered = filterCandidates(rawCandidates, effectiveInclude, pinPlayers, playersByPin);
        if (subsets.size() > firstEnd && filtered.size() < 6) {
            int secondEnd = Math.min(40, subsets.size());
            for (int i = firstEnd; i < secondEnd; i++) {
                backtrackSubset(subsets.get(i), pinPlayers, rawCandidates);
            }
        }

        // Expand pool if fewer than 100 candidates across more subsets for actualUtr re-ranking
        List<Lineup> allCandidates = filterCandidates(rawCandidates, effectiveInclude, pinPlayers, playersByPin);
        if (allCandidates.size() < 100 && subsets.size() > 40) {
            for (int i = 40; i < subsets.size() && allCandidates.size() < 100; i++) {
                backtrackSubset(subsets.get(i), pinPlayers, rawCandidates);
                allCandidates = filterCandidates(rawCandidates, effectiveInclude, pinPlayers, playersByPin);
            }
        }

        if (allCandidates.isEmpty() && !pinPlayers.isEmpty()) {
            throw new IllegalArgumentException("无法生成满足位置约束的排阵");
        }

        // Dedup by player set: keep one representative per unique 8-player combination.
        // All pair arrangements of the same 8 players have the same actualUtrSum, so we
        // keep only the first (which the backtracking naturally produces as the strongest pairing).
        Set<String> seen = new HashSet<>();
        List<Lineup> deduped = new ArrayList<>();
        for (Lineup l : allCandidates) {
            if (l.getPairs() == null) { deduped.add(l); continue; }
            List<String> ids = l.getPairs().stream()
                    .flatMap(p -> java.util.stream.Stream.of(p.getPlayer1Id(), p.getPlayer2Id()))
                    .filter(java.util.Objects::nonNull)
                    .sorted()
                    .collect(Collectors.toList());
            String key = String.join(",", ids);
            if (seen.add(key)) deduped.add(l);
        }

        // Re-rank by actualUtrSum descending; return top 6
        deduped.sort(Comparator.comparingDouble(
                (Lineup l) -> l.getActualUtrSum() != null ? l.getActualUtrSum() : l.getTotalUtr()
        ).reversed());
        return deduped.subList(0, Math.min(6, deduped.size()));
    }

    /**
     * Run pair-level backtracking on a single 8-player subset and collect valid lineups.
     *
     * Pair candidates are sorted by combined UTR descending. For the first two pair slots
     * (which become D1/D2 after UTR-based position assignment) only the top-20 pairs are
     * considered — UNLESS pin constraints are present, in which case all pairs are used for
     * every slot to avoid missing pinned-pair combinations that may rank below position 20.
     */
    private void backtrackSubset(List<Player> subset, Map<String, String> pinPlayers, List<Lineup> result) {
        List<int[]> allPairs = generateValidPairs(subset);
        // With pin constraints: use all pairs every round so pinned combinations are never missed.
        // Without pin constraints: top-20 pairs for D1/D2 rounds (performance optimization).
        List<int[]> top20Pairs = pinPlayers.isEmpty()
                ? allPairs.subList(0, Math.min(20, allPairs.size()))
                : allPairs;
        backtrackWithPairs(subset, top20Pairs, allPairs, new ArrayList<>(), new boolean[subset.size()], result);
    }

    /**
     * Enumerate all C(optional, remaining) subsets of eligible players where locked players
     * (mustInclude) are always present. Subsets are sorted by:
     *   1. Cap-valid (totalUtr ≤ 40.5) first.
     *   2. Within same validity group: highest totalUtr first (closest to cap).
     *
     * Each returned subset is a unique 8-player combination.
     */
    private List<List<Player>> enumerateSubsets(List<Player> eligible, int size, Set<String> mustInclude) {
        List<Player> required = eligible.stream()
                .filter(p -> mustInclude.contains(p.getId()))
                .collect(Collectors.toList());
        List<Player> optional = eligible.stream()
                .filter(p -> !mustInclude.contains(p.getId()))
                .collect(Collectors.toList());

        if (required.size() > size) {
            return List.of();
        }

        int remaining = size - required.size();
        List<List<Player>> subsets = new ArrayList<>();
        generateCombinations(optional, remaining, 0, new ArrayList<>(), required, subsets);

        subsets.sort((a, b) -> {
            double utrA = totalUtr(a);
            double utrB = totalUtr(b);
            boolean validA = utrA <= UTR_CAP;
            boolean validB = utrB <= UTR_CAP;
            if (validA != validB) return validA ? -1 : 1;
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

    /**
     * Generate all valid pairs from pool where |player1.utr - player2.utr| ≤ 3.5,
     * sorted by combined UTR descending.
     */
    private List<int[]> generateValidPairs(List<Player> pool) {
        List<int[]> pairs = new ArrayList<>();
        for (int i = 0; i < pool.size(); i++) {
            for (int j = i + 1; j < pool.size(); j++) {
                if (Math.abs(pool.get(i).getUtr() - pool.get(j).getUtr()) <= 3.5) {
                    pairs.add(new int[]{i, j});
                }
            }
        }
        pairs.sort((a, b) -> Double.compare(
                pool.get(b[0]).getUtr() + pool.get(b[1]).getUtr(),
                pool.get(a[0]).getUtr() + pool.get(a[1]).getUtr()
        ));
        return pairs;
    }

    /**
     * Backtrack over pre-sorted pair candidates to assemble 4 non-overlapping pairs.
     *
     * Uses top20Pairs for the first 2 pair slots (which become D1/D2 after UTR-based
     * position assignment) and allPairs for the remaining slots (D3/D4).
     */
    private void backtrackWithPairs(List<Player> pool, List<int[]> top20Pairs, List<int[]> allPairs,
                                    List<int[]> currentPairs, boolean[] used, List<Lineup> result) {
        int pairCount = currentPairs.size();
        if (pairCount == 4) {
            Lineup lineup = buildLineup(pool, currentPairs);
            ConstraintService.ValidationResult validation =
                    constraintService.validateLineup(lineup, pool);
            if (validation.isValid()) {
                result.add(lineup);
            }
            return;
        }

        List<int[]> candidates = (pairCount < 2) ? top20Pairs : allPairs;

        for (int[] pair : candidates) {
            int i = pair[0], j = pair[1];
            if (used[i] || used[j]) continue;
            used[i] = true;
            used[j] = true;
            currentPairs.add(pair);
            backtrackWithPairs(pool, top20Pairs, allPairs, currentPairs, used, result);
            currentPairs.remove(currentPairs.size() - 1);
            used[i] = false;
            used[j] = false;
        }
    }

    /**
     * Check that all pin constraints are satisfied.
     */
    private boolean satisfiesPinConstraints(Lineup lineup, Map<String, String> pinPlayers,
                                            Map<String, List<String>> playersByPin) {
        for (Map.Entry<String, List<String>> entry : playersByPin.entrySet()) {
            String position = entry.getKey();
            List<String> pinnedIds = entry.getValue();

            if (pinnedIds.size() == 2) {
                String id1 = pinnedIds.get(0);
                String id2 = pinnedIds.get(1);
                boolean found = lineup.getPairs().stream()
                        .filter(pair -> position.equals(pair.getPosition()))
                        .anyMatch(pair ->
                                (id1.equals(pair.getPlayer1Id()) && id2.equals(pair.getPlayer2Id())) ||
                                (id2.equals(pair.getPlayer1Id()) && id1.equals(pair.getPlayer2Id())));
                if (!found) return false;
            } else {
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
     * Apply include-player and pin-constraint post-filters to a raw candidate list.
     */
    private List<Lineup> filterCandidates(List<Lineup> candidates, Set<String> effectiveInclude,
                                           Map<String, String> pinPlayers,
                                           Map<String, List<String>> playersByPin) {
        List<Lineup> result = new ArrayList<>(candidates);

        if (!effectiveInclude.isEmpty()) {
            result = result.stream()
                    .filter(lineup -> {
                        Set<String> usedIds = lineup.getPairs().stream()
                                .flatMap(p -> java.util.stream.Stream.of(p.getPlayer1Id(), p.getPlayer2Id()))
                                .collect(Collectors.toSet());
                        return usedIds.containsAll(effectiveInclude);
                    })
                    .collect(Collectors.toList());
        }

        if (!pinPlayers.isEmpty()) {
            result = result.stream()
                    .filter(lineup -> satisfiesPinConstraints(lineup, pinPlayers, playersByPin))
                    .collect(Collectors.toList());
        }

        return result;
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
        double actualUtrTotal = 0;

        for (int i = 0; i < sorted.size(); i++) {
            Player p1 = roster.get(sorted.get(i)[0]);
            Player p2 = roster.get(sorted.get(i)[1]);
            double combined = p1.getUtr() + p2.getUtr();
            total += combined;
            actualUtrTotal += getEffectiveActualUtr(p1) + getEffectiveActualUtr(p2);

            Pair pair = new Pair();
            pair.setPosition(positions[i]);
            pair.setPlayer1Id(p1.getId());
            pair.setPlayer1Name(p1.getName());
            pair.setPlayer1Utr(p1.getUtr());
            pair.setPlayer1Gender(p1.getGender());
            pair.setPlayer1Notes(p1.getNotes());
            pair.setPlayer1ActualUtr(p1.getActualUtr());
            pair.setPlayer2Id(p2.getId());
            pair.setPlayer2Name(p2.getName());
            pair.setPlayer2Utr(p2.getUtr());
            pair.setPlayer2Gender(p2.getGender());
            pair.setPlayer2Notes(p2.getNotes());
            pair.setPlayer2ActualUtr(p2.getActualUtr());
            pair.setCombinedUtr(combined);
            pairs.add(pair);
        }

        Lineup lineup = new Lineup();
        lineup.setPairs(pairs);
        lineup.setTotalUtr(total);
        lineup.setActualUtrSum(actualUtrTotal);
        lineup.setValid(true);
        lineup.setViolationMessages(new ArrayList<>());
        return lineup;
    }

    private double getEffectiveActualUtr(Player player) {
        return player.getActualUtr() != null ? player.getActualUtr() : player.getUtr();
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
