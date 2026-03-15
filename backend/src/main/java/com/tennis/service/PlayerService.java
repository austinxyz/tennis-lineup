package com.tennis.service;

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
public class PlayerService {
    private final JsonRepository jsonRepository;

    @Autowired
    public PlayerService(JsonRepository jsonRepository) {
        this.jsonRepository = jsonRepository;
    }

    public Player addPlayer(String teamId, String name, String gender, Double utr, Double verifiedDoublesUtr, Boolean verified) {
        TeamData teamData = jsonRepository.readData();
        Team team = teamData.getTeams().stream()
                .filter(t -> t.getId().equals(teamId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("队伍不存在"));

        // Validate player data
        validatePlayerData(name, gender, utr);

        Player newPlayer = new Player();
        newPlayer.setId(generatePlayerId());
        newPlayer.setName(name.trim());
        newPlayer.setGender(gender.toLowerCase());
        newPlayer.setUtr(utr);
        newPlayer.setVerifiedDoublesUtr(verifiedDoublesUtr);
        newPlayer.setVerified(verified != null ? verified : false);

        team.getPlayers().add(newPlayer);
        jsonRepository.writeData(teamData);

        log.info("Added player {} to team {}", newPlayer.getId(), teamId);
        return newPlayer;
    }

    public Player updatePlayer(String teamId, String playerId, String name, String gender, Double utr, Double verifiedDoublesUtr, Boolean verified) {
        TeamData teamData = jsonRepository.readData();
        Team team = teamData.getTeams().stream()
                .filter(t -> t.getId().equals(teamId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("队伍不存在"));

        Player playerToUpdate = team.getPlayers().stream()
                .filter(player -> player.getId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("球员不存在"));

        // Validate player data
        validatePlayerData(name, gender, utr);

        playerToUpdate.setName(name.trim());
        playerToUpdate.setGender(gender.toLowerCase());
        playerToUpdate.setUtr(utr);
        playerToUpdate.setVerifiedDoublesUtr(verifiedDoublesUtr);
        playerToUpdate.setVerified(verified != null ? verified : false);

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

        boolean removed = team.getPlayers().removeIf(player -> player.getId().equals(playerId));
        if (!removed) {
            throw new IllegalArgumentException("球员不存在");
        }

        jsonRepository.writeData(teamData);
        log.info("Deleted player {} from team {}", playerId, teamId);
    }

    public List<Player> getPlayersByTeamId(String teamId) {
        TeamData teamData = jsonRepository.readData();
        Team team = teamData.getTeams().stream()
                .filter(t -> t.getId().equals(teamId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("队伍不存在"));

        return team.getPlayers();
    }

    private void validatePlayerData(String name, String gender, Double utr) {
        // Validate name
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("姓名不能为空");
        }

        // Validate gender
        if (gender == null || (!gender.equalsIgnoreCase("male") && !gender.equalsIgnoreCase("female"))) {
            throw new IllegalArgumentException("性别必须是male或female");
        }

        // Validate UTR range
        if (utr == null || utr < 0.0 || utr > 16.0) {
            throw new IllegalArgumentException("UTR必须在0.0到16.0之间");
        }
    }

    private String generatePlayerId() {
        return "player-" + System.nanoTime();
    }
}