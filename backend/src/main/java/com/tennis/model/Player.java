package com.tennis.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Player {
    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("gender")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private String gender; // "male" or "female"

    @JsonProperty("utr")
    private Double utr; // 0.0 - 16.0

    @JsonProperty("verifiedDoublesUtr")
    private Double verifiedDoublesUtr; // null if not verified

    @JsonProperty("verified")
    private Boolean verified; // has 100% Verified Doubles UTR

    @JsonProperty("profileUrl")
    private String profileUrl; // UTR profile page URL, optional
}
