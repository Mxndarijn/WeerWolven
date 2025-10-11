package me.mxndarijn.weerwolven.data;

import lombok.Getter;

@Getter
public enum UpcomingGameStatus {
    WAITING("<yellow>Wachtend", true),
    CHOOSING_PLAYERS("<yellow>Spelers kiezen", true),
    PLAYING("<green>Bezig", false),
    FREEZE("<aqua>Freezed", false),
    FINISHED("<red>Afgelopen", false);

    private final String status;
    private final boolean canJoinQueue;

    UpcomingGameStatus(String status, boolean canJoinQueue) {
        this.status = status;
        this.canJoinQueue = canJoinQueue;
    }

    @Override
    public String toString() {
        return status;
    }

}