package com.tennis.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LineupMatchupResponse {

    @JsonProperty("results")
    private List<MatchupResult> results;

    @JsonProperty("aiRecommendation")
    private AiRecommendation aiRecommendation;
}
