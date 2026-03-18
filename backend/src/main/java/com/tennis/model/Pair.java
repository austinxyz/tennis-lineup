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
}
