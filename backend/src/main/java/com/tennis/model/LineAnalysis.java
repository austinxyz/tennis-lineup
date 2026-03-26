package com.tennis.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
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

    /** Sum of official (non-actual) UTRs for own pair — for display alongside ownCombinedUtr (actual). */
    @JsonProperty("ownCombinedRegularUtr")
    private Double ownCombinedRegularUtr;

    /** Sum of actual UTRs for opponent pair — for display and win probability computation. */
    @JsonProperty("opponentCombinedActualUtr")
    private Double opponentCombinedActualUtr;

    public LineAnalysis(String position, double ownCombinedUtr, double opponentCombinedUtr,
                        double delta, double winProbability, String label) {
        this.position = position;
        this.ownCombinedUtr = ownCombinedUtr;
        this.opponentCombinedUtr = opponentCombinedUtr;
        this.delta = delta;
        this.winProbability = winProbability;
        this.label = label;
    }
}
