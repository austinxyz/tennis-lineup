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
import java.util.List;

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

    public Lineup generateAndSave(String teamId, String strategyType, String preset, String naturalLanguage) {
        TeamData teamData = jsonRepository.readData();
        Team team = teamData.getTeams().stream()
                .filter(t -> t.getId().equals(teamId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("队伍不存在"));

        List<Player> players = team.getPlayers();
        if (players.size() < 8) {
            throw new IllegalArgumentException("队伍球员不足8人，无法生成排阵");
        }

        List<Lineup> candidates = generationService.generateCandidates(players);
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("无法生成满足约束的排阵");
        }

        Lineup selected;
        boolean aiUsed = false;

        if ("custom".equals(strategyType)) {
            int aiIndex = aiService.selectBestLineup(candidates, naturalLanguage != null ? naturalLanguage : "balanced");
            if (aiIndex >= 0) {
                selected = candidates.get(aiIndex);
                aiUsed = true;
            } else {
                selected = generationService.selectByHeuristic(candidates, "balanced");
            }
        } else {
            String strategy = preset != null ? preset : "balanced";
            int aiIndex = aiService.selectBestLineup(candidates, strategy);
            if (aiIndex >= 0) {
                selected = candidates.get(aiIndex);
                aiUsed = true;
            } else {
                selected = generationService.selectByHeuristic(candidates, strategy);
            }
        }

        String strategyLabel = "custom".equals(strategyType)
                ? (naturalLanguage != null ? naturalLanguage : "custom")
                : (preset != null ? preset : "balanced");

        selected.setId(generateLineupId());
        selected.setCreatedAt(Instant.now());
        selected.setStrategy(strategyLabel);
        selected.setAiUsed(aiUsed);

        if (team.getLineups() == null) {
            team.setLineups(new ArrayList<>());
        }
        team.getLineups().add(selected);
        jsonRepository.writeData(teamData);

        log.info("Generated and saved lineup {} for team {}", selected.getId(), teamId);
        return selected;
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
