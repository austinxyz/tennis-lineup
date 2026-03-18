package com.tennis.controller;

import com.tennis.model.Lineup;
import com.tennis.service.LineupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class LineupController {

    private final LineupService lineupService;

    @Autowired
    public LineupController(LineupService lineupService) {
        this.lineupService = lineupService;
    }

    @PostMapping("/api/lineups/generate")
    public ResponseEntity<Lineup> generateLineup(@RequestBody GenerateLineupRequest request) {
        Lineup lineup = lineupService.generateAndSave(
                request.getTeamId(),
                request.getStrategyType(),
                request.getPreset(),
                request.getNaturalLanguage()
        );
        return ResponseEntity.ok(lineup);
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
