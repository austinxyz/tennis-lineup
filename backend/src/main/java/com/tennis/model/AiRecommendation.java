package com.tennis.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiRecommendation {

    @JsonProperty("lineup")
    private Lineup lineup;

    @JsonProperty("explanation")
    private String explanation;

    @JsonProperty("aiUsed")
    private boolean aiUsed;
}
