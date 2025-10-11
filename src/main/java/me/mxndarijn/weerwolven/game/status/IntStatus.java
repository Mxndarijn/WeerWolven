package me.mxndarijn.weerwolven.game.status;

import me.mxndarijn.weerwolven.game.Game;
import me.mxndarijn.weerwolven.game.Phase;
import me.mxndarijn.weerwolven.game.StatusKey;

import java.util.UUID;

/** Numerieke status (bv. +1 stem) – vaak stackable. */
public record IntStatus(
        StatusKey key,
        int amount,
        int expiresOnDay,
        Phase expiresAtPhase
) implements Status {
    @Override
    public UUID source() {
        return null;
    }

    @Override public boolean isExpired(Game g) {
        return g.getDayNumber() > expiresOnDay
                || (g.getDayNumber() == expiresOnDay && g.getPhase().isAfter(expiresAtPhase));
    }
}
