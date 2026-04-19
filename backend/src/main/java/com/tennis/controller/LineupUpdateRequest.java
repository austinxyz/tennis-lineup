package com.tennis.controller;

import com.tennis.model.Pair;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * PATCH request DTO for updating a saved lineup.
 * Null fields mean "do not update that field".
 */
@Data
@NoArgsConstructor
public class LineupUpdateRequest {

    /** New pairs list. Null means "keep existing pairs". */
    private List<Pair> pairs;

    /** Custom display name. Null means "keep existing label". */
    private String label;

    /** Free text note. Null means "keep existing comment". */
    private String comment;

    /** Display order (ascending). Null means "keep existing sortOrder". */
    private Integer sortOrder;
}
