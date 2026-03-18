package com.tennis.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GenerateLineupRequest {
    private String teamId;
    private String strategyType; // "preset" or "custom"
    private String preset;        // "balanced" or "aggressive" (when strategyType="preset")
    private String naturalLanguage; // (when strategyType="custom")
    private List<String> includePlayers = new ArrayList<>(); // player IDs that must appear
    private List<String> excludePlayers = new ArrayList<>(); // player IDs to exclude
    private Map<String, String> pinPlayers = new HashMap<>(); // playerId → "D1"|"D2"|"D3"|"D4"

    public String getTeamId() { return teamId; }
    public void setTeamId(String teamId) { this.teamId = teamId; }
    public String getStrategyType() { return strategyType; }
    public void setStrategyType(String strategyType) { this.strategyType = strategyType; }
    public String getPreset() { return preset; }
    public void setPreset(String preset) { this.preset = preset; }
    public String getNaturalLanguage() { return naturalLanguage; }
    public void setNaturalLanguage(String naturalLanguage) { this.naturalLanguage = naturalLanguage; }
    public List<String> getIncludePlayers() { return includePlayers; }
    public void setIncludePlayers(List<String> includePlayers) { this.includePlayers = includePlayers != null ? includePlayers : new ArrayList<>(); }
    public List<String> getExcludePlayers() { return excludePlayers; }
    public void setExcludePlayers(List<String> excludePlayers) { this.excludePlayers = excludePlayers != null ? excludePlayers : new ArrayList<>(); }
    public Map<String, String> getPinPlayers() { return pinPlayers; }
    public void setPinPlayers(Map<String, String> pinPlayers) { this.pinPlayers = pinPlayers != null ? pinPlayers : new HashMap<>(); }
}
