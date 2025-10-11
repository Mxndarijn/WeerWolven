package me.mxndarijn.weerwolven.game.status;

import me.mxndarijn.weerwolven.game.Game;
import me.mxndarijn.weerwolven.game.Phase;
import me.mxndarijn.weerwolven.game.StatusKey;

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
