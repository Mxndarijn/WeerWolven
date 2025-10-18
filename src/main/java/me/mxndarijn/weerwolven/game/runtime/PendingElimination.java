// me.mxndarijn.weerwolven.game.combat.PendingKill
package me.mxndarijn.weerwolven.game.runtime;

import me.mxndarijn.weerwolven.game.bus.events.PlayerEliminatedEvent;
import me.mxndarijn.weerwolven.game.core.GamePlayer;

public record PendingElimination(
        GamePlayer target,
        GamePlayer source,                 // may be null
        PlayerEliminatedEvent.Cause cause
) {}
