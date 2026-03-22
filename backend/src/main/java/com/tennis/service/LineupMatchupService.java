package com.tennis.service;

import com.tennis.controller.LineupMatchupRequest;
import com.tennis.exception.NotFoundException;
import com.tennis.model.LineAnalysis;
import com.tennis.model.Lineup;
import com.tennis.model.LineupMatchupResponse;
import com.tennis.model.MatchupResult;
import com.tennis.model.Pair;
import com.tennis.model.Player;
import com.tennis.model.Team;
import com.tennis.model.TeamData;
import com.tennis.repository.JsonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LineupMatchupService {

    private static final Map<String, Integer> POSITION_POINTS = Map.of(
            "D1", 1, "D2", 2, "D3", 3, "D4", 4
    );

    private final JsonRepository jsonRepository;
    private final OpponentAnalysisService opponentAnalysisService;

    @Autowired
    public LineupMatchupService(JsonRepository jsonRepository,
                                OpponentAnalysisService opponentAnalysisService) {
        this.jsonRepository = jsonRepository;
        this.opponentAnalysisService = opponentAnalysisService;
    }

    public LineupMatchupResponse matchup(LineupMatchupRequest request) {
        TeamData teamData = jsonRepository.readData();

        Team ownTeam = teamData.getTeams().stream()
                .filter(t -> t.getId().equals(request.getTeamId()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("队伍不存在"));

        Team opponentTeam = teamData.getTeams().stream()
                .filter(t -> t.getId().equals(request.getOpponentTeamId()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("对手队伍不存在"));

        // Find and enrich opponent lineup UTRs from current roster
        Lineup opponentLineup = findAndEnrichLineup(
                opponentTeam, request.getOpponentLineupId(), "对手排阵不存在");

        // Get own team's saved lineups (may be empty)
        List<Lineup> ownLineups = ownTeam.getLineups() != null ? ownTeam.getLineups() : List.of();

        if (ownLineups.isEmpty()) {
            return new LineupMatchupResponse(List.of());
        }

        // Enrich own lineup UTRs from current own team roster
        Map<String, Player> ownPlayerMap = ownTeam.getPlayers() != null
                ? ownTeam.getPlayers().stream().collect(Collectors.toMap(Player::getId, p -> p))
                : Map.of();

        // Build opponent position → combinedUtr map
        Map<String, Double> opponentUtrByPosition = opponentLineup.getPairs().stream()
                .collect(Collectors.toMap(Pair::getPosition, Pair::getCombinedUtr));

        List<MatchupResult> results = new ArrayList<>();
        for (Lineup ownLineup : ownLineups) {
            Lineup enriched = enrichLineup(ownLineup, ownPlayerMap);
            List<LineAnalysis> analysis = opponentAnalysisService.computeLineAnalysis(enriched, opponentUtrByPosition);
            double expectedScore = analysis.stream()
                    .mapToDouble(a -> POSITION_POINTS.getOrDefault(a.getPosition(), 0) * a.getWinProbability())
                    .sum();
            double rounded = Math.round(expectedScore * 10.0) / 10.0;
            double opponentScore = Math.round((10.0 - expectedScore) * 10.0) / 10.0;
            String verdict = verdictFor(rounded);
            results.add(new MatchupResult(enriched, opponentLineup, analysis, rounded, opponentScore, verdict));
        }

        results.sort(Comparator.comparingDouble(MatchupResult::getExpectedScore).reversed());
        return new LineupMatchupResponse(results);
    }

    private Lineup findAndEnrichLineup(Team team, String lineupId, String notFoundMsg) {
        List<Lineup> lineups = team.getLineups();
        if (lineups == null || lineups.isEmpty()) {
            throw new NotFoundException(notFoundMsg);
        }
        Lineup lineup = lineups.stream()
                .filter(l -> lineupId.equals(l.getId()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(notFoundMsg));

        Map<String, Player> playerMap = team.getPlayers() != null
                ? team.getPlayers().stream().collect(Collectors.toMap(Player::getId, p -> p))
                : Map.of();
        return enrichLineup(lineup, playerMap);
    }

    private Lineup enrichLineup(Lineup lineup, Map<String, Player> playerMap) {
        // Create a shallow copy with enriched pair UTRs (don't mutate stored data)
        List<Pair> enrichedPairs = new ArrayList<>();
        double totalUtr = 0;
        for (Pair pair : lineup.getPairs()) {
            Pair p = copyPair(pair);
            Player p1 = playerMap.get(pair.getPlayer1Id());
            Player p2 = playerMap.get(pair.getPlayer2Id());
            double utr1 = p1 != null ? p1.getUtr() : (pair.getPlayer1Utr() != null ? pair.getPlayer1Utr() : 0);
            double utr2 = p2 != null ? p2.getUtr() : (pair.getPlayer2Utr() != null ? pair.getPlayer2Utr() : 0);
            p.setPlayer1Utr(utr1);
            p.setPlayer2Utr(utr2);
            p.setCombinedUtr(utr1 + utr2);
            totalUtr += utr1 + utr2;
            enrichedPairs.add(p);
        }
        Lineup copy = new Lineup();
        copy.setId(lineup.getId());
        copy.setCreatedAt(lineup.getCreatedAt());
        copy.setStrategy(lineup.getStrategy());
        copy.setAiUsed(lineup.isAiUsed());
        copy.setPairs(enrichedPairs);
        copy.setTotalUtr(totalUtr);
        copy.setValid(lineup.isValid());
        copy.setViolationMessages(lineup.getViolationMessages());
        return copy;
    }

    private Pair copyPair(Pair src) {
        Pair p = new Pair();
        p.setPosition(src.getPosition());
        p.setPlayer1Id(src.getPlayer1Id());
        p.setPlayer1Name(src.getPlayer1Name());
        p.setPlayer1Utr(src.getPlayer1Utr());
        p.setPlayer1Gender(src.getPlayer1Gender());
        p.setPlayer2Id(src.getPlayer2Id());
        p.setPlayer2Name(src.getPlayer2Name());
        p.setPlayer2Utr(src.getPlayer2Utr());
        p.setPlayer2Gender(src.getPlayer2Gender());
        p.setCombinedUtr(src.getCombinedUtr());
        return p;
    }

    String verdictFor(double expectedScore) {
        if (expectedScore > 6.0) return "能赢";
        if (expectedScore >= 4.0) return "势均力敌";
        return "劣势";
    }
}
