package com.tennis.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class OpponentAnalysisRequest {

    @JsonProperty("teamId")
    private String teamId;

    @JsonProperty("opponentTeamId")
    private String opponentTeamId;

    @JsonProperty("opponentLineupId")
    private String opponentLineupId;

    @JsonProperty("strategyType")
    private String strategyType;

    @JsonProperty("naturalLanguage")
    private String naturalLanguage;

    @JsonProperty("includePlayers")
    private List<String> includePlayers;

    @JsonProperty("excludePlayers")
    private List<String> excludePlayers;

    @JsonProperty("pinPlayers")
    private Map<String, String> pinPlayers;

    @JsonProperty("includeAi")
    private boolean includeAi = false;
}
