package me.mxndarijn.weerwolven.game.status;

import me.mxndarijn.weerwolven.game.core.Game;
import me.mxndarijn.weerwolven.game.phase.Phase;

import java.util.UUID;

public record FlagStatus(
        StatusKey key,
        UUID source,           // mag null zijn
        int expiresOnDay,
        Phase expiresAtPhase
) implements Status {

    @Override
    public boolean isExpired(Game g) {
        return g.getDayNumber() > expiresOnDay
                || (g.getDayNumber() == expiresOnDay && g.getPhase().isAfter(expiresAtPhase));
    }
}
