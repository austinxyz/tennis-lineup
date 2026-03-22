package com.tennis.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpponentAnalysisResponse {

    @JsonProperty("utrRecommendation")
    private UtrRecommendation utrRecommendation;

    @JsonProperty("aiRecommendation")
    private AiRecommendation aiRecommendation;
}
