package com.tennis.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class ConfigData {
    // key = teamId, value = list of presets for that team
    @JsonProperty("constraintPresets")
    private Map<String, List<ConstraintPreset>> constraintPresets = new HashMap<>();
}
