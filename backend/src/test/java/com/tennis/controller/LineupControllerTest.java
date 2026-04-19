package com.tennis.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tennis.exception.NotFoundException;
import com.tennis.model.Lineup;
import com.tennis.model.Pair;
import com.tennis.service.LineupMatchupService;
import com.tennis.service.LineupService;
import com.tennis.service.MatchupCommentaryService;
import com.tennis.service.OpponentAnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.mock.web.MockMultipartFile;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(LineupController.class)
@DisplayName("LineupController Test")
class LineupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LineupService lineupService;

    @MockBean
    private OpponentAnalysisService opponentAnalysisService;

    @MockBean
    private LineupMatchupService lineupMatchupService;

    @MockBean
    private MatchupCommentaryService matchupCommentaryService;

    @Autowired
    private ObjectMapper objectMapper;

    private Lineup testLineup;

    @BeforeEach
    void setUp() {
        testLineup = new Lineup();
        testLineup.setId("lineup-001");
        testLineup.setCreatedAt(Instant.parse("2026-03-17T10:00:00Z"));
        testLineup.setStrategy("balanced");
        testLineup.setAiUsed(false);
        testLineup.setValid(true);
        testLineup.setTotalUtr(30.0);
        testLineup.setViolationMessages(new ArrayList<>());

        Pair d1 = new Pair("D1", "p1", "Alice", "p2", "Bob", 15.5, null, null, null, null, null, null, null, null, null);
        Pair d2 = new Pair("D2", "p3", "Carol", "p4", "Dave", 13.5, null, null, null, null, null, null, null, null, null);
        Pair d3 = new Pair("D3", "p5", "Eve", "p6", "Frank", 11.5, null, null, null, null, null, null, null, null, null);
        Pair d4 = new Pair("D4", "p7", "Grace", "p8", "Hank", 9.5, null, null, null, null, null, null, null, null, null);
        testLineup.setPairs(Arrays.asList(d1, d2, d3, d4));
    }

    @Test
    @DisplayName("POST /api/lineups/generate returns 200 with array of lineups")
    void testGenerateLineupSuccess() throws Exception {
        when(lineupService.generateMultiple(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(testLineup));

        mockMvc.perform(post("/api/lineups/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("teamId", "team-1", "strategyType", "preset", "preset", "balanced"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("lineup-001"))
                .andExpect(jsonPath("$[0].strategy").value("balanced"))
                .andExpect(jsonPath("$[0].pairs.length()").value(4));
    }

    @Test
    @DisplayName("POST /api/lineups/generate returns multiple candidates")
    void testGenerateLineupReturnsMultiple() throws Exception {
        Lineup second = new Lineup();
        second.setId("lineup-002");
        second.setCreatedAt(Instant.now());
        second.setStrategy("balanced");
        second.setAiUsed(false);
        second.setValid(true);
        second.setTotalUtr(28.0);
        second.setViolationMessages(new ArrayList<>());
        second.setPairs(testLineup.getPairs());

        when(lineupService.generateMultiple(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(testLineup, second));

        mockMvc.perform(post("/api/lineups/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("teamId", "team-1", "strategyType", "preset", "preset", "balanced"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("POST /api/lineups/generate returns 400 when fewer than 8 players")
    void testGenerateLineupInsufficientPlayers() throws Exception {
        when(lineupService.generateMultiple(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("队伍球员不足8人，无法生成排阵"));

        mockMvc.perform(post("/api/lineups/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("teamId", "team-small", "strategyType", "preset", "preset", "balanced"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("队伍球员不足8人，无法生成排阵"));
    }

    @Test
    @DisplayName("POST /api/lineups/generate returns 400 for constraint violation")
    void testGenerateLineupConstraintViolation() throws Exception {
        when(lineupService.generateMultiple(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("排除球员后可用球员不足8人"));

        mockMvc.perform(post("/api/lineups/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("teamId", "team-1", "excludePlayers", List.of("p1", "p2", "p3")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("排除球员后可用球员不足8人"));
    }

    @Test
    @DisplayName("POST /api/lineups/generate returns 404 when team not found")
    void testGenerateLineupTeamNotFound() throws Exception {
        when(lineupService.generateMultiple(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new NotFoundException("队伍不存在"));

        mockMvc.perform(post("/api/lineups/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("teamId", "unknown", "strategyType", "preset", "preset", "balanced"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/lineups/generate passes pinPlayers to service")
    void testGenerateLineupWithPinPlayers() throws Exception {
        when(lineupService.generateMultiple(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(testLineup));

        String body = "{\"teamId\":\"team-1\",\"strategyType\":\"preset\",\"preset\":\"balanced\","
                + "\"pinPlayers\":{\"p1\":\"D1\"}}";

        mockMvc.perform(post("/api/lineups/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("lineup-001"));
    }

    @Test
    @DisplayName("POST /api/lineups/generate passes includePlayers to service")
    void testGenerateLineupWithIncludePlayers() throws Exception {
        when(lineupService.generateMultiple(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(testLineup));

        String body = "{\"teamId\":\"team-1\",\"strategyType\":\"preset\",\"preset\":\"balanced\","
                + "\"includePlayers\":[\"p1\",\"p2\"]}";

        mockMvc.perform(post("/api/lineups/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("lineup-001"));
    }

    @Test
    @DisplayName("Response pair includes player1Gender and player2Gender when set")
    void testResponseIncludesGenderFields() throws Exception {
        testLineup.getPairs().get(0).setPlayer1Gender("male");
        testLineup.getPairs().get(0).setPlayer2Gender("female");
        when(lineupService.generateMultiple(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(testLineup));

        mockMvc.perform(post("/api/lineups/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"teamId\":\"team-1\",\"strategyType\":\"preset\",\"preset\":\"balanced\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pairs[0].player1Gender").value("male"))
                .andExpect(jsonPath("$[0].pairs[0].player2Gender").value("female"));
    }

    @Test
    @DisplayName("POST /api/teams/{teamId}/lineups saves lineup and returns 200")
    void testSaveLineupSuccess() throws Exception {
        when(lineupService.saveLineup(eq("team-1"), any())).thenReturn(testLineup);

        mockMvc.perform(post("/api/teams/team-1/lineups")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testLineup)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("lineup-001"));
    }

    @Test
    @DisplayName("POST /api/teams/{teamId}/lineups returns 404 for unknown team")
    void testSaveLineupTeamNotFound() throws Exception {
        when(lineupService.saveLineup(eq("unknown"), any()))
                .thenThrow(new NotFoundException("队伍不存在"));

        mockMvc.perform(post("/api/teams/unknown/lineups")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testLineup)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/teams/{id}/lineups returns lineup list in order")
    void testGetLineupHistory() throws Exception {
        when(lineupService.getLineupsByTeam("team-1")).thenReturn(List.of(testLineup));

        mockMvc.perform(get("/api/teams/team-1/lineups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value("lineup-001"));
    }

    @Test
    @DisplayName("GET /api/teams/{id}/lineups returns 404 for unknown team")
    void testGetLineupHistoryTeamNotFound() throws Exception {
        when(lineupService.getLineupsByTeam("unknown"))
                .thenThrow(new NotFoundException("队伍不存在"));

        mockMvc.perform(get("/api/teams/unknown/lineups"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/teams/{id}/lineups returns empty array for team with no lineups")
    void testGetLineupHistoryEmpty() throws Exception {
        when(lineupService.getLineupsByTeam("team-1")).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/api/teams/team-1/lineups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("DELETE /api/lineups/{id} returns 204 on success")
    void testDeleteLineupSuccess() throws Exception {
        doNothing().when(lineupService).deleteLineup("lineup-001");

        mockMvc.perform(delete("/api/lineups/lineup-001"))
                .andExpect(status().isNoContent());

        verify(lineupService).deleteLineup("lineup-001");
    }

    @Test
    @DisplayName("DELETE /api/lineups/{id} returns 404 for non-existent lineup")
    void testDeleteLineupNotFound() throws Exception {
        doThrow(new NotFoundException("排阵不存在")).when(lineupService).deleteLineup("nonexistent");

        mockMvc.perform(delete("/api/lineups/nonexistent"))
                .andExpect(status().isNotFound());
    }

    // --- Export tests ---

    @Test
    @DisplayName("GET /api/teams/{teamId}/lineups/export returns 200 with attachment header")
    void testExportLineupsSuccess() throws Exception {
        Map<String, Object> envelope = Map.of(
                "exportedAt", "2026-04-18T10:00:00Z",
                "teamId", "team-1",
                "teamName", "浙江队",
                "lineups", List.of(testLineup)
        );
        when(lineupService.exportLineups("team-1")).thenReturn(envelope);

        mockMvc.perform(get("/api/teams/team-1/lineups/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(jsonPath("$.teamId").value("team-1"))
                .andExpect(jsonPath("$.teamName").value("浙江队"))
                .andExpect(jsonPath("$.lineups").isArray());
    }

    @Test
    @DisplayName("GET /api/teams/{teamId}/lineups/export returns 404 for unknown team")
    void testExportLineupsTeamNotFound() throws Exception {
        when(lineupService.exportLineups("unknown")).thenThrow(new NotFoundException("队伍不存在"));

        mockMvc.perform(get("/api/teams/unknown/lineups/export"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/teams/{teamId}/lineups/export returns empty lineups array when no lineups")
    void testExportLineupsEmpty() throws Exception {
        Map<String, Object> envelope = Map.of(
                "exportedAt", "2026-04-18T10:00:00Z",
                "teamId", "team-1",
                "teamName", "浙江队",
                "lineups", List.of()
        );
        when(lineupService.exportLineups("team-1")).thenReturn(envelope);

        mockMvc.perform(get("/api/teams/team-1/lineups/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineups").isArray())
                .andExpect(jsonPath("$.lineups.length()").value(0));
    }

    // --- Import tests ---

    @Test
    @DisplayName("POST /api/teams/{teamId}/lineups/import returns imported and skipped counts")
    void testImportLineupsSuccess() throws Exception {
        when(lineupService.importLineups(eq("team-1"), anyList()))
                .thenReturn(Map.of("imported", 2, "skipped", 0));

        String json = "{\"lineups\":[" + objectMapper.writeValueAsString(testLineup) + "]}";
        MockMultipartFile file = new MockMultipartFile("file", "lineups.json",
                "application/json", json.getBytes());

        mockMvc.perform(multipart("/api/teams/team-1/lineups/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(2))
                .andExpect(jsonPath("$.skipped").value(0));
    }

    @Test
    @DisplayName("POST /api/teams/{teamId}/lineups/import skips duplicate lineups by player name")
    void testImportLineupsDuplicatesSkipped() throws Exception {
        when(lineupService.importLineups(eq("team-1"), anyList()))
                .thenReturn(Map.of("imported", 0, "skipped", 1));

        String json = "{\"lineups\":[" + objectMapper.writeValueAsString(testLineup) + "]}";
        MockMultipartFile file = new MockMultipartFile("file", "lineups.json",
                "application/json", json.getBytes());

        mockMvc.perform(multipart("/api/teams/team-1/lineups/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(0))
                .andExpect(jsonPath("$.skipped").value(1));
    }

    @Test
    @DisplayName("POST /api/teams/{teamId}/lineups/import returns 404 for unknown team")
    void testImportLineupsTeamNotFound() throws Exception {
        when(lineupService.importLineups(eq("unknown"), anyList()))
                .thenThrow(new NotFoundException("队伍不存在"));

        String json = "{\"lineups\":[]}";
        MockMultipartFile file = new MockMultipartFile("file", "lineups.json",
                "application/json", json.getBytes());

        mockMvc.perform(multipart("/api/teams/unknown/lineups/import").file(file))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/teams/{teamId}/lineups/import returns 400 for invalid JSON")
    void testImportLineupsInvalidJson() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "lineups.json",
                "application/json", "not-valid-json".getBytes());

        mockMvc.perform(multipart("/api/teams/team-1/lineups/import").file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/teams/{teamId}/lineups/import returns 400 when lineups field missing")
    void testImportLineupsMissingLineupsField() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "lineups.json",
                "application/json", "{\"teamId\":\"team-1\"}".getBytes());

        mockMvc.perform(multipart("/api/teams/team-1/lineups/import").file(file))
                .andExpect(status().isBadRequest());
    }

    // ======================== PATCH /api/teams/{teamId}/lineups/{lineupId} tests ========================

    @Test
    @DisplayName("PATCH /api/teams/{teamId}/lineups/{lineupId} returns 200 with updated lineup")
    void testPatchLineupSuccess() throws Exception {
        testLineup.setLabel("My Label");
        when(lineupService.updateLineup(eq("team-1"), eq("lineup-001"), any()))
                .thenReturn(testLineup);

        String body = "{\"label\":\"My Label\"}";

        mockMvc.perform(patch("/api/teams/team-1/lineups/lineup-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("lineup-001"))
                .andExpect(jsonPath("$.label").value("My Label"));
    }

    @Test
    @DisplayName("PATCH /api/teams/{teamId}/lineups/{lineupId} returns 404 when lineup not found")
    void testPatchLineupNotFound() throws Exception {
        when(lineupService.updateLineup(eq("team-1"), eq("nonexistent"), any()))
                .thenThrow(new NotFoundException("排阵不存在"));

        mockMvc.perform(patch("/api/teams/team-1/lineups/nonexistent")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"label\":\"x\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /api/teams/{teamId}/lineups/{lineupId} returns 400 on duplicate pairs")
    void testPatchLineupDuplicatePairs() throws Exception {
        when(lineupService.updateLineup(eq("team-1"), eq("lineup-001"), any()))
                .thenThrow(new IllegalArgumentException("该排阵已保存，请勿重复保存"));

        String body = "{\"pairs\":[{\"position\":\"D1\",\"player1Id\":\"p1\",\"player2Id\":\"p2\"}]}";

        mockMvc.perform(patch("/api/teams/team-1/lineups/lineup-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("该排阵已保存，请勿重复保存"));
    }

    @Test
    @DisplayName("PATCH /api/teams/{teamId}/lineups/{lineupId} returns 400 for malformed JSON")
    void testPatchLineupBadRequest() throws Exception {
        mockMvc.perform(patch("/api/teams/team-1/lineups/lineup-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("not-json"))
                .andExpect(status().isBadRequest());
    }
}
