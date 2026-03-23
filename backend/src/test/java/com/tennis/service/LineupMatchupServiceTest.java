package com.tennis.service;

import com.tennis.controller.LineupMatchupRequest;
import com.tennis.exception.NotFoundException;
import com.tennis.model.AiRecommendation;
import com.tennis.model.LineAnalysis;
import com.tennis.model.Lineup;
import com.tennis.model.LineupMatchupResponse;
import com.tennis.model.Pair;
import com.tennis.model.Team;
import com.tennis.model.TeamData;
import com.tennis.repository.JsonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LineupMatchupServiceTest {

    @Mock private JsonRepository jsonRepository;
    @Mock private OpponentAnalysisService opponentAnalysisService;
    @Mock private ZhipuAiService aiService;

    @InjectMocks
    private LineupMatchupService service;

    private Team ownTeam;
    private Team opponentTeam;
    private Lineup opponentLineup;
    private TeamData teamData;

    @BeforeEach
    void setUp() {
        opponentLineup = buildLineup("opp-lineup-1", 9.0, 8.5, 8.0, 7.5);
        ownTeam = buildTeamWithLineups("own-team",
                List.of(buildLineup("own-1", 8.0, 8.0, 7.5, 7.0),
                        buildLineup("own-2", 10.0, 9.5, 9.0, 8.5)));
        opponentTeam = buildTeamWithLineups("opp-team", List.of(opponentLineup));

        teamData = new TeamData();
        teamData.setTeams(new ArrayList<>(List.of(ownTeam, opponentTeam)));
    }

    private void stubRepository() {
        when(jsonRepository.readData()).thenReturn(teamData);
    }

    // --- Verdict threshold tests ---

    @Test
    void verdictFor_above6_returnsWin() {
        assertThat(service.verdictFor(6.1)).isEqualTo("能赢");
        assertThat(service.verdictFor(10.0)).isEqualTo("能赢");
    }

    @Test
    void verdictFor_between4and6_returnsClose() {
        assertThat(service.verdictFor(4.0)).isEqualTo("势均力敌");
        assertThat(service.verdictFor(6.0)).isEqualTo("势均力敌");
        assertThat(service.verdictFor(5.0)).isEqualTo("势均力敌");
    }

    @Test
    void verdictFor_below4_returnsLose() {
        assertThat(service.verdictFor(3.9)).isEqualTo("劣势");
        assertThat(service.verdictFor(0.0)).isEqualTo("劣势");
    }

    // --- Results sorted by expected score ---

    @Test
    void matchup_resultsSortedByExpectedScoreDescending() {
        stubRepository();
        List<LineAnalysis> lowAnalysis = buildAnalysis(0.4, 0.4, 0.4, 0.4);
        List<LineAnalysis> highAnalysis = buildAnalysis(0.8, 0.8, 0.8, 0.8);

        when(opponentAnalysisService.computeLineAnalysis(
                argMatchingId("own-1"), anyMap())).thenReturn(lowAnalysis);
        when(opponentAnalysisService.computeLineAnalysis(
                argMatchingId("own-2"), anyMap())).thenReturn(highAnalysis);

        LineupMatchupResponse response = service.matchup(buildRequest("own-team", "opp-team", "opp-lineup-1"));

        assertThat(response.getResults()).hasSize(2);
        assertThat(response.getResults().get(0).getExpectedScore())
                .isGreaterThan(response.getResults().get(1).getExpectedScore());
    }

    // --- ownLineupId filter (task 3.1) ---

    @Test
    void matchup_withOwnLineupId_returnsSingleResult() {
        stubRepository();
        when(opponentAnalysisService.computeLineAnalysis(
                argMatchingId("own-2"), anyMap())).thenReturn(buildAnalysis(0.8, 0.8, 0.8, 0.8));

        LineupMatchupRequest req = buildRequest("own-team", "opp-team", "opp-lineup-1");
        req.setOwnLineupId("own-2");
        LineupMatchupResponse response = service.matchup(req);

        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResults().get(0).getLineup().getId()).isEqualTo("own-2");
    }

    @Test
    void matchup_withOwnLineupId_notFound_returnsEmpty() {
        stubRepository();
        LineupMatchupRequest req = buildRequest("own-team", "opp-team", "opp-lineup-1");
        req.setOwnLineupId("nonexistent");
        LineupMatchupResponse response = service.matchup(req);

        assertThat(response.getResults()).isEmpty();
    }

    // --- includeAi (task 3.2) ---

    @Test
    void matchup_includeAi_callsAiServiceAndPopulatesRecommendation() {
        stubRepository();
        List<LineAnalysis> analysis = buildAnalysis(0.8, 0.8, 0.8, 0.8);
        when(opponentAnalysisService.computeLineAnalysis(any(), anyMap())).thenReturn(analysis);
        when(aiService.selectBestWithResult(any(), any(), any()))
                .thenReturn(new ZhipuAiService.AiResult(0, "D1组合UTR优势明显"));

        LineupMatchupRequest req = buildRequest("own-team", "opp-team", "opp-lineup-1");
        req.setIncludeAi(true);
        LineupMatchupResponse response = service.matchup(req);

        assertThat(response.getAiRecommendation()).isNotNull();
        assertThat(response.getAiRecommendation().isAiUsed()).isTrue();
        assertThat(response.getAiRecommendation().getExplanation()).isEqualTo("D1组合UTR优势明显");
        verify(aiService).selectBestWithResult(any(), any(), any());
    }

    // --- includeAi + ownLineupId ignores AI (task 3.3) ---

    @Test
    void matchup_includeAiWithOwnLineupId_aiNotCalled_aiRecNull() {
        stubRepository();
        when(opponentAnalysisService.computeLineAnalysis(
                argMatchingId("own-1"), anyMap())).thenReturn(buildAnalysis(0.5, 0.5, 0.5, 0.5));

        LineupMatchupRequest req = buildRequest("own-team", "opp-team", "opp-lineup-1");
        req.setOwnLineupId("own-1");
        req.setIncludeAi(true);
        LineupMatchupResponse response = service.matchup(req);

        assertThat(response.getAiRecommendation()).isNull();
        verify(aiService, never()).selectBestWithResult(any(), any(), any());
    }

    // --- Not found errors ---

    @Test
    void matchup_ownTeamNotFound_throws404() {
        stubRepository();
        assertThatThrownBy(() -> service.matchup(buildRequest("nonexistent", "opp-team", "opp-lineup-1")))
                .isInstanceOf(NotFoundException.class).hasMessageContaining("队伍不存在");
    }

    @Test
    void matchup_opponentTeamNotFound_throws404() {
        stubRepository();
        assertThatThrownBy(() -> service.matchup(buildRequest("own-team", "nonexistent", "opp-lineup-1")))
                .isInstanceOf(NotFoundException.class).hasMessageContaining("对手队伍不存在");
    }

    @Test
    void matchup_opponentLineupNotFound_throws404() {
        stubRepository();
        assertThatThrownBy(() -> service.matchup(buildRequest("own-team", "opp-team", "nonexistent-lineup")))
                .isInstanceOf(NotFoundException.class).hasMessageContaining("对手排阵不存在");
    }

    @Test
    void matchup_ownTeamNoLineups_returnsEmptyResults() {
        stubRepository();
        ownTeam.setLineups(new ArrayList<>());
        LineupMatchupResponse response = service.matchup(buildRequest("own-team", "opp-team", "opp-lineup-1"));
        assertThat(response.getResults()).isEmpty();
    }

    // --- Helpers ---

    private LineupMatchupRequest buildRequest(String teamId, String opponentTeamId, String opponentLineupId) {
        LineupMatchupRequest req = new LineupMatchupRequest();
        req.setTeamId(teamId);
        req.setOpponentTeamId(opponentTeamId);
        req.setOpponentLineupId(opponentLineupId);
        return req;
    }

    private Lineup buildLineup(String id, double d1, double d2, double d3, double d4) {
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

    private Team buildTeamWithLineups(String id, List<Lineup> lineups) {
        Team team = new Team();
        team.setId(id);
        team.setName("Team " + id);
        team.setPlayers(new ArrayList<>());
        team.setLineups(new ArrayList<>(lineups));
        return team;
    }

    private List<LineAnalysis> buildAnalysis(double d1prob, double d2prob, double d3prob, double d4prob) {
        return List.of(
                new LineAnalysis("D1", 9.0, 9.0, 0.0, d1prob, "对等"),
                new LineAnalysis("D2", 8.5, 8.5, 0.0, d2prob, "对等"),
                new LineAnalysis("D3", 8.0, 8.0, 0.0, d3prob, "对等"),
                new LineAnalysis("D4", 7.5, 7.5, 0.0, d4prob, "对等")
        );
    }

    private Lineup argMatchingId(String id) {
        return org.mockito.ArgumentMatchers.argThat(l -> l != null && id.equals(l.getId()));
    }
}
