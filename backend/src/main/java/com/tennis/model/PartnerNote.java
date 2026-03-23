package com.tennis.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PartnerNote {

    @JsonProperty("id")
    private String id;

    @JsonProperty("teamId")
    private String teamId;

    @JsonProperty("player1Id")
    private String player1Id;

    @JsonProperty("player2Id")
    private String player2Id;

    @JsonProperty("player1Name")
    private String player1Name;

    @JsonProperty("player2Name")
    private String player2Name;

    @JsonProperty("note")
    private String note;

    @JsonProperty("createdAt")
    private Instant createdAt;

    @JsonProperty("updatedAt")
    private Instant updatedAt;
}
