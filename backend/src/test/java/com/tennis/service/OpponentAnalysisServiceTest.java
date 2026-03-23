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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpponentAnalysisServiceTest {

    @Mock
    private JsonRepository jsonRepository;

    @Mock
    private LineupGenerationService generationService;

    @Mock
    private ZhipuAiService aiService;

    @InjectMocks
    private OpponentAnalysisService service;

    private Team ownTeam;
    private Team opponentTeam;
    private Lineup opponentLineup;
    private TeamData teamData;

    @BeforeEach
    void setUp() {
        ownTeam = buildTeam("own-team", buildPlayers(8));
        opponentLineup = buildLineupWith4Pairs("opp-lineup-1",
                9.0, 8.5, 8.0, 7.5);
        opponentTeam = buildTeamWithLineup("opp-team", buildPlayers(8), opponentLineup);

        teamData = new TeamData();
        teamData.setTeams(new ArrayList<>(List.of(ownTeam, opponentTeam)));

        lenient().when(jsonRepository.readData()).thenReturn(teamData);
        lenient().when(aiService.selectBestLineupWithOpponent(anyList(), anyString(), any())).thenReturn(-1);
    }

    // --- Win probability threshold tests ---

    @Test
    void winProbability_deltaAbove1_returns80() {
        assertThat(service.winProbability(1.1)).isEqualTo(0.8);
        assertThat(service.winProbability(2.0)).isEqualTo(0.8);
    }

    @Test
    void winProbability_deltaAboveHalf_returns60() {
        assertThat(service.winProbability(0.51)).isEqualTo(0.6);
        assertThat(service.winProbability(1.0)).isEqualTo(0.6);
    }

    @Test
    void winProbability_deltaNearZero_returns50() {
        assertThat(service.winProbability(0.0)).isEqualTo(0.5);
        assertThat(service.winProbability(0.5)).isEqualTo(0.5);
        assertThat(service.winProbability(-0.5)).isEqualTo(0.5);
    }

    @Test
    void winProbability_deltaBelowHalf_returns40() {
        assertThat(service.winProbability(-0.51)).isEqualTo(0.4);
        assertThat(service.winProbability(-1.0)).isEqualTo(0.4);
    }

    @Test
    void winProbability_deltaBelow1_returns20() {
        assertThat(service.winProbability(-1.01)).isEqualTo(0.2);
        assertThat(service.winProbability(-2.0)).isEqualTo(0.2);
    }

    // --- Expected score calculation ---

    @Test
    void computeUtrRecommendation_expectedScoreCalculatedCorrectly() {
        // own D1=9.5 vs opp D1=9.0 → delta=0.5 → 50% → 0.5*1pt = 0.5
        // own D2=8.5 vs opp D2=8.5 → delta=0.0 → 50% → 0.5*2pt = 1.0
        // own D3=8.0 vs opp D3=8.0 → delta=0.0 → 50% → 0.5*3pt = 1.5
        // own D4=7.5 vs opp D4=7.5 → delta=0.0 → 50% → 0.5*4pt = 2.0
        // expectedScore = 5.0
        Lineup candidate = buildLineupWith4Pairs("cand-1", 9.5, 8.5, 8.0, 7.5);
        Lineup opponent = buildLineupWith4Pairs("opp-1", 9.0, 8.5, 8.0, 7.5);

        UtrRecommendation result = service.computeUtrRecommendation(List.of(candidate), opponent);

        assertThat(result.getLineup()).isEqualTo(candidate);
        assertThat(result.getExpectedScore()).isEqualTo(5.0);
        assertThat(result.getOpponentExpectedScore()).isEqualTo(5.0);
    }

    // --- Not found errors ---

    @Test
    void analyze_ownTeamNotFound_throws404() {
        OpponentAnalysisRequest req = buildRequest("nonexistent", "opp-team", "opp-lineup-1");
        assertThatThrownBy(() -> service.analyze(req))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("队伍不存在");
    }

    @Test
    void analyze_opponentTeamNotFound_throws404() {
        OpponentAnalysisRequest req = buildRequest("own-team", "nonexistent", "opp-lineup-1");
        assertThatThrownBy(() -> service.analyze(req))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("对手队伍不存在");
    }

    @Test
    void analyze_opponentLineupNotFound_throws404() {
        OpponentAnalysisRequest req = buildRequest("own-team", "opp-team", "nonexistent-lineup");
        assertThatThrownBy(() -> service.analyze(req))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("对手排阵不存在");
    }

    // --- AI fallback ---

    @Test
    void analyze_aiFails_fallsBackToUtrRecommendation() {
        Lineup candidate = buildLineupWith4Pairs("c1", 9.0, 8.5, 8.0, 7.5);
        when(generationService.generateCandidates(any(), any(), any(), any()))
                .thenReturn(List.of(candidate));
        when(aiService.selectBestLineupWithOpponent(anyList(), anyString(), any())).thenReturn(-1);

        OpponentAnalysisRequest req = buildRequest("own-team", "opp-team", "opp-lineup-1");
        req.setIncludeAi(true);
        OpponentAnalysisResponse response = service.analyze(req);

        AiRecommendation aiRec = response.getAiRecommendation();
        assertThat(aiRec.isAiUsed()).isFalse();
        assertThat(aiRec.getExplanation()).contains("AI 不可用");
        assertThat(aiRec.getLineup()).isEqualTo(response.getUtrRecommendation().getLineup());
    }

    // --- Helpers ---

    private OpponentAnalysisRequest buildRequest(String teamId, String opponentTeamId, String opponentLineupId) {
        OpponentAnalysisRequest req = new OpponentAnalysisRequest();
        req.setTeamId(teamId);
        req.setOpponentTeamId(opponentTeamId);
        req.setOpponentLineupId(opponentLineupId);
        req.setStrategyType("preset");
        return req;
    }

    private Lineup buildLineupWith4Pairs(String id, double d1, double d2, double d3, double d4) {
        List<Pair> pairs = new ArrayList<>();
        pairs.add(pairAt("D1", d1));
        pairs.add(pairAt("D2", d2));
        pairs.add(pairAt("D3", d3));
        pairs.add(pairAt("D4", d4));
        Lineup lineup = new Lineup();
        lineup.setId(id);
        lineup.setPairs(pairs);
        lineup.setTotalUtr(d1 + d2 + d3 + d4);
        lineup.setViolationMessages(new ArrayList<>());
        return lineup;
    }

    private Pair pairAt(String position, double combinedUtr) {
        Pair pair = new Pair();
        pair.setPosition(position);
        pair.setCombinedUtr(combinedUtr);
        pair.setPlayer1Id("p1-" + position);
        pair.setPlayer2Id("p2-" + position);
        pair.setPlayer1Name("P1 " + position);
        pair.setPlayer2Name("P2 " + position);
        pair.setPlayer1Utr(combinedUtr / 2);
        pair.setPlayer2Utr(combinedUtr / 2);
        return pair;
    }

    private List<Player> buildPlayers(int count) {
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Player p = new Player();
            p.setId("player-" + i);
            p.setName("Player " + i);
            p.setGender(i % 2 == 0 ? "female" : "male");
            p.setUtr(5.0 - i * 0.2);
            p.setVerified(true);
            players.add(p);
        }
        return players;
    }

    private Team buildTeam(String id, List<Player> players) {
        Team team = new Team();
        team.setId(id);
        team.setName("Team " + id);
        team.setPlayers(new ArrayList<>(players));
        team.setLineups(new ArrayList<>());
        return team;
    }

    private Team buildTeamWithLineup(String id, List<Player> players, Lineup lineup) {
        Team team = buildTeam(id, players);
        team.setLineups(new ArrayList<>(List.of(lineup)));
        return team;
    }
}
