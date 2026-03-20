package com.tennis.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class ConstraintPreset {
    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("createdAt")
    private Instant createdAt;

    @JsonProperty("excludePlayers")
    private List<String> excludePlayers = new ArrayList<>();

    @JsonProperty("includePlayers")
    private List<String> includePlayers = new ArrayList<>();

    @JsonProperty("pinPlayers")
    private Map<String, String> pinPlayers = new HashMap<>();
}
