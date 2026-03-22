package com.tennis.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LineAnalysis {

    @JsonProperty("position")
    private String position;

    @JsonProperty("ownCombinedUtr")
    private double ownCombinedUtr;

    @JsonProperty("opponentCombinedUtr")
    private double opponentCombinedUtr;

    @JsonProperty("delta")
    private double delta;

    @JsonProperty("winProbability")
    private double winProbability;

    @JsonProperty("label")
    private String label;
}
