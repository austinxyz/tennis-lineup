package com.tennis.controller;

import com.tennis.model.Player;
import com.tennis.model.Team;
import com.tennis.service.BatchImportService;
import com.tennis.service.TeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/teams")
public class TeamController {
    private final TeamService teamService;
    private final BatchImportService batchImportService;

    @Autowired
    public TeamController(TeamService teamService, BatchImportService batchImportService) {
        this.teamService = teamService;
        this.batchImportService = batchImportService;
    }

    @GetMapping
    public ResponseEntity<List<Team>> getAllTeams() {
        List<Team> teams = teamService.getAllTeams();
        return ResponseEntity.ok(teams);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Team> getTeamById(@PathVariable String id) {
        Team team = teamService.getTeamById(id);
        return ResponseEntity.ok(team);
    }

    @GetMapping("/{id}/players")
    public ResponseEntity<List<Player>> getPlayersByTeamId(@PathVariable String id) {
        Team team = teamService.getTeamById(id);
        return ResponseEntity.ok(team.getPlayers());
    }

    @PostMapping
    public ResponseEntity<Team> createTeam(@RequestBody Team team) {
        Team createdTeam = teamService.createTeam(team.getName());
        return ResponseEntity.ok(createdTeam);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Team> updateTeamName(@PathVariable String id, @RequestBody Team team) {
        Team updatedTeam = teamService.updateTeamName(id, team.getName());
        return ResponseEntity.ok(updatedTeam);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTeam(@PathVariable String id) {
        teamService.deleteTeam(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/players")
    public ResponseEntity<Player> addPlayerToTeam(@PathVariable String id, @RequestBody PlayerRequest playerRequest) {
        Player player = teamService.addPlayer(id,
            playerRequest.getName(),
            playerRequest.getGender(),
            playerRequest.getUtr(),
            playerRequest.getVerifiedDoublesUtr(),
            playerRequest.getVerified(),
            playerRequest.getProfileUrl());
        return ResponseEntity.ok(player);
    }

    @PutMapping("/{id}/players/{playerId}")
    public ResponseEntity<Player> updatePlayer(@PathVariable String id, @PathVariable String playerId, @RequestBody PlayerRequest playerRequest) {
        Player player = teamService.updatePlayer(id, playerId,
            playerRequest.getName(),
            playerRequest.getGender(),
            playerRequest.getUtr(),
            playerRequest.getVerifiedDoublesUtr(),
            playerRequest.getVerified(),
            playerRequest.getProfileUrl());
        return ResponseEntity.ok(player);
    }

    @DeleteMapping("/{id}/players/{playerId}")
    public ResponseEntity<Void> deletePlayer(@PathVariable String id, @PathVariable String playerId) {
        teamService.deletePlayer(id, playerId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/import")
    public ResponseEntity<BatchImportService.ImportResult> importPlayers(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("请选择要导入的文件");
        }

        try {
            String content = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))
                    .lines()
                    .collect(java.util.stream.Collectors.joining("\n"));

            BatchImportService.ImportResult result;
            String fileName = file.getOriginalFilename().toLowerCase();

            if (fileName.endsWith(".csv")) {
                result = batchImportService.importFromCSV(content);
            } else if (fileName.endsWith(".json")) {
                result = batchImportService.importFromJSON(content);
            } else {
                throw new IllegalArgumentException("不支持的文件格式，请上传 CSV 或 JSON 文件");
            }

            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("文件导入失败: " + e.getMessage());
        }
    }
}