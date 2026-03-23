package com.tennis.service;

import com.tennis.controller.MatchupCommentaryRequest;
import com.tennis.controller.MatchupCommentaryResponse;
import com.tennis.controller.MatchupCommentaryResponse.LineCommentary;
import com.tennis.exception.NotFoundException;
import com.tennis.model.Lineup;
import com.tennis.model.Pair;
import com.tennis.model.Team;
import com.tennis.model.TeamData;
import com.tennis.repository.JsonRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class MatchupCommentaryService {

    private final JsonRepository jsonRepository;
    private final ZhipuAiService aiService;

    @Autowired
    public MatchupCommentaryService(JsonRepository jsonRepository, ZhipuAiService aiService) {
        this.jsonRepository = jsonRepository;
        this.aiService = aiService;
    }

    public MatchupCommentaryResponse getCommentary(MatchupCommentaryRequest request) {
        TeamData teamData = jsonRepository.readData();

        Lineup ownLineup = findLineup(teamData, request.getTeamId(), request.getOwnLineupId());
        Lineup oppLineup = findLineup(teamData, request.getOpponentTeamId(), request.getOpponentLineupId());

        Map<String, String> aiCommentary = aiService.getCommentary(ownLineup, oppLineup);
        boolean aiUsed = !aiCommentary.isEmpty();

        List<LineCommentary> lines = buildLines(ownLineup, oppLineup, aiCommentary);
        return new MatchupCommentaryResponse(lines, aiUsed);
    }

    private Lineup findLineup(TeamData teamData, String teamId, String lineupId) {
        Team team = teamData.getTeams().stream()
                .filter(t -> t.getId().equals(teamId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("队伍不存在: " + teamId));

        return team.getLineups().stream()
                .filter(l -> l.getId().equals(lineupId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("排阵不存在: " + lineupId));
    }

    private List<LineCommentary> buildLines(Lineup ownLineup, Lineup oppLineup, Map<String, String> aiCommentary) {
        List<LineCommentary> lines = new ArrayList<>();

        Map<String, Pair> ownByPos = pairsByPosition(ownLineup);
        Map<String, Pair> oppByPos = pairsByPosition(oppLineup);

        for (String pos : List.of("D1", "D2", "D3", "D4")) {
            Pair own = ownByPos.get(pos);
            Pair opp = oppByPos.get(pos);
            if (own == null || opp == null) continue;

            String commentary;
            if (aiCommentary.containsKey(pos)) {
                commentary = aiCommentary.get(pos);
            } else {
                commentary = fallbackCommentary(own, opp);
            }
            lines.add(new LineCommentary(pos, commentary));
        }

        return lines;
    }

    private String fallbackCommentary(Pair own, Pair opp) {
        double delta = (own.getCombinedUtr() != null ? own.getCombinedUtr() : 0)
                     - (opp.getCombinedUtr() != null ? opp.getCombinedUtr() : 0);
        if (delta > 0.5) return "具备优势，建议主动进攻";
        if (delta < -0.5) return "处于劣势，多以防守反击为主";
        return "势均力敌，注重稳定发挥";
    }

    private Map<String, Pair> pairsByPosition(Lineup lineup) {
        Map<String, Pair> map = new java.util.HashMap<>();
        if (lineup != null && lineup.getPairs() != null) {
            for (Pair p : lineup.getPairs()) {
                map.put(p.getPosition(), p);
            }
        }
        return map;
    }
}
