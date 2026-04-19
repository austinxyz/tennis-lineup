package com.tennis.service;

import com.tennis.controller.LineupUpdateRequest;
import com.tennis.exception.NotFoundException;
import com.tennis.model.Lineup;
import com.tennis.model.Pair;
import com.tennis.model.Player;
import com.tennis.model.Team;
import com.tennis.model.TeamData;
import com.tennis.repository.JsonRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class LineupService {

    private final JsonRepository jsonRepository;
    private final LineupGenerationService generationService;
    private final ZhipuAiService aiService;
    private final ConstraintService constraintService;

    @Autowired
    public LineupService(JsonRepository jsonRepository,
                         LineupGenerationService generationService,
                         ZhipuAiService aiService,
                         ConstraintService constraintService) {
        this.jsonRepository = jsonRepository;
        this.generationService = generationService;
        this.aiService = aiService;
        this.constraintService = constraintService;
    }

    public List<Lineup> generateMultiple(String teamId, String strategyType, String preset,
                                         String naturalLanguage,
                                         List<String> includePlayers, List<String> excludePlayers) {
        return generateMultiple(teamId, strategyType, preset, naturalLanguage,
                includePlayers, excludePlayers, Map.of());
    }

    public List<Lineup> generateMultiple(String teamId, String strategyType, String preset,
                                         String naturalLanguage,
                                         List<String> includePlayers, List<String> excludePlayers,
                                         Map<String, String> pinPlayers) {
        TeamData teamData = jsonRepository.readData();
        Team team = teamData.getTeams().stream()
                .filter(t -> t.getId().equals(teamId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("队伍不存在"));

        List<Player> players = team.getPlayers();
        if (players.size() < 8) {
            throw new IllegalArgumentException("队伍球员不足8人，无法生成排阵");
        }

        Set<String> include = new HashSet<>(includePlayers != null ? includePlayers : List.of());
        Set<String> exclude = new HashSet<>(excludePlayers != null ? excludePlayers : List.of());
        Map<String, String> pins = pinPlayers != null ? pinPlayers : Map.of();

        List<Lineup> candidates = generationService.generateCandidates(players, include, exclude, pins);
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("无法生成满足约束的排阵");
        }

        String strategy = "custom".equals(strategyType) ? "balanced" : (preset != null ? preset : "balanced");
        String strategyLabel = "custom".equals(strategyType)
                ? (naturalLanguage != null ? naturalLanguage : "custom")
                : strategy;

        // Sort all candidates by heuristic; for custom strategy, AI may reorder
        List<Lineup> sorted = sortByHeuristic(candidates, strategy);

        boolean aiUsed = false;
        if ("custom".equals(strategyType)) {
            int aiIndex = aiService.selectBestLineup(sorted, naturalLanguage != null ? naturalLanguage : strategy);
            if (aiIndex >= 0 && aiIndex < sorted.size()) {
                Lineup aiPick = sorted.remove(aiIndex);
                sorted.add(0, aiPick);
                aiUsed = true;
            }
        }

        // Dedup by player set: keep only the first lineup per unique 8-player combination
        Set<String> seenPlayerSets = new HashSet<>();
        List<Lineup> deduped = sorted.stream()
                .filter(l -> {
                    if (l.getPairs() == null) return true;
                    List<String> ids = l.getPairs().stream()
                            .flatMap(p -> java.util.stream.Stream.of(p.getPlayer1Id(), p.getPlayer2Id()))
                            .filter(java.util.Objects::nonNull)
                            .sorted()
                            .collect(Collectors.toList());
                    String key = String.join(",", ids);
                    return seenPlayerSets.add(key);
                })
                .collect(Collectors.toList());

        // Take up to 6 candidates
        List<Lineup> top6 = deduped.stream().limit(6).collect(Collectors.toList());

        // Assign metadata (id + createdAt needed for display and potential manual save)
        for (int i = 0; i < top6.size(); i++) {
            Lineup l = top6.get(i);
            l.setId(generateLineupId());
            l.setCreatedAt(Instant.now());
            l.setStrategy(strategyLabel);
            l.setAiUsed(i == 0 && aiUsed);
        }

        log.info("Generated {} candidates for team {}", top6.size(), teamId);
        return top6;
    }

    /**
     * Manually save a lineup chosen by the user to persistent storage.
     */
    public Lineup saveLineup(String teamId, Lineup lineup) {
        TeamData teamData = jsonRepository.readData();
        Team team = teamData.getTeams().stream()
                .filter(t -> t.getId().equals(teamId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("队伍不存在"));

        if (lineup.getId() == null || lineup.getId().isBlank()) {
            lineup.setId(generateLineupId());
        }
        if (lineup.getCreatedAt() == null) {
            lineup.setCreatedAt(Instant.now());
        }

        if (team.getLineups() == null) {
            team.setLineups(new ArrayList<>());
        }

        // Dedup: reject if same pairing combination already saved
        // (same 8 players in different pairings = different lineup, allowed)
        if (lineup.getPairs() != null && !lineup.getPairs().isEmpty()) {
            String incomingPairKey = buildPairingKeyByIds(lineup.getPairs());
            boolean duplicate = team.getLineups().stream().anyMatch(existing -> {
                if (existing.getPairs() == null || existing.getPairs().isEmpty()) return false;
                String existingKey = buildPairingKeyByIds(existing.getPairs());
                return incomingPairKey != null && incomingPairKey.equals(existingKey);
            });
            if (duplicate) {
                throw new IllegalArgumentException("该排阵已保存，请勿重复保存");
            }
        }

        team.getLineups().add(lineup);
        jsonRepository.writeData(teamData);

        log.info("Saved lineup {} for team {}", lineup.getId(), teamId);
        return lineup;
    }

    private static final double UTR_CAP = 40.5;
    private static final double TARGET_PER_LINE = UTR_CAP / 4; // 10.125

    private List<Lineup> sortByHeuristic(List<Lineup> candidates, String strategy) {
        List<Lineup> sorted = new ArrayList<>(candidates);
        if ("aggressive".equals(strategy)) {
            // Primary: closest to UTR cap; secondary: max top-three
            sorted.sort(Comparator
                    .comparingDouble(this::utrCapDistance)
                    .thenComparingDouble((Lineup l) -> topThreeUtr(l)).reversed()
                    .thenComparingDouble(this::utrCapDistance));
        } else {
            // Primary: closest to UTR cap; secondary: min deviation from 10.125 per line
            sorted.sort(Comparator
                    .comparingDouble(this::utrCapDistance)
                    .thenComparingDouble(this::balancedDeviation));
        }
        return sorted;
    }

    /** Distance from 40.5 cap — lower is better (closer to cap without exceeding). */
    private double utrCapDistance(Lineup lineup) {
        return UTR_CAP - lineup.getTotalUtr();
    }

    private double topThreeUtr(Lineup lineup) {
        return lineup.getPairs().stream()
                .filter(p -> "D1".equals(p.getPosition()) || "D2".equals(p.getPosition()) || "D3".equals(p.getPosition()))
                .mapToDouble(com.tennis.model.Pair::getCombinedUtr)
                .sum();
    }

    /** Sum of |pair.combinedUtr - 10.125| across all 4 pairs — lower is more balanced. */
    private double balancedDeviation(Lineup lineup) {
        return lineup.getPairs().stream()
                .mapToDouble(p -> Math.abs(p.getCombinedUtr() - TARGET_PER_LINE))
                .sum();
    }

    public List<Lineup> getLineupsByTeam(String teamId) {
        TeamData teamData = jsonRepository.readData();
        Team team = teamData.getTeams().stream()
                .filter(t -> t.getId().equals(teamId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("队伍不存在"));

        List<Lineup> lineups = team.getLineups();
        if (lineups == null) return new ArrayList<>();

        // Enrich historical pairs that lack UTR/gender fields (old format compatibility)
        Map<String, Player> playerMap = team.getPlayers() != null
                ? team.getPlayers().stream().collect(Collectors.toMap(Player::getId, p -> p))
                : Map.of();

        // Self-heal: for imported lineups whose player IDs don't match the current team roster
        // (common when importing from another environment), resolve IDs by player name.
        Map<String, Player> playerByName = team.getPlayers() != null
                ? team.getPlayers().stream()
                    .filter(p -> p.getName() != null)
                    .collect(Collectors.toMap(Player::getName, p -> p, (a, b) -> a))
                : Map.of();

        for (Lineup lineup : lineups) {
            for (Pair pair : lineup.getPairs()) {
                if (!playerMap.containsKey(pair.getPlayer1Id()) && pair.getPlayer1Name() != null) {
                    Player byName = playerByName.get(pair.getPlayer1Name());
                    if (byName != null) pair.setPlayer1Id(byName.getId());
                }
                if (!playerMap.containsKey(pair.getPlayer2Id()) && pair.getPlayer2Name() != null) {
                    Player byName = playerByName.get(pair.getPlayer2Name());
                    if (byName != null) pair.setPlayer2Id(byName.getId());
                }
            }
        }

        for (Lineup lineup : lineups) {
            double lineupTotalUtr = 0;
            double lineupActualUtrTotal = 0;
            for (Pair pair : lineup.getPairs()) {
                Player p1 = playerMap.get(pair.getPlayer1Id());
                Player p2 = playerMap.get(pair.getPlayer2Id());
                if (p1 != null) {
                    pair.setPlayer1Utr(p1.getUtr()); // always reflect current UTR
                    pair.setPlayer1ActualUtr(p1.getActualUtr()); // null if no override
                    if (pair.getPlayer1Gender() == null) pair.setPlayer1Gender(p1.getGender());
                }
                if (p2 != null) {
                    pair.setPlayer2Utr(p2.getUtr()); // always reflect current UTR
                    pair.setPlayer2ActualUtr(p2.getActualUtr()); // null if no override
                    if (pair.getPlayer2Gender() == null) pair.setPlayer2Gender(p2.getGender());
                }
                // Recalculate combinedUtr from current values
                double utr1 = pair.getPlayer1Utr() != null ? pair.getPlayer1Utr() : 0;
                double utr2 = pair.getPlayer2Utr() != null ? pair.getPlayer2Utr() : 0;
                pair.setCombinedUtr(utr1 + utr2);
                lineupTotalUtr += utr1 + utr2;
                // Actual UTR uses override if set, otherwise falls back to official UTR
                double actual1 = p1 != null ? p1.getEffectiveActualUtr() : utr1;
                double actual2 = p2 != null ? p2.getEffectiveActualUtr() : utr2;
                lineupActualUtrTotal += actual1 + actual2;
            }
            lineup.setTotalUtr(lineupTotalUtr);
            lineup.setActualUtrSum(lineupActualUtrTotal);
        }

        // Re-validate each lineup against current player UTRs
        List<Player> currentPlayers = team.getPlayers() != null ? team.getPlayers() : List.of();
        for (Lineup lineup : lineups) {
            ConstraintService.ValidationResult result = constraintService.validateLineup(lineup, currentPlayers);
            lineup.setCurrentValid(result.isValid());
            lineup.setCurrentViolations(result.getViolations());
        }

        return lineups.stream()
                .sorted(Comparator.comparingInt(Lineup::getSortOrder)
                        .thenComparing(Comparator.comparing(Lineup::getCreatedAt).reversed()))
                .toList();
    }

    public Map<String, Object> exportLineups(String teamId) {
        TeamData teamData = jsonRepository.readData();
        Team team = teamData.getTeams().stream()
                .filter(t -> t.getId().equals(teamId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("队伍不存在"));

        List<Lineup> lineups = team.getLineups() != null ? team.getLineups() : List.of();

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("exportedAt", Instant.now().toString());
        envelope.put("teamId", team.getId());
        envelope.put("teamName", team.getName());
        envelope.put("lineups", lineups);
        return envelope;
    }

    public Map<String, Integer> importLineups(String teamId, List<Lineup> incoming) {
        if (incoming == null) incoming = List.of();

        TeamData teamData = jsonRepository.readData();
        Team team = teamData.getTeams().stream()
                .filter(t -> t.getId().equals(teamId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("队伍不存在"));

        if (team.getLineups() == null) team.setLineups(new ArrayList<>());

        Set<String> existingNameKeys = team.getLineups().stream()
                .map(this::buildPlayerNameKey)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Imported lineups go AFTER existing ones, preserving source relative order.
        // label, comment, and pairs (with player names) are preserved as-is.
        int nextSortOrder = team.getLineups().stream()
                .mapToInt(Lineup::getSortOrder)
                .max()
                .orElse(-1) + 1;

        // Sort incoming by their original sortOrder so relative order carries over
        List<Lineup> sortedIncoming = incoming.stream()
                .sorted(Comparator.comparingInt(Lineup::getSortOrder))
                .collect(Collectors.toList());

        // Remap source player IDs to target team player IDs by name.
        // Without this, imported lineups' player IDs don't match the target's,
        // so ConstraintService (which looks up by ID) can't find players and
        // reports false violations like "上场女性球员少于2人".
        Map<String, String> nameToTargetId = team.getPlayers() == null ? Map.of()
                : team.getPlayers().stream()
                        .filter(p -> p.getName() != null)
                        .collect(Collectors.toMap(Player::getName, Player::getId, (a, b) -> a));

        int imported = 0;
        int skipped = 0;

        for (Lineup lineup : sortedIncoming) {
            if (lineup.getPairs() == null) { skipped++; continue; }
            String key = buildPlayerNameKey(lineup);
            if (key != null && existingNameKeys.contains(key)) {
                skipped++;
                continue;
            }
            // Remap pair player IDs by name so validation finds them in target team
            for (Pair pair : lineup.getPairs()) {
                if (pair.getPlayer1Name() != null && nameToTargetId.containsKey(pair.getPlayer1Name())) {
                    pair.setPlayer1Id(nameToTargetId.get(pair.getPlayer1Name()));
                }
                if (pair.getPlayer2Name() != null && nameToTargetId.containsKey(pair.getPlayer2Name())) {
                    pair.setPlayer2Id(nameToTargetId.get(pair.getPlayer2Name()));
                }
            }
            lineup.setId(generateLineupId());
            lineup.setCreatedAt(Instant.now());
            lineup.setSortOrder(nextSortOrder++);
            team.getLineups().add(lineup);
            if (key != null) existingNameKeys.add(key);
            imported++;
        }

        if (imported > 0) jsonRepository.writeData(teamData);
        log.info("Imported {} lineups for team {}, skipped {}", imported, teamId, skipped);
        return Map.of("imported", imported, "skipped", skipped);
    }

    /**
     * Build dedup key from pair combinations using player IDs (for saveLineup path).
     */
    private String buildPairingKeyByIds(List<Pair> pairs) {
        if (pairs == null) return null;
        List<String> pairKeys = pairs.stream()
                .filter(p -> p.getPlayer1Id() != null && p.getPlayer2Id() != null)
                .map(p -> {
                    String a = p.getPlayer1Id();
                    String b = p.getPlayer2Id();
                    return a.compareTo(b) <= 0 ? a + "+" + b : b + "+" + a;
                })
                .sorted()
                .collect(Collectors.toList());
        if (pairKeys.isEmpty()) return null;
        return String.join(",", pairKeys);
    }

    /**
     * Build a dedup key based on PAIRINGS (who is paired with whom), not just player set.
     * Two lineups are considered duplicate only if they have the same pair combinations,
     * regardless of position labels. Same 8 players in different pairings → different lineup.
     */
    private String buildPlayerNameKey(Lineup lineup) {
        if (lineup.getPairs() == null) return null;
        return buildPlayerNameKeyFromPairs(lineup.getPairs());
    }

    /**
     * Partially update a saved lineup. Only non-null fields in the request are applied.
     * When pairs are updated, the player-name dedup key is rebuilt and checked for duplicates.
     */
    public Lineup updateLineup(String teamId, String lineupId, LineupUpdateRequest req) {
        // Validate pairs dedup before entering the write lock (read-only check, cheap)
        if (req.getPairs() != null) {
            validatePairsNotDuplicate(teamId, lineupId, req.getPairs());
        }

        // Atomic read-modify-write under write lock — prevents lost-update races
        final Lineup[] result = new Lineup[1];
        jsonRepository.updateData(teamData -> {
            Team team = teamData.getTeams().stream()
                    .filter(t -> t.getId().equals(teamId))
                    .findFirst()
                    .orElseThrow(() -> new NotFoundException("队伍不存在"));

            Lineup target = team.getLineups() == null ? null : team.getLineups().stream()
                    .filter(l -> lineupId.equals(l.getId()))
                    .findFirst()
                    .orElse(null);
            if (target == null) {
                throw new NotFoundException("排阵不存在");
            }

            boolean changed = false;

            // null = don't update; blank string = explicit clear to null
            if (req.getLabel() != null) {
                target.setLabel(req.getLabel().isBlank() ? null : req.getLabel());
                changed = true;
            }
            if (req.getComment() != null) {
                target.setComment(req.getComment().isBlank() ? null : req.getComment());
                changed = true;
            }
            if (req.getSortOrder() != null) {
                target.setSortOrder(req.getSortOrder());
                changed = true;
            }
            if (req.getPairs() != null) {
                target.setPairs(new ArrayList<>(req.getPairs())); // defensive copy
                changed = true;
            }

            result[0] = target;
            if (!changed) return teamData; // skip write if nothing changed
            log.info("Updated lineup {} for team {}", lineupId, teamId);
            return teamData;
        });
        return result[0];
    }

    private void validatePairsNotDuplicate(String teamId, String lineupId, List<Pair> pairs) {
        TeamData teamData = jsonRepository.readData();
        Team team = teamData.getTeams().stream()
                .filter(t -> t.getId().equals(teamId))
                .findFirst()
                .orElse(null);
        if (team == null || team.getLineups() == null) return;

        // Both keys are now PAIRING-based (who is paired with whom),
        // not player-set-based. Same players in different pairings = different lineup.
        String incomingNameKey = buildPlayerNameKeyFromPairs(pairs);
        String incomingIdKey = buildPairingKeyByIds(pairs);

        boolean duplicate = team.getLineups().stream()
                .filter(l -> !lineupId.equals(l.getId()))
                .anyMatch(other -> {
                    String otherNameKey = buildPlayerNameKey(other);
                    // Use name-based pairing key when both sides have names
                    if (incomingNameKey != null && otherNameKey != null) {
                        return incomingNameKey.equals(otherNameKey);
                    }
                    // Fall back to ID-based pairing key
                    String otherIdKey = other.getPairs() != null ? buildPairingKeyByIds(other.getPairs()) : null;
                    if (incomingIdKey != null && otherIdKey != null) {
                        return incomingIdKey.equals(otherIdKey);
                    }
                    return false;
                });
        if (duplicate) {
            throw new IllegalArgumentException("该排阵已保存，请勿重复保存");
        }
    }

    /**
     * Build dedup key from pair combinations. Each pair contributes "name1+name2"
     * (with names sorted within the pair), and all pair keys are sorted and joined.
     * Same 8 players paired differently → different keys → different lineup.
     */
    private String buildPlayerNameKeyFromPairs(List<Pair> pairs) {
        if (pairs == null) return null;
        List<String> pairKeys = pairs.stream()
                .filter(p -> p.getPlayer1Name() != null && p.getPlayer2Name() != null)
                .map(p -> {
                    String a = p.getPlayer1Name();
                    String b = p.getPlayer2Name();
                    return a.compareTo(b) <= 0 ? a + "+" + b : b + "+" + a;
                })
                .sorted()
                .collect(Collectors.toList());
        if (pairKeys.isEmpty()) return null;
        return String.join(",", pairKeys);
    }

    public void deleteLineup(String lineupId) {
        TeamData teamData = jsonRepository.readData();
        for (Team team : teamData.getTeams()) {
            if (team.getLineups() != null) {
                boolean removed = team.getLineups().removeIf(l -> lineupId.equals(l.getId()));
                if (removed) {
                    jsonRepository.writeData(teamData);
                    log.info("Deleted lineup {}", lineupId);
                    return;
                }
            }
        }
        throw new NotFoundException("排阵不存在");
    }

    private String generateLineupId() {
        return "lineup-" + System.nanoTime();
    }
}
