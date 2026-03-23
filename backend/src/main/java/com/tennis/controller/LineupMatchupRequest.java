package com.tennis.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LineupMatchupRequest {

    @JsonProperty("teamId")
    private String teamId;

    @JsonProperty("opponentTeamId")
    private String opponentTeamId;

    @JsonProperty("opponentLineupId")
    private String opponentLineupId;

    @JsonProperty("ownLineupId")
    private String ownLineupId;

    @JsonProperty("includeAi")
    private boolean includeAi = false;
}
