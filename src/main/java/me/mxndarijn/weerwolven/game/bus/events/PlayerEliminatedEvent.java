// me.mxndarijn.weerwolven.game.bus.events.PlayerEliminatedEvent
package me.mxndarijn.weerwolven.game.bus.events;

import me.mxndarijn.weerwolven.game.bus.GameBusEvent;
import me.mxndarijn.weerwolven.game.core.GamePlayer;

public final class PlayerEliminatedEvent implements GameBusEvent {
    public enum Cause {
        NIGHT_ELIMINATION, POISON, VOTED_OUT, BOMB_DETONATION, IGNITION,
        TRAP_TRIGGER, RANGED_ELIMINATION, LOVERS_LINK, OTHER
    }
    private final GamePlayer target;
    private final GamePlayer source; // may be null
    private final Cause cause;

    public PlayerEliminatedEvent(GamePlayer target, GamePlayer source, Cause cause) {
        this.target = target;
        this.source = source;
        this.cause = (cause == null ? Cause.OTHER : cause);
    }
    public GamePlayer target() { return target; }
    public GamePlayer source() { return source; }
    public Cause cause() { return cause; }
}
