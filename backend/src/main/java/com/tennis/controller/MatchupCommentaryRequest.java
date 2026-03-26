package com.tennis.controller;

public class MatchupCommentaryRequest {
    private String teamId;
    private String ownLineupId;
    private String opponentTeamId;
    private String opponentLineupId;
    private java.util.List<LineupMatchupRequest.PartnerNoteDto> ownPartnerNotes;
    private java.util.List<LineupMatchupRequest.PartnerNoteDto> opponentPartnerNotes;

    public String getTeamId() { return teamId; }
    public void setTeamId(String teamId) { this.teamId = teamId; }

    public String getOwnLineupId() { return ownLineupId; }
    public void setOwnLineupId(String ownLineupId) { this.ownLineupId = ownLineupId; }

    public String getOpponentTeamId() { return opponentTeamId; }
    public void setOpponentTeamId(String opponentTeamId) { this.opponentTeamId = opponentTeamId; }

    public String getOpponentLineupId() { return opponentLineupId; }
    public void setOpponentLineupId(String opponentLineupId) { this.opponentLineupId = opponentLineupId; }

    public java.util.List<LineupMatchupRequest.PartnerNoteDto> getOwnPartnerNotes() { return ownPartnerNotes; }
    public void setOwnPartnerNotes(java.util.List<LineupMatchupRequest.PartnerNoteDto> ownPartnerNotes) { this.ownPartnerNotes = ownPartnerNotes; }

    public java.util.List<LineupMatchupRequest.PartnerNoteDto> getOpponentPartnerNotes() { return opponentPartnerNotes; }
    public void setOpponentPartnerNotes(java.util.List<LineupMatchupRequest.PartnerNoteDto> opponentPartnerNotes) { this.opponentPartnerNotes = opponentPartnerNotes; }
}
