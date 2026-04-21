package com.tennis.service;

import com.tennis.exception.NotFoundException;
import com.tennis.exception.TeamNotEmptyException;
import com.tennis.model.Player;
import com.tennis.model.Team;
import com.tennis.model.TeamData;
import com.tennis.repository.JsonRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TeamService {
    private final JsonRepository jsonRepository;

    @Autowired
    public TeamService(JsonRepository jsonRepository) {
        this.jsonRepository = jsonRepository;
    }

    public Team createTeam(String name) {
        // Validate team name first (before hitting the database)
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("队名不能为空");
        }
        if (name.length() > 50) {
            throw new IllegalArgumentException("队名不能超过50个字符");
        }

        TeamData teamData = jsonRepository.readData();

        // Check for duplicate team name
        if (teamData.getTeams().stream().anyMatch(team -> team.getName().equals(name))) {
            throw new IllegalArgumentException("队名已存在");
        }

        Team newTeam = new Team();
        newTeam.setId(generateTeamId());
        newTeam.setName(name.trim());
        newTeam.setCreatedAt(Instant.now());
        newTeam.setPlayers(List.of());
        newTeam.setLineups(List.of());

        teamData.getTeams().add(newTeam);
        jsonRepository.writeData(teamData);

        log.info("Created team: {}", newTeam.getId());
        return newTeam;
    }

    public List<Team> getAllTeams() {
        TeamData teamData = jsonRepository.readData();
        return teamData.getTeams().stream()
                .sorted((t1, t2) -> t2.getCreatedAt().compareTo(t1.getCreatedAt()))
                .collect(Collectors.toList());
    }

    public Team getTeamById(String teamId) {
        TeamData teamData = jsonRepository.readData();
        return teamData.getTeams().stream()
                .filter(team -> team.getId().equals(teamId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("队伍不存在"));
    }

    public Team updateTeamName(String teamId, String newName) {
        TeamData teamData = jsonRepository.readData();

        Team teamToUpdate = teamData.getTeams().stream()
                .filter(team -> team.getId().equals(teamId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("队伍不存在"));

        // Check for duplicate team name (excluding current team)
        if (teamData.getTeams().stream()
                .filter(team -> !team.getId().equals(teamId))
                .anyMatch(team -> team.getName().equals(newName))) {
            throw new IllegalArgumentException("队名已存在");
        }

        // Validate new name
        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("队名不能为空");
        }
        if (newName.length() > 50) {
            throw new IllegalArgumentException("队名不能超过50个字符");
        }

        teamToUpdate.setName(newName.trim());
        jsonRepository.writeData(teamData);

        log.info("Updated team name: {} -> {}", teamId, newName);
        return teamToUpdate;
    }

    public void deleteTeam(String teamId) {
        TeamData teamData = jsonRepository.readData();

        Team target = teamData.getTeams().stream()
                .filter(team -> team.getId().equals(teamId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("队伍不存在"));

        int playerCount = target.getPlayers() == null ? 0 : target.getPlayers().size();
        int lineupCount = target.getLineups() == null ? 0 : target.getLineups().size();
        if (playerCount > 0 || lineupCount > 0) {
            throw new TeamNotEmptyException(playerCount, lineupCount);
        }

        teamData.getTeams().remove(target);
        jsonRepository.writeData(teamData);
        log.info("Deleted team: {}", teamId);
    }

    private String generateTeamId() {
        return "team-" + System.nanoTime();
    }

    private String generatePlayerId() {
        return "player-" + System.nanoTime();
    }

    public Player addPlayer(String teamId, String name, String gender, Double utr, Double verifiedDoublesUtr, Boolean verified, String profileUrl, String notes, Double actualUtr) {
        TeamData teamData = jsonRepository.readData();

        Team team = teamData.getTeams().stream()
                .filter(t -> t.getId().equals(teamId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("队伍不存在"));

        // Check for duplicate player name
        if (team.getPlayers().stream().anyMatch(player -> player.getName().equals(name))) {
            throw new IllegalArgumentException("球员姓名已存在");
        }

        // Validate player data
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("球员姓名不能为空");
        }
        if (name.length() > 50) {
            throw new IllegalArgumentException("球员姓名不能超过50个字符");
        }
        if (gender == null || (!gender.equals("male") && !gender.equals("female"))) {
            throw new IllegalArgumentException("性别必须是 male 或 female");
        }
        if (utr == null || utr < 0 || utr > 16) {
            throw new IllegalArgumentException("UTR 必须在 0-16 之间");
        }

        Player newPlayer = new Player();
        newPlayer.setId(generatePlayerId());
        newPlayer.setName(name.trim());
        newPlayer.setGender(gender);
        newPlayer.setUtr(utr);
        newPlayer.setVerifiedDoublesUtr(verifiedDoublesUtr);
        newPlayer.setVerified(verified);
        newPlayer.setProfileUrl(profileUrl != null && profileUrl.isBlank() ? null : profileUrl);
        newPlayer.setNotes(notes != null && notes.isBlank() ? null : notes);
        if (actualUtr != null && (actualUtr < 0.0 || actualUtr > 16.0)) {
            throw new IllegalArgumentException("实际UTR必须在0.0到16.0之间");
        }
        newPlayer.setActualUtr(actualUtr);

        team.getPlayers().add(newPlayer);
        jsonRepository.writeData(teamData);

        log.info("Added player {} to team {}", newPlayer.getId(), teamId);
        return newPlayer;
    }

    public Player updatePlayer(String teamId, String playerId, String name, String gender, Double utr, Double verifiedDoublesUtr, Boolean verified, String profileUrl, String notes, Double actualUtr) {
        TeamData teamData = jsonRepository.readData();

        Team team = teamData.getTeams().stream()
                .filter(t -> t.getId().equals(teamId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("队伍不存在"));

        Player playerToUpdate = team.getPlayers().stream()
                .filter(p -> p.getId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("球员不存在"));

        // Use existing values for fields not provided (partial update support)
        String effectiveName = (name != null) ? name : playerToUpdate.getName();
        String effectiveGender = (gender != null) ? gender : playerToUpdate.getGender();
        Double effectiveUtr = (utr != null) ? utr : playerToUpdate.getUtr();

        // Check for duplicate player name (excluding current player)
        if (team.getPlayers().stream()
                .filter(p -> !p.getId().equals(playerId))
                .anyMatch(p -> p.getName().equals(effectiveName))) {
            throw new IllegalArgumentException("球员姓名已存在");
        }

        // Validate player data
        if (effectiveName == null || effectiveName.trim().isEmpty()) {
            throw new IllegalArgumentException("球员姓名不能为空");
        }
        if (effectiveName.length() > 50) {
            throw new IllegalArgumentException("球员姓名不能超过50个字符");
        }
        if (!effectiveGender.equals("male") && !effectiveGender.equals("female")) {
            throw new IllegalArgumentException("性别必须是 male 或 female");
        }
        if (effectiveUtr < 0 || effectiveUtr > 16) {
            throw new IllegalArgumentException("UTR 必须在 0-16 之间");
        }

        playerToUpdate.setName(effectiveName.trim());
        playerToUpdate.setGender(effectiveGender);
        playerToUpdate.setUtr(effectiveUtr);
        if (verifiedDoublesUtr != null) playerToUpdate.setVerifiedDoublesUtr(verifiedDoublesUtr);
        if (verified != null) playerToUpdate.setVerified(verified);
        if (profileUrl != null) playerToUpdate.setProfileUrl(profileUrl.isBlank() ? null : profileUrl);
        if (notes != null) playerToUpdate.setNotes(notes.isBlank() ? null : notes);
        if (actualUtr != null && (actualUtr < 0.0 || actualUtr > 16.0)) {
            throw new IllegalArgumentException("实际UTR必须在0.0到16.0之间");
        }
        playerToUpdate.setActualUtr(actualUtr);

        jsonRepository.writeData(teamData);

        log.info("Updated player {} in team {}", playerId, teamId);
        return playerToUpdate;
    }

    public void deletePlayer(String teamId, String playerId) {
        TeamData teamData = jsonRepository.readData();

        Team team = teamData.getTeams().stream()
                .filter(t -> t.getId().equals(teamId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("队伍不存在"));

        boolean removed = team.getPlayers().removeIf(p -> p.getId().equals(playerId));
        if (!removed) {
            throw new IllegalArgumentException("球员不存在");
        }

        jsonRepository.writeData(teamData);
        log.info("Deleted player {} from team {}", playerId, teamId);
    }
}