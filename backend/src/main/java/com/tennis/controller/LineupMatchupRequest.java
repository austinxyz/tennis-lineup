package com.tennis.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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

    /** Partner notes for own team to include in AI prompt (max 10). Each: {player1Name, player2Name, note} */
    @JsonProperty("ownPartnerNotes")
    private List<PartnerNoteDto> ownPartnerNotes;

    /** Partner notes for opponent team to include in AI prompt (max 10). */
    @JsonProperty("opponentPartnerNotes")
    private List<PartnerNoteDto> opponentPartnerNotes;

    @Data
    @NoArgsConstructor
    public static class PartnerNoteDto {
        @JsonProperty("player1Name")
        private String player1Name;
        @JsonProperty("player2Name")
        private String player2Name;
        @JsonProperty("note")
        private String note;
    }
}
