package me.mxndarijn.weerwolven.game.bus.events;

import me.mxndarijn.weerwolven.game.bus.GameBusEvent;
import me.mxndarijn.weerwolven.game.core.GamePlayer;

/**
 * @param source may be null
 *               An ingame player eliminated event in minecraft.
 */
public record PlayerEliminatedEvent(GamePlayer target, GamePlayer source, Cause cause) implements GameBusEvent {
    public enum Cause {
        NIGHT_ELIMINATION, POISON, VOTED_OUT, EXPLOSION_DETONATION, IGNITION,
        TRAP_TRIGGER, RANGED_ELIMINATION, LOVERS_LINK, OTHER
    }

    public PlayerEliminatedEvent(GamePlayer target, GamePlayer source, Cause cause) {
        this.target = target;
        this.source = source;
        this.cause = (cause == null ? Cause.OTHER : cause);
    }
}
