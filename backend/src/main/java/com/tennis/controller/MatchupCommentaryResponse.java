package com.tennis.controller;

import java.util.List;

public class MatchupCommentaryResponse {
    private List<LineCommentary> lines;
    private boolean aiUsed;

    public MatchupCommentaryResponse(List<LineCommentary> lines, boolean aiUsed) {
        this.lines = lines;
        this.aiUsed = aiUsed;
    }

    public List<LineCommentary> getLines() { return lines; }
    public boolean isAiUsed() { return aiUsed; }

    public static class LineCommentary {
        private final String position;
        private final String commentary;

        public LineCommentary(String position, String commentary) {
            this.position = position;
            this.commentary = commentary;
        }

        public String getPosition() { return position; }
        public String getCommentary() { return commentary; }
    }
}
