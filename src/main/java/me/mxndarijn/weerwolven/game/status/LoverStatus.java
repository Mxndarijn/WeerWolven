// me.mxndarijn.weerwolven.game.status.LoverStatus
package me.mxndarijn.weerwolven.game.status;


import me.mxndarijn.weerwolven.game.core.Game;

import java.util.UUID;

/** Persistent “bond” with partner. */
public record LoverStatus(UUID partner) implements Status {
    @Override public StatusKey key() { return StatusKey.LOVERS; }

    @Override
    public UUID source() {
        return null;
    }

    @Override public boolean isExpired(Game game) { return false; }
    @Override public boolean stackable() { return false; }
}
