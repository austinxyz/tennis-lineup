package com.tennis.exception;

public class TeamNotEmptyException extends RuntimeException {
    private final int playerCount;
    private final int lineupCount;

    public TeamNotEmptyException(int playerCount, int lineupCount) {
        super("队伍中还有球员或已保存的排阵，无法删除");
        this.playerCount = playerCount;
        this.lineupCount = lineupCount;
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public int getLineupCount() {
        return lineupCount;
    }
}
