package com.tennis.service;

import com.tennis.exception.NotFoundException;
import com.tennis.model.Lineup;
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

    public List<Lineup> generateMultipleAndSave(String teamId, String strategyType, String preset,
                                                String naturalLanguage,
                                                List<String> includePlayers, List<String> excludePlayers) {
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

        List<Lineup> candidates = generationService.generateCandidates(players, include, exclude);
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

        // Assign metadata to all returned lineups
        for (int i = 0; i < top6.size(); i++) {
            Lineup l = top6.get(i);
            l.setId(generateLineupId());
            l.setCreatedAt(Instant.now());
            l.setStrategy(strategyLabel);
            l.setAiUsed(i == 0 && aiUsed);
        }

        // Persist only the first (best) lineup
        if (team.getLineups() == null) {
            team.setLineups(new ArrayList<>());
        }
        team.getLineups().add(top6.get(0));
        jsonRepository.writeData(teamData);

        log.info("Generated {} candidates, saved best lineup {} for team {}", top6.size(), top6.get(0).getId(), teamId);
        return top6;
    }

    private List<Lineup> sortByHeuristic(List<Lineup> candidates, String strategy) {
        List<Lineup> sorted = new ArrayList<>(candidates);
        if ("aggressive".equals(strategy)) {
            sorted.sort(Comparator.comparingDouble(this::topThreeUtr).reversed());
        } else {
            sorted.sort(Comparator.comparingDouble(this::combinedUtrVariance));
        }
        return sorted;
    }

    private double topThreeUtr(Lineup lineup) {
        return lineup.getPairs().stream()
                .filter(p -> "D1".equals(p.getPosition()) || "D2".equals(p.getPosition()) || "D3".equals(p.getPosition()))
                .mapToDouble(com.tennis.model.Pair::getCombinedUtr)
                .sum();
    }

    private double combinedUtrVariance(Lineup lineup) {
        List<Double> utrs = lineup.getPairs().stream()
                .map(com.tennis.model.Pair::getCombinedUtr)
                .toList();
        double mean = utrs.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        return utrs.stream().mapToDouble(u -> (u - mean) * (u - mean)).average().orElse(0);
    }

    public List<Lineup> getLineupsByTeam(String teamId) {
        TeamData teamData = jsonRepository.readData();
        Team team = teamData.getTeams().stream()
                .filter(t -> t.getId().equals(teamId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("队伍不存在"));

        List<Lineup> lineups = team.getLineups();
        if (lineups == null) return new ArrayList<>();
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
