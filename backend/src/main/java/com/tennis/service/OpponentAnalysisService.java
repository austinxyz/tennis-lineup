package com.tennis.service;

import com.tennis.controller.OpponentAnalysisRequest;
import com.tennis.exception.NotFoundException;
import com.tennis.model.AiRecommendation;
import com.tennis.model.LineAnalysis;
import com.tennis.model.Lineup;
import com.tennis.model.OpponentAnalysisResponse;
import com.tennis.model.Pair;
import com.tennis.model.Player;
import com.tennis.model.Team;
import com.tennis.model.TeamData;
import com.tennis.model.UtrRecommendation;
import com.tennis.repository.JsonRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OpponentAnalysisService {

    // Position points: D1=1, D2=2, D3=3, D4=4
    private static final Map<String, Integer> POSITION_POINTS = Map.of(
            "D1", 1, "D2", 2, "D3", 3, "D4", 4
    );

    private final JsonRepository jsonRepository;
    private final LineupGenerationService generationService;
    private final ZhipuAiService aiService;

    @Autowired
    public OpponentAnalysisService(JsonRepository jsonRepository,
                                   LineupGenerationService generationService,
                                   ZhipuAiService aiService) {
        this.jsonRepository = jsonRepository;
        this.generationService = generationService;
        this.aiService = aiService;
    }

    public OpponentAnalysisResponse analyze(OpponentAnalysisRequest request) {
        TeamData teamData = jsonRepository.readData();

        // Load own team
        Team ownTeam = teamData.getTeams().stream()
                .filter(t -> t.getId().equals(request.getTeamId()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("队伍不存在"));

        // Load opponent team
        Team opponentTeam = teamData.getTeams().stream()
                .filter(t -> t.getId().equals(request.getOpponentTeamId()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("对手队伍不存在"));

        // Load and enrich opponent lineup
        Lineup opponentLineup = findAndEnrichOpponentLineup(opponentTeam, request.getOpponentLineupId());

        // Generate own-team candidates
        Set<String> include = new HashSet<>(request.getIncludePlayers() != null ? request.getIncludePlayers() : List.of());
        Set<String> exclude = new HashSet<>(request.getExcludePlayers() != null ? request.getExcludePlayers() : List.of());
        Map<String, String> pins = request.getPinPlayers() != null ? request.getPinPlayers() : Map.of();

        List<Player> ownPlayers = ownTeam.getPlayers();
        List<Lineup> candidates = generationService.generateCandidates(ownPlayers, include, exclude, pins);

        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("无法生成满足约束的排阵");
        }

        // Compute UTR recommendation
        UtrRecommendation utrRec = computeUtrRecommendation(candidates, opponentLineup);

        // Compute AI recommendation only if explicitly requested
        AiRecommendation aiRec = request.isIncludeAi()
                ? computeAiRecommendation(candidates, opponentLineup, request, utrRec)
                : null;

        return new OpponentAnalysisResponse(utrRec, aiRec);
    }

    private Lineup findAndEnrichOpponentLineup(Team opponentTeam, String opponentLineupId) {
        List<Lineup> opponentLineups = opponentTeam.getLineups();
        if (opponentLineups == null || opponentLineups.isEmpty()) {
            throw new NotFoundException("对手排阵不存在");
        }
        Lineup lineup = opponentLineups.stream()
                .filter(l -> opponentLineupId.equals(l.getId()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("对手排阵不存在"));

        // Enrich opponent pair UTRs from current opponent roster
        Map<String, Player> playerMap = opponentTeam.getPlayers() != null
                ? opponentTeam.getPlayers().stream().collect(Collectors.toMap(Player::getId, p -> p))
                : Map.of();

        for (Pair pair : lineup.getPairs()) {
            Player p1 = playerMap.get(pair.getPlayer1Id());
            Player p2 = playerMap.get(pair.getPlayer2Id());
            double utr1 = p1 != null ? p1.getUtr() : (pair.getPlayer1Utr() != null ? pair.getPlayer1Utr() : 0);
            double utr2 = p2 != null ? p2.getUtr() : (pair.getPlayer2Utr() != null ? pair.getPlayer2Utr() : 0);
            pair.setCombinedUtr(utr1 + utr2);
            if (p1 != null) pair.setPlayer1ActualUtr(p1.getActualUtr());
            if (p2 != null) pair.setPlayer2ActualUtr(p2.getActualUtr());
        }

        return lineup;
    }

    UtrRecommendation computeUtrRecommendation(List<Lineup> candidates, Lineup opponentLineup) {
        // Build position→combinedUtr map for opponent
        Map<String, Double> opponentUtrByPosition = opponentLineup.getPairs().stream()
                .collect(Collectors.toMap(Pair::getPosition, Pair::getCombinedUtr));

        Lineup bestLineup = null;
        List<LineAnalysis> bestAnalysis = null;
        double bestExpectedScore = -1;

        for (Lineup candidate : candidates) {
            List<LineAnalysis> analysis = computeLineAnalysis(candidate, opponentUtrByPosition);
            double expectedScore = analysis.stream()
                    .mapToDouble(a -> POSITION_POINTS.getOrDefault(a.getPosition(), 0) * a.getWinProbability())
                    .sum();

            if (expectedScore > bestExpectedScore) {
                bestExpectedScore = expectedScore;
                bestLineup = candidate;
                bestAnalysis = analysis;
            }
        }

        double totalPoints = POSITION_POINTS.values().stream().mapToInt(Integer::intValue).sum(); // 10
        double opponentExpectedScore = totalPoints - bestExpectedScore;

        return new UtrRecommendation(bestLineup, bestAnalysis,
                Math.round(bestExpectedScore * 10.0) / 10.0,
                Math.round(opponentExpectedScore * 10.0) / 10.0);
    }

    List<LineAnalysis> computeLineAnalysis(Lineup candidate, Map<String, Double> opponentUtrByPosition) {
        List<LineAnalysis> result = new ArrayList<>();
        for (Pair pair : candidate.getPairs()) {
            String position = pair.getPosition();
            double p1Actual = pair.getPlayer1ActualUtr() != null ? pair.getPlayer1ActualUtr() : (pair.getPlayer1Utr() != null ? pair.getPlayer1Utr() : 0);
            double p2Actual = pair.getPlayer2ActualUtr() != null ? pair.getPlayer2ActualUtr() : (pair.getPlayer2Utr() != null ? pair.getPlayer2Utr() : 0);
            double ownUtr = p1Actual + p2Actual;
            double oppUtr = opponentUtrByPosition.getOrDefault(position, 0.0);
            double delta = ownUtr - oppUtr;
            double winProb = winProbability(delta);
            String label = winLabel(winProb);
            result.add(new LineAnalysis(position, ownUtr, oppUtr,
                    Math.round(delta * 100.0) / 100.0, winProb, label));
        }
        return result;
    }

    double winProbability(double delta) {
        if (delta > 1.0) return 0.8;
        if (delta > 0.5) return 0.6;
        if (delta >= -0.5) return 0.5;
        if (delta >= -1.0) return 0.4;
        return 0.2;
    }

    String winLabel(double winProb) {
        if (winProb == 0.8) return "80% 赢";
        if (winProb == 0.6) return "60% 赢";
        if (winProb == 0.5) return "对等";
        if (winProb == 0.4) return "60% 输";
        return "80% 输";
    }

    private AiRecommendation computeAiRecommendation(List<Lineup> candidates, Lineup opponentLineup,
                                                      OpponentAnalysisRequest request,
                                                      UtrRecommendation utrFallback) {
        String strategy = "custom".equals(request.getStrategyType())
                ? (request.getNaturalLanguage() != null ? request.getNaturalLanguage() : "均衡")
                : (request.getStrategyType() != null ? request.getStrategyType() : "均衡");

        // Limit to top 5 by totalUtr to keep the prompt size manageable
        List<Lineup> aiCandidates = candidates.stream()
                .sorted((a, b) -> Double.compare(b.getTotalUtr(), a.getTotalUtr()))
                .limit(5)
                .toList();
        int aiIndex = aiService.selectBestLineupWithOpponent(aiCandidates, strategy, opponentLineup);
        if (aiIndex >= 0 && aiIndex < aiCandidates.size()) {
            Lineup aiLineup = aiCandidates.get(aiIndex);
            Map<String, Double> oppUtrByPos = opponentLineup.getPairs().stream()
                    .collect(Collectors.toMap(Pair::getPosition, Pair::getCombinedUtr));
            List<LineAnalysis> aiLineAnalysis = computeLineAnalysis(aiLineup, oppUtrByPos);
            double aiExpected = aiLineAnalysis.stream()
                    .mapToDouble(a -> POSITION_POINTS.getOrDefault(a.getPosition(), 0) * a.getWinProbability())
                    .sum();
            double totalPoints = POSITION_POINTS.values().stream().mapToInt(Integer::intValue).sum();
            return new AiRecommendation(aiLineup, opponentLineup, aiLineAnalysis,
                    Math.round(aiExpected * 10.0) / 10.0,
                    Math.round((totalPoints - aiExpected) * 10.0) / 10.0,
                    "AI 根据对手排阵选择最优方案", true);
        }

        return new AiRecommendation(utrFallback.getLineup(), opponentLineup,
                utrFallback.getLineAnalysis(), utrFallback.getExpectedScore(),
                utrFallback.getOpponentExpectedScore(), "AI 不可用，已用UTR分析代替", false);
    }
}
