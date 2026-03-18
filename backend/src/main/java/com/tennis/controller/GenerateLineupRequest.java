package com.tennis.controller;

public class GenerateLineupRequest {
    private String teamId;
    private String strategyType; // "preset" or "custom"
    private String preset;        // "balanced" or "aggressive" (when strategyType="preset")
    private String naturalLanguage; // (when strategyType="custom")

    public String getTeamId() { return teamId; }
    public void setTeamId(String teamId) { this.teamId = teamId; }
    public String getStrategyType() { return strategyType; }
    public void setStrategyType(String strategyType) { this.strategyType = strategyType; }
    public String getPreset() { return preset; }
    public void setPreset(String preset) { this.preset = preset; }
    public String getNaturalLanguage() { return naturalLanguage; }
    public void setNaturalLanguage(String naturalLanguage) { this.naturalLanguage = naturalLanguage; }
}
