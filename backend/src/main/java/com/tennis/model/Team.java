package com.tennis.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Team {
    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name; // max 50 chars

    @JsonProperty("createdAt")
    private Instant createdAt;

    @JsonProperty("players")
    private List<Player> players = new ArrayList<>();

    @JsonProperty("lineups")
    private List<Lineup> lineups = new ArrayList<>();

    @JsonProperty("partnerNotes")
    private List<PartnerNote> partnerNotes = new ArrayList<>();
}
