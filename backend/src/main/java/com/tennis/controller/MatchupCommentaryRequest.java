package com.tennis.controller;

public class MatchupCommentaryRequest {
    private String teamId;
    private String ownLineupId;
    private String opponentTeamId;
    private String opponentLineupId;

    public String getTeamId() { return teamId; }
    public void setTeamId(String teamId) { this.teamId = teamId; }

    public String getOwnLineupId() { return ownLineupId; }
    public void setOwnLineupId(String ownLineupId) { this.ownLineupId = ownLineupId; }

    public String getOpponentTeamId() { return opponentTeamId; }
    public void setOpponentTeamId(String opponentTeamId) { this.opponentTeamId = opponentTeamId; }

    public String getOpponentLineupId() { return opponentLineupId; }
    public void setOpponentLineupId(String opponentLineupId) { this.opponentLineupId = opponentLineupId; }
}
