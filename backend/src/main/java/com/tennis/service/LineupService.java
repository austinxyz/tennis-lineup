package com.tennis.service;

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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LineupService {

    private final JsonRepository jsonRepository;
    private final LineupGenerationService generationService;
    private final ZhipuAiService aiService;

    @Autowired
    public LineupService(JsonRepository jsonRepository,
                         LineupGenerationService generationService,
                         ZhipuAiService aiService) {
        this.jsonRepository = jsonRepository;
        this.generationService = generationService;
        this.aiService = aiService;
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

        // Sort all candidates by heuristic; AI selects the best one (put it first)
        List<Lineup> sorted = sortByHeuristic(candidates, strategy);

        boolean aiUsed = false;
        if ("custom".equals(strategyType)) {
            int aiIndex = aiService.selectBestLineup(sorted, naturalLanguage != null ? naturalLanguage : strategy);
            if (aiIndex >= 0 && aiIndex < sorted.size()) {
                Lineup aiPick = sorted.remove(aiIndex);
                sorted.add(0, aiPick);
                aiUsed = true;
            }
        } else {
            int aiIndex = aiService.selectBestLineup(sorted, strategy);
            if (aiIndex >= 0 && aiIndex < sorted.size()) {
                Lineup aiPick = sorted.remove(aiIndex);
                sorted.add(0, aiPick);
                aiUsed = true;
            }
        }

        // Take up to 6 candidates
        List<Lineup> top6 = sorted.stream().limit(6).collect(Collectors.toList());

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

        for (Lineup lineup : lineups) {
            for (Pair pair : lineup.getPairs()) {
                Player p1 = playerMap.get(pair.getPlayer1Id());
                Player p2 = playerMap.get(pair.getPlayer2Id());
                if (p1 != null) {
                    if (pair.getPlayer1Utr() == null) pair.setPlayer1Utr(p1.getUtr());
                    if (pair.getPlayer1Gender() == null) pair.setPlayer1Gender(p1.getGender());
                }
                if (p2 != null) {
                    if (pair.getPlayer2Utr() == null) pair.setPlayer2Utr(p2.getUtr());
                    if (pair.getPlayer2Gender() == null) pair.setPlayer2Gender(p2.getGender());
                }
            }
        }

        return lineups.stream()
                .sorted(Comparator.comparing(Lineup::getCreatedAt).reversed())
                .toList();
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
