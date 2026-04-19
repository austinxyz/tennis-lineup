package com.tennis.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tennis.model.Lineup;
import com.tennis.model.LineupMatchupResponse;
import com.tennis.model.OpponentAnalysisResponse;
import com.tennis.service.LineupMatchupService;
import com.tennis.service.LineupService;
import com.tennis.service.MatchupCommentaryService;
import com.tennis.service.OpponentAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
public class LineupController {

    private final LineupService lineupService;
    private final OpponentAnalysisService opponentAnalysisService;
    private final LineupMatchupService lineupMatchupService;
    private final MatchupCommentaryService matchupCommentaryService;
    private final ObjectMapper objectMapper;

    @Autowired
    public LineupController(LineupService lineupService,
                            OpponentAnalysisService opponentAnalysisService,
                            LineupMatchupService lineupMatchupService,
                            MatchupCommentaryService matchupCommentaryService,
                            ObjectMapper objectMapper) {
        this.lineupService = lineupService;
        this.opponentAnalysisService = opponentAnalysisService;
        this.lineupMatchupService = lineupMatchupService;
        this.matchupCommentaryService = matchupCommentaryService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/api/lineups/generate")
    public ResponseEntity<List<Lineup>> generateLineup(@RequestBody GenerateLineupRequest request) {
        List<Lineup> lineups = lineupService.generateMultiple(
                request.getTeamId(),
                request.getStrategyType(),
                request.getPreset(),
                request.getNaturalLanguage(),
                request.getIncludePlayers(),
                request.getExcludePlayers(),
                request.getPinPlayers()
        );
        return ResponseEntity.ok(lineups);
    }

    @PostMapping("/api/lineups/analyze-opponent")
    public ResponseEntity<OpponentAnalysisResponse> analyzeOpponent(@RequestBody OpponentAnalysisRequest request) {
        OpponentAnalysisResponse response = opponentAnalysisService.analyze(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/lineups/matchup")
    public ResponseEntity<LineupMatchupResponse> matchup(@RequestBody LineupMatchupRequest request) {
        LineupMatchupResponse response = lineupMatchupService.matchup(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/lineups/matchup-commentary")
    public ResponseEntity<MatchupCommentaryResponse> matchupCommentary(@RequestBody MatchupCommentaryRequest request) {
        MatchupCommentaryResponse response = matchupCommentaryService.getCommentary(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/teams/{teamId}/lineups")
    public ResponseEntity<Lineup> saveLineup(@PathVariable String teamId,
                                             @RequestBody Lineup lineup) {
        Lineup saved = lineupService.saveLineup(teamId, lineup);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/api/teams/{id}/lineups")
    public ResponseEntity<List<Lineup>> getLineupHistory(@PathVariable String id) {
        List<Lineup> lineups = lineupService.getLineupsByTeam(id);
        return ResponseEntity.ok(lineups);
    }

    @GetMapping("/api/teams/{teamId}/lineups/export")
    public ResponseEntity<Map<String, Object>> exportLineups(@PathVariable String teamId) {
        Map<String, Object> envelope = lineupService.exportLineups(teamId);
        String teamName = envelope.get("teamName").toString().replaceAll("[^\\w\\u4e00-\\u9fff-]", "_");
        String date = Instant.now().toString().substring(0, 10);
        String filename = "lineups-" + teamName + "-" + date + ".json";
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .header("Content-Type", "application/json; charset=UTF-8")
                .body(envelope);
    }

    @PostMapping("/api/teams/{teamId}/lineups/import")
    public ResponseEntity<Map<String, Integer>> importLineups(
            @PathVariable String teamId,
            @RequestParam("file") MultipartFile file) throws IOException {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        JsonNode root;
        try {
            root = objectMapper.readTree(content);
        } catch (Exception e) {
            throw new IllegalArgumentException("文件格式非法：不是有效的 JSON");
        }
        JsonNode lineupsNode = root.get("lineups");
        if (lineupsNode == null || !lineupsNode.isArray()) {
            throw new IllegalArgumentException("文件格式非法：缺少 lineups 数组");
        }
        List<Lineup> lineups = new ArrayList<>();
        for (JsonNode node : lineupsNode) {
            try {
                lineups.add(objectMapper.treeToValue(node, Lineup.class));
            } catch (Exception ignored) {
            }
        }
        Map<String, Integer> result = lineupService.importLineups(teamId, lineups);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/api/teams/{teamId}/lineups/{lineupId}")
    public ResponseEntity<com.tennis.model.Lineup> updateLineup(
            @PathVariable String teamId,
            @PathVariable String lineupId,
            @RequestBody LineupUpdateRequest req) {
        com.tennis.model.Lineup updated = lineupService.updateLineup(teamId, lineupId, req);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/api/lineups/{id}")
    public ResponseEntity<Void> deleteLineup(@PathVariable String id) {
        lineupService.deleteLineup(id);
        return ResponseEntity.noContent().build();
    }
}
