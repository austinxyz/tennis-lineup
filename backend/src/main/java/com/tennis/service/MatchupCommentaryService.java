package com.tennis.service;

import com.tennis.controller.LineupMatchupRequest;
import com.tennis.controller.MatchupCommentaryRequest;
import com.tennis.controller.MatchupCommentaryResponse;
import com.tennis.controller.MatchupCommentaryResponse.LineCommentary;
import com.tennis.exception.NotFoundException;
import com.tennis.model.Lineup;
import com.tennis.model.Pair;
import com.tennis.model.PartnerNote;
import com.tennis.model.Player;
import com.tennis.model.Team;
import com.tennis.model.TeamData;
import com.tennis.repository.JsonRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

        // Enrich lineups with current player actual UTRs
        enrichLineupActualUtrs(ownLineup, findTeam(teamData, request.getTeamId()));
        enrichLineupActualUtrs(oppLineup, findTeam(teamData, request.getOpponentTeamId()));

        // Use partner notes from request, or fetch from repository
        List<LineupMatchupRequest.PartnerNoteDto> ownNotes = resolvePartnerNotes(
                request.getOwnPartnerNotes(), teamData, request.getTeamId());
        List<LineupMatchupRequest.PartnerNoteDto> oppNotes = resolvePartnerNotes(
                request.getOpponentPartnerNotes(), teamData, request.getOpponentTeamId());

        Map<String, String> aiCommentary = aiService.getCommentary(ownLineup, oppLineup, ownNotes, oppNotes);
        boolean aiUsed = !aiCommentary.isEmpty();

        List<LineCommentary> lines = buildLines(ownLineup, oppLineup, aiCommentary);
        return new MatchupCommentaryResponse(lines, aiUsed);
    }

    private void enrichLineupActualUtrs(Lineup lineup, Team team) {
        if (team.getPlayers() == null || lineup.getPairs() == null) return;
        Map<String, Player> playerMap = team.getPlayers().stream()
                .collect(Collectors.toMap(Player::getId, p -> p));
        for (Pair pair : lineup.getPairs()) {
            Player p1 = playerMap.get(pair.getPlayer1Id());
            Player p2 = playerMap.get(pair.getPlayer2Id());
            if (p1 != null) pair.setPlayer1ActualUtr(p1.getActualUtr());
            if (p2 != null) pair.setPlayer2ActualUtr(p2.getActualUtr());
            double u1 = pair.getPlayer1Utr() != null ? pair.getPlayer1Utr() : 0;
            double u2 = pair.getPlayer2Utr() != null ? pair.getPlayer2Utr() : 0;
            double a1 = (p1 != null && p1.getActualUtr() != null) ? p1.getActualUtr() : u1;
            double a2 = (p2 != null && p2.getActualUtr() != null) ? p2.getActualUtr() : u2;
            pair.setCombinedActualUtr(a1 + a2);
        }
    }

    private List<LineupMatchupRequest.PartnerNoteDto> resolvePartnerNotes(
            List<LineupMatchupRequest.PartnerNoteDto> fromRequest, TeamData teamData, String teamId) {
        if (fromRequest != null && !fromRequest.isEmpty()) return fromRequest;
        Team team = teamData.getTeams().stream()
                .filter(t -> t.getId().equals(teamId)).findFirst().orElse(null);
        if (team == null || team.getPartnerNotes() == null) return List.of();
        return team.getPartnerNotes().stream()
                .filter(n -> n.getNote() != null && !n.getNote().isBlank())
                .limit(10)
                .map(n -> {
                    LineupMatchupRequest.PartnerNoteDto dto = new LineupMatchupRequest.PartnerNoteDto();
                    dto.setPlayer1Name(n.getPlayer1Name());
                    dto.setPlayer2Name(n.getPlayer2Name());
                    dto.setNote(n.getNote());
                    return dto;
                })
                .toList();
    }

    private Team findTeam(TeamData teamData, String teamId) {
        return teamData.getTeams().stream()
                .filter(t -> t.getId().equals(teamId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("队伍不存在: " + teamId));
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
