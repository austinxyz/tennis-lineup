package com.tennis.service;

import com.tennis.model.Lineup;
import com.tennis.model.Pair;
import com.tennis.model.Player;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ConstraintService {

    public ValidationResult validateLineup(Lineup lineup, List<Player> players) {
        List<String> violations = new ArrayList<>();
        Map<String, Player> playerMap = players.stream()
                .collect(Collectors.toMap(Player::getId, p -> p));

        List<Pair> pairs = lineup.getPairs();

        // 1. Player uniqueness
        Set<String> usedIds = new HashSet<>();
        for (Pair pair : pairs) {
            if (!usedIds.add(pair.getPlayer1Id()) || !usedIds.add(pair.getPlayer2Id())) {
                violations.add("球员重复出现");
            }
        }

        // 2. Partner UTR gap ≤ 3.5
        for (Pair pair : pairs) {
            Player p1 = playerMap.get(pair.getPlayer1Id());
            Player p2 = playerMap.get(pair.getPlayer2Id());
            if (p1 != null && p2 != null) {
                double gap = Math.abs(p1.getUtr() - p2.getUtr());
                if (gap > 3.5) {
                    violations.add("搭档UTR差超过3.5: " + pair.getPosition());
                }
            }
        }

        // 3. Total UTR ≤ 40.5
        double totalUtr = usedIds.stream()
                .mapToDouble(id -> playerMap.containsKey(id) ? playerMap.get(id).getUtr() : 0)
                .sum();
        if (totalUtr > 40.5) {
            violations.add("总UTR超过40.5: " + totalUtr);
        }

        // 4. Minimum 2 female players on court
        long femaleCount = usedIds.stream()
                .filter(id -> playerMap.containsKey(id) && "female".equals(playerMap.get(id).getGender()))
                .count();
        if (femaleCount < 2) {
            violations.add("上场女性球员少于2人");
        }

        // 5. UTR ordering: D1 ≥ D2 ≥ D3 ≥ D4 by combined UTR
        String[] positions = {"D1", "D2", "D3", "D4"};
        Double prevUtr = null;
        for (String pos : positions) {
            Pair pair = pairs.stream().filter(p -> pos.equals(p.getPosition())).findFirst().orElse(null);
            if (pair != null) {
                if (prevUtr != null && pair.getCombinedUtr() > prevUtr) {
                    violations.add("UTR排序违反: " + pos + " 组合UTR高于上一线");
                }
                prevUtr = pair.getCombinedUtr();
            }
        }

        // 6. D4 must have verified players
        Pair d4 = pairs.stream().filter(p -> "D4".equals(p.getPosition())).findFirst().orElse(null);
        if (d4 != null) {
            Player p1 = playerMap.get(d4.getPlayer1Id());
            Player p2 = playerMap.get(d4.getPlayer2Id());
            if ((p1 != null && !Boolean.TRUE.equals(p1.getVerified())) ||
                (p2 != null && !Boolean.TRUE.equals(p2.getVerified()))) {
                violations.add("D4球员必须有Verified Doubles UTR");
            }
        }

        return new ValidationResult(violations.isEmpty(), violations);
    }

    public static class ValidationResult {
        private final boolean valid;
        private final List<String> violations;

        public ValidationResult(boolean valid, List<String> violations) {
            this.valid = valid;
            this.violations = violations;
        }

        public boolean isValid() { return valid; }
        public List<String> getViolations() { return violations; }
    }
}
