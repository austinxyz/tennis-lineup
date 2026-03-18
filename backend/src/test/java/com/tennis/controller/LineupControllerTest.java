package com.tennis.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tennis.exception.NotFoundException;
import com.tennis.model.Lineup;
import com.tennis.model.Pair;
import com.tennis.service.LineupService;
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

import static org.mockito.ArgumentMatchers.any;
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

        Pair d1 = new Pair("D1", "p1", "Alice", "p2", "Bob", 15.5);
        Pair d2 = new Pair("D2", "p3", "Carol", "p4", "Dave", 13.5);
        Pair d3 = new Pair("D3", "p5", "Eve", "p6", "Frank", 11.5);
        Pair d4 = new Pair("D4", "p7", "Grace", "p8", "Hank", 9.5);
        testLineup.setPairs(Arrays.asList(d1, d2, d3, d4));
    }

    @Test
    @DisplayName("POST /api/lineups/generate returns 200 with lineup")
    void testGenerateLineupSuccess() throws Exception {
        when(lineupService.generateAndSave(eq("team-1"), eq("preset"), eq("balanced"), any()))
                .thenReturn(testLineup);

        mockMvc.perform(post("/api/lineups/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("teamId", "team-1", "strategyType", "preset", "preset", "balanced"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("lineup-001"))
                .andExpect(jsonPath("$.strategy").value("balanced"))
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.pairs").isArray())
                .andExpect(jsonPath("$.pairs.length()").value(4));
    }

    @Test
    @DisplayName("POST /api/lineups/generate returns 400 when fewer than 8 players")
    void testGenerateLineupInsufficientPlayers() throws Exception {
        when(lineupService.generateAndSave(any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("队伍球员不足8人，无法生成排阵"));

        mockMvc.perform(post("/api/lineups/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("teamId", "team-small", "strategyType", "preset", "preset", "balanced"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("队伍球员不足8人，无法生成排阵"));
    }

    @Test
    @DisplayName("POST /api/lineups/generate returns 404 when team not found")
    void testGenerateLineupTeamNotFound() throws Exception {
        when(lineupService.generateAndSave(any(), any(), any(), any()))
                .thenThrow(new NotFoundException("队伍不存在"));

        mockMvc.perform(post("/api/lineups/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("teamId", "unknown", "strategyType", "preset", "preset", "balanced"))))
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

    @Test
    @DisplayName("POST /api/lineups/generate with aiUsed=false when AI unavailable")
    void testGenerateLineupAiUnavailable() throws Exception {
        testLineup.setAiUsed(false);
        when(lineupService.generateAndSave(any(), any(), any(), any())).thenReturn(testLineup);

        mockMvc.perform(post("/api/lineups/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("teamId", "team-1", "strategyType", "preset", "preset", "aggressive"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aiUsed").value(false));
    }
}
