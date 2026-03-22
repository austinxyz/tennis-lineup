package com.tennis.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UtrRecommendation {

    @JsonProperty("lineup")
    private Lineup lineup;

    @JsonProperty("lineAnalysis")
    private List<LineAnalysis> lineAnalysis;

    @JsonProperty("expectedScore")
    private double expectedScore;

    @JsonProperty("opponentExpectedScore")
    private double opponentExpectedScore;
}
