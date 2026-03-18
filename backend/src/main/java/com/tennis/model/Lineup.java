package com.tennis.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Lineup {
    @JsonProperty("id")
    private String id;

    @JsonProperty("createdAt")
    private Instant createdAt;

    @JsonProperty("strategy")
    private String strategy; // "balanced", "aggressive", "custom"

    @JsonProperty("aiUsed")
    private boolean aiUsed;

    @JsonProperty("pairs")
    private List<Pair> pairs = new ArrayList<>();

    @JsonProperty("totalUtr")
    private Double totalUtr;

    @JsonProperty("valid")
    private boolean valid;

    @JsonProperty("violationMessages")
    private List<String> violationMessages = new ArrayList<>();
}
