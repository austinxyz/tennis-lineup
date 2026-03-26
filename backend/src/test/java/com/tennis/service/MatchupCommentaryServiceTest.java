package com.tennis.service;

import com.tennis.controller.MatchupCommentaryRequest;
import com.tennis.controller.MatchupCommentaryResponse;
import com.tennis.exception.NotFoundException;
import com.tennis.model.Lineup;
import com.tennis.model.Pair;
import com.tennis.model.Team;
import com.tennis.model.TeamData;
import com.tennis.repository.JsonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MatchupCommentaryService Test")
class MatchupCommentaryServiceTest {

    @Mock
    private JsonRepository jsonRepository;

    @Mock
    private ZhipuAiService aiService;

    @InjectMocks
    private MatchupCommentaryService service;

    private TeamData teamData;
    private Team ownTeam;
    private Team oppTeam;
    private Lineup ownLineup;
    private Lineup oppLineup;

    @BeforeEach
    void setUp() {
        ownLineup = buildLineup("lineup-own");
        oppLineup = buildLineup("lineup-opp");

        ownTeam = new Team();
        ownTeam.setId("team-own");
        ownTeam.setPlayers(new ArrayList<>());
        ownTeam.setLineups(new ArrayList<>(List.of(ownLineup)));

        oppTeam = new Team();
        oppTeam.setId("team-opp");
        oppTeam.setPlayers(new ArrayList<>());
        oppTeam.setLineups(new ArrayList<>(List.of(oppLineup)));

        teamData = new TeamData();
        teamData.getTeams().add(ownTeam);
        teamData.getTeams().add(oppTeam);
    }

    private Lineup buildLineup(String id) {
        Lineup lineup = new Lineup();
        lineup.setId(id);
        lineup.setPairs(new ArrayList<>());
        for (String pos : List.of("D1", "D2", "D3", "D4")) {
            Pair pair = new Pair();
            pair.setPosition(pos);
            pair.setPlayer1Name("甲" + pos);
            pair.setPlayer1Utr(6.0);
            pair.setPlayer2Name("乙" + pos);
            pair.setPlayer2Utr(5.0);
            pair.setCombinedUtr(11.0);
            lineup.getPairs().add(pair);
        }
        lineup.setTotalUtr(44.0);
        return lineup;
    }

    private MatchupCommentaryRequest buildRequest() {
        MatchupCommentaryRequest req = new MatchupCommentaryRequest();
        req.setTeamId("team-own");
        req.setOwnLineupId("lineup-own");
        req.setOpponentTeamId("team-opp");
        req.setOpponentLineupId("lineup-opp");
        return req;
    }

    @Test
    @DisplayName("AI 成功时返回 AI 评析文字")
    void shouldReturnAiCommentaryWhenAiSucceeds() {
        when(jsonRepository.readData()).thenReturn(teamData);
        when(aiService.getCommentary(any(), any(), any(), any())).thenReturn(Map.of(
                "D1", "己方UTR优势明显，建议主动进攻",
                "D2", "势均力敌，发挥稳定",
                "D3", "己方略占优势",
                "D4", "处于劣势，注重防守"
        ));

        MatchupCommentaryResponse result = service.getCommentary(buildRequest());

        assertTrue(result.isAiUsed());
        assertEquals(4, result.getLines().size());
        assertEquals("D1", result.getLines().get(0).getPosition());
        assertEquals("己方UTR优势明显，建议主动进攻", result.getLines().get(0).getCommentary());
    }

    @Test
    @DisplayName("AI 不可用时按 delta 规则兜底")
    void shouldUseFallbackCommentaryWhenAiUnavailable() {
        // Modify ownLineup D1 to have higher UTR (delta > 0.5)
        ownLineup.getPairs().get(0).setCombinedUtr(13.0); // D1: 13 vs 11 => delta +2
        // D2: equal => delta 0
        // D3: equal => delta 0
        // Modify ownLineup D4 to be lower (delta < -0.5)
        ownLineup.getPairs().get(3).setCombinedUtr(9.0); // D4: 9 vs 11 => delta -2

        when(jsonRepository.readData()).thenReturn(teamData);
        when(aiService.getCommentary(any(), any(), any(), any())).thenReturn(Map.of());

        MatchupCommentaryResponse result = service.getCommentary(buildRequest());

        assertFalse(result.isAiUsed());
        assertEquals(4, result.getLines().size());

        MatchupCommentaryResponse.LineCommentary d1 = result.getLines().stream()
                .filter(l -> "D1".equals(l.getPosition())).findFirst().orElseThrow();
        assertEquals("具备优势，建议主动进攻", d1.getCommentary());

        MatchupCommentaryResponse.LineCommentary d4 = result.getLines().stream()
                .filter(l -> "D4".equals(l.getPosition())).findFirst().orElseThrow();
        assertEquals("处于劣势，多以防守反击为主", d4.getCommentary());

        MatchupCommentaryResponse.LineCommentary d2 = result.getLines().stream()
                .filter(l -> "D2".equals(l.getPosition())).findFirst().orElseThrow();
        assertEquals("势均力敌，注重稳定发挥", d2.getCommentary());
    }

    @Test
    @DisplayName("排阵不存在时抛出 NotFoundException")
    void shouldThrowNotFoundWhenLineupMissing() {
        when(jsonRepository.readData()).thenReturn(teamData);

        MatchupCommentaryRequest req = buildRequest();
        req.setOwnLineupId("non-existent-lineup");

        assertThrows(NotFoundException.class, () -> service.getCommentary(req));
    }

    @Test
    @DisplayName("队伍不存在时抛出 NotFoundException")
    void shouldThrowNotFoundWhenTeamMissing() {
        when(jsonRepository.readData()).thenReturn(teamData);

        MatchupCommentaryRequest req = buildRequest();
        req.setTeamId("non-existent-team");

        assertThrows(NotFoundException.class, () -> service.getCommentary(req));
    }
}
