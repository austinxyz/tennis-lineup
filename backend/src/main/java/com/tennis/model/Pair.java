package com.tennis.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pair {
    @JsonProperty("position")
    private String position; // "D1", "D2", "D3", "D4"

    @JsonProperty("player1Id")
    private String player1Id;

    @JsonProperty("player1Name")
    private String player1Name;

    @JsonProperty("player2Id")
    private String player2Id;

    @JsonProperty("player2Name")
    private String player2Name;

    @JsonProperty("combinedUtr")
    private Double combinedUtr;

    @JsonProperty("player1Utr")
    private Double player1Utr;

    @JsonProperty("player2Utr")
    private Double player2Utr;

    @JsonProperty("player1Gender")
    private String player1Gender; // "male" | "female"

    @JsonProperty("player2Gender")
    private String player2Gender; // "male" | "female"

    @JsonProperty("player1Notes")
    private String player1Notes; // optional player characteristics

    @JsonProperty("player2Notes")
    private String player2Notes; // optional player characteristics

    @JsonProperty("player1ActualUtr")
    private Double player1ActualUtr; // null if same as utr

    @JsonProperty("player2ActualUtr")
    private Double player2ActualUtr; // null if same as utr

    @JsonProperty("combinedActualUtr")
    private Double combinedActualUtr; // sum of actual UTRs (falls back to utr when actualUtr is null)
}
