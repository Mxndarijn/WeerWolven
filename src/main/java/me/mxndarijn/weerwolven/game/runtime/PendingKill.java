// me.mxndarijn.weerwolven.game.combat.PendingKill
package me.mxndarijn.weerwolven.game.runtime;

import me.mxndarijn.weerwolven.game.GamePlayer;
import me.mxndarijn.weerwolven.game.bus.events.PlayerEliminatedEvent;

public record PendingKill(
        GamePlayer target,
        GamePlayer source,                 // may be null
        PlayerEliminatedEvent.Cause cause
) {}
