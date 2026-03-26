package com.tennis.service;

import com.tennis.controller.LineupMatchupRequest;
import com.tennis.exception.NotFoundException;
import com.tennis.model.AiRecommendation;
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
    private final ZhipuAiService aiService;

    @Autowired
    public LineupMatchupService(JsonRepository jsonRepository,
                                OpponentAnalysisService opponentAnalysisService,
                                ZhipuAiService aiService) {
        this.jsonRepository = jsonRepository;
        this.opponentAnalysisService = opponentAnalysisService;
        this.aiService = aiService;
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
        List<Lineup> allOwnLineups = ownTeam.getLineups() != null ? ownTeam.getLineups() : List.of();

        // Filter to specific own lineup if provided (Mode B head-to-head)
        List<Lineup> ownLineups;
        if (request.getOwnLineupId() != null && !request.getOwnLineupId().isBlank()) {
            ownLineups = allOwnLineups.stream()
                    .filter(l -> request.getOwnLineupId().equals(l.getId()))
                    .toList();
        } else {
            ownLineups = allOwnLineups;
        }

        if (ownLineups.isEmpty()) {
            return new LineupMatchupResponse(List.of(), null);
        }

        // Enrich own lineup UTRs from current own team roster
        Map<String, Player> ownPlayerMap = ownTeam.getPlayers() != null
                ? ownTeam.getPlayers().stream().collect(Collectors.toMap(Player::getId, p -> p))
                : Map.of();

        // Build opponent position → combinedUtr and combinedActualUtr maps
        Map<String, Double> opponentUtrByPosition = opponentLineup.getPairs().stream()
                .collect(Collectors.toMap(Pair::getPosition, Pair::getCombinedUtr));
        Map<String, Double> opponentActualUtrByPosition = opponentLineup.getPairs().stream()
                .collect(Collectors.toMap(Pair::getPosition,
                        p -> p.getCombinedActualUtr() != null ? p.getCombinedActualUtr() : p.getCombinedUtr()));

        List<MatchupResult> results = new ArrayList<>();
        for (Lineup ownLineup : ownLineups) {
            Lineup enriched = enrichLineup(ownLineup, ownPlayerMap);
            List<LineAnalysis> analysis = opponentAnalysisService.computeLineAnalysis(enriched, opponentUtrByPosition, opponentActualUtrByPosition);
            double expectedScore = analysis.stream()
                    .mapToDouble(a -> POSITION_POINTS.getOrDefault(a.getPosition(), 0) * a.getWinProbability())
                    .sum();
            double rounded = Math.round(expectedScore * 10.0) / 10.0;
            double opponentScore = Math.round((10.0 - expectedScore) * 10.0) / 10.0;
            String verdict = verdictFor(rounded);
            results.add(new MatchupResult(enriched, opponentLineup, analysis, rounded, opponentScore, verdict));
        }

        results.sort(Comparator.comparingDouble(MatchupResult::getExpectedScore).reversed());

        // AI recommendation: only when includeAi=true AND no specific ownLineupId
        AiRecommendation aiRec = null;
        if (request.isIncludeAi() && (request.getOwnLineupId() == null || request.getOwnLineupId().isBlank())) {
            aiRec = computeAiRecommendation(results, opponentLineup, opponentUtrByPosition,
                    opponentActualUtrByPosition, request.getOwnPartnerNotes(), request.getOpponentPartnerNotes());
        }

        return new LineupMatchupResponse(results, aiRec);
    }

    private AiRecommendation computeAiRecommendation(List<MatchupResult> sortedResults,
                                                      Lineup opponentLineup,
                                                      Map<String, Double> opponentUtrByPosition,
                                                      Map<String, Double> opponentActualUtrByPosition,
                                                      List<LineupMatchupRequest.PartnerNoteDto> ownPartnerNotes,
                                                      List<LineupMatchupRequest.PartnerNoteDto> opponentPartnerNotes) {
        // Take top-5 by expected score for AI to evaluate
        List<Lineup> top5 = sortedResults.stream()
                .limit(5)
                .map(MatchupResult::getLineup)
                .toList();

        ZhipuAiService.AiResult aiResult = aiService.selectBestWithResult(
                top5, "均衡", opponentLineup, ownPartnerNotes, opponentPartnerNotes);
        int aiIndex = aiResult.index();

        Lineup aiLineup;
        String explanation;
        boolean aiUsed;

        if (aiIndex >= 0 && aiIndex < top5.size()) {
            aiLineup = top5.get(aiIndex);
            explanation = aiResult.explanation() != null
                    ? aiResult.explanation()
                    : "AI 根据对手排阵选择最优方案";
            aiUsed = true;
        } else {
            // Fallback to UTR top result
            aiLineup = sortedResults.get(0).getLineup();
            explanation = "AI 不可用，已用UTR分析代替";
            aiUsed = false;
        }

        List<LineAnalysis> lineAnalysis = opponentAnalysisService.computeLineAnalysis(aiLineup, opponentUtrByPosition, opponentActualUtrByPosition);
        double expectedScore = lineAnalysis.stream()
                .mapToDouble(a -> POSITION_POINTS.getOrDefault(a.getPosition(), 0) * a.getWinProbability())
                .sum();
        double totalPoints = POSITION_POINTS.values().stream().mapToInt(Integer::intValue).sum();

        return new AiRecommendation(aiLineup, opponentLineup, lineAnalysis,
                Math.round(expectedScore * 10.0) / 10.0,
                Math.round((totalPoints - expectedScore) * 10.0) / 10.0,
                explanation, aiUsed);
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
        List<Pair> enrichedPairs = new ArrayList<>();
        double totalUtr = 0;
        double actualUtrTotal = 0;
        for (Pair pair : lineup.getPairs()) {
            Pair p = copyPair(pair);
            Player p1 = playerMap.get(pair.getPlayer1Id());
            Player p2 = playerMap.get(pair.getPlayer2Id());
            double utr1 = p1 != null ? p1.getUtr() : (pair.getPlayer1Utr() != null ? pair.getPlayer1Utr() : 0);
            double utr2 = p2 != null ? p2.getUtr() : (pair.getPlayer2Utr() != null ? pair.getPlayer2Utr() : 0);
            p.setPlayer1Utr(utr1);
            p.setPlayer2Utr(utr2);
            if (p1 != null) p.setPlayer1ActualUtr(p1.getActualUtr());
            if (p2 != null) p.setPlayer2ActualUtr(p2.getActualUtr());
            p.setCombinedUtr(utr1 + utr2);
            totalUtr += utr1 + utr2;
            double a1 = (p1 != null && p1.getActualUtr() != null) ? p1.getActualUtr() : utr1;
            double a2 = (p2 != null && p2.getActualUtr() != null) ? p2.getActualUtr() : utr2;
            actualUtrTotal += a1 + a2;
            p.setCombinedActualUtr(a1 + a2);
            enrichedPairs.add(p);
        }
        Lineup copy = new Lineup();
        copy.setId(lineup.getId());
        copy.setCreatedAt(lineup.getCreatedAt());
        copy.setStrategy(lineup.getStrategy());
        copy.setAiUsed(lineup.isAiUsed());
        copy.setPairs(enrichedPairs);
        copy.setTotalUtr(totalUtr);
        copy.setActualUtrSum(actualUtrTotal);
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
        p.setPlayer1ActualUtr(src.getPlayer1ActualUtr());
        p.setPlayer1Gender(src.getPlayer1Gender());
        p.setPlayer1Notes(src.getPlayer1Notes());
        p.setPlayer2Id(src.getPlayer2Id());
        p.setPlayer2Name(src.getPlayer2Name());
        p.setPlayer2Utr(src.getPlayer2Utr());
        p.setPlayer2ActualUtr(src.getPlayer2ActualUtr());
        p.setPlayer2Gender(src.getPlayer2Gender());
        p.setPlayer2Notes(src.getPlayer2Notes());
        p.setCombinedUtr(src.getCombinedUtr());
        return p;
    }

    String verdictFor(double expectedScore) {
        if (expectedScore > 6.0) return "能赢";
        if (expectedScore >= 4.0) return "势均力敌";
        return "劣势";
    }
}
