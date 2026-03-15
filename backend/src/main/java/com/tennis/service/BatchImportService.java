package com.tennis.service;

import com.tennis.model.Player;
import com.tennis.model.TeamData;
import com.tennis.repository.JsonRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BatchImportService {
    private final JsonRepository jsonRepository;
    private final PlayerService playerService;

    @Autowired
    public BatchImportService(JsonRepository jsonRepository, PlayerService playerService) {
        this.jsonRepository = jsonRepository;
        this.playerService = playerService;
    }

    public ImportResult importFromCSV(String csvContent) {
        List<Player> players = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int lineNum = 0;

        try (BufferedReader reader = new BufferedReader(new StringReader(csvContent))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (lineNum == 1) continue; // Skip header row

                try {
                    Player player = parseCSVLine(line);
                    players.add(player);
                } catch (IllegalArgumentException e) {
                    errors.add(String.format("行 %d: %s", lineNum, e.getMessage()));
                }
            }
        } catch (IOException e) {
            log.error("Failed to read CSV content", e);
            throw new RuntimeException("Failed to read CSV content", e);
        }

        return importPlayers(players, errors);
    }

    public ImportResult importFromJSON(String jsonContent) {
        List<Player> players;
        List<String> errors = new ArrayList<>();

        try {
            players = parseJSONContent(jsonContent);
        } catch (Exception e) {
            errors.add("JSON解析错误: " + e.getMessage());
            return new ImportResult(0, 0, errors);
        }

        return importPlayers(players, errors);
    }

    private Player parseCSVLine(String line) {
        String[] fields = line.split(",");
        if (fields.length < 4) {
            throw new IllegalArgumentException("字段数量不足，需要: name,gender,utr,verified");
        }

        String name = fields[0].trim();
        String gender = fields[1].trim().toLowerCase();
        String utrStr = fields[2].trim();
        String verifiedStr = fields.length > 3 ? fields[3].trim() : "false";

        // Validate fields
        if (name.isEmpty()) {
            throw new IllegalArgumentException("姓名不能为空");
        }
        if (!gender.equals("male") && !gender.equals("female")) {
            throw new IllegalArgumentException("性别必须是male或female");
        }
        double utr;
        try {
            utr = Double.parseDouble(utrStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("UTR必须是有效的数字");
        }
        if (utr < 0.0 || utr > 16.0) {
            throw new IllegalArgumentException("UTR必须在0.0到16.0之间");
        }

        Player player = new Player();
        player.setName(name);
        player.setGender(gender);
        player.setUtr(utr);
        player.setVerified(Boolean.parseBoolean(verifiedStr));
        player.setVerifiedDoublesUtr(null); // Will be set by service

        return player;
    }

    private List<Player> parseJSONContent(String jsonContent) {
        // Simple JSON parsing - in production, use proper JSON library
        List<Player> players = new ArrayList<>();

        // Remove brackets and split by comma
        String content = jsonContent.trim();
        if (content.startsWith("[") && content.endsWith("]")) {
            content = content.substring(1, content.length() - 1);
        }

        if (content.isEmpty()) {
            return players;
        }

        String[] playerJsons = content.split("\\},\\s*\\{");
        for (String playerJson : playerJsons) {
            try {
                Player player = parseJSONPlayer(playerJson);
                players.add(player);
            } catch (Exception e) {
                throw new RuntimeException("Invalid player JSON: " + e.getMessage());
            }
        }

        return players;
    }

    private Player parseJSONPlayer(String playerJson) {
        Player player = new Player();

        // Simple field extraction
        String name = extractJsonValue(playerJson, "name");
        String gender = extractJsonValue(playerJson, "gender");
        String utrStr = extractJsonValue(playerJson, "utr");
        String verifiedStr = extractJsonValue(playerJson, "verified", "false");

        // Validate fields
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("姓名不能为空");
        }
        if (gender == null || (!gender.equals("male") && !gender.equals("female"))) {
            throw new IllegalArgumentException("性别必须是male或female");
        }
        double utr;
        try {
            utr = Double.parseDouble(utrStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("UTR必须是有效的数字");
        }
        if (utr < 0.0 || utr > 16.0) {
            throw new IllegalArgumentException("UTR必须在0.0到16.0之间");
        }

        player.setName(name);
        player.setGender(gender);
        player.setUtr(utr);
        player.setVerified(Boolean.parseBoolean(verifiedStr));
        player.setVerifiedDoublesUtr(null); // Will be set by service

        return player;
    }

    private String extractJsonValue(String json, String key) {
        return extractJsonValue(json, key, null);
    }

    private String extractJsonValue(String json, String key, String defaultValue) {
        // Try quoted string first: "key":"value"
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"" + key + "\":\"([^\"]*)\"");
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        // Try unquoted value (numbers, booleans): "key":value
        p = java.util.regex.Pattern.compile("\"" + key + "\":([^,}\\]]+)");
        m = p.matcher(json);
        if (m.find()) {
            return m.group(1).trim();
        }
        return defaultValue;
    }

    private ImportResult importPlayers(List<Player> players, List<String> initialErrors) {
        TeamData teamData = jsonRepository.readData();
        List<String> errors = new ArrayList<>(initialErrors);
        int successCount = 0;

        for (Player player : players) {
            try {
                // In a real implementation, you would assign players to teams
                // For now, we'll just add them to a default team or create a new team
                // This is a simplified implementation
                playerService.addPlayer("team-1", player.getName(), player.getGender(),
                                    player.getUtr(), null, player.getVerified());
                successCount++;
            } catch (Exception e) {
                errors.add("导入失败: " + e.getMessage());
            }
        }

        return new ImportResult(successCount, initialErrors.size() + (players.size() - successCount), errors);
    }

    public static class ImportResult {
        private final int successCount;
        private final int failureCount;
        private final List<String> errors;

        public ImportResult(int successCount, int failureCount, List<String> errors) {
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.errors = errors;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getFailureCount() {
            return failureCount;
        }

        public List<String> getErrors() {
            return errors;
        }
    }
}