package com.tennis.controller;

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

import java.util.List;

@RestController
public class LineupController {

    private final LineupService lineupService;
    private final OpponentAnalysisService opponentAnalysisService;
    private final LineupMatchupService lineupMatchupService;
    private final MatchupCommentaryService matchupCommentaryService;

    @Autowired
    public LineupController(LineupService lineupService,
                            OpponentAnalysisService opponentAnalysisService,
                            LineupMatchupService lineupMatchupService,
                            MatchupCommentaryService matchupCommentaryService) {
        this.lineupService = lineupService;
        this.opponentAnalysisService = opponentAnalysisService;
        this.lineupMatchupService = lineupMatchupService;
        this.matchupCommentaryService = matchupCommentaryService;
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

    @DeleteMapping("/api/lineups/{id}")
    public ResponseEntity<Void> deleteLineup(@PathVariable String id) {
        lineupService.deleteLineup(id);
        return ResponseEntity.noContent().build();
    }
}
