package com.tennis.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private String code;
    private String message;
    private Object details;

    public ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
        this.details = null;
    }
}