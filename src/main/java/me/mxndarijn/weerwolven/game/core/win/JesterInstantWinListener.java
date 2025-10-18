package me.mxndarijn.weerwolven.game.core.win;

import me.mxndarijn.weerwolven.data.Roles;
import me.mxndarijn.weerwolven.game.bus.events.PlayerEliminatedEvent;
import me.mxndarijn.weerwolven.game.bus.GameEventBus;
import me.mxndarijn.weerwolven.game.bus.Priority;
import me.mxndarijn.weerwolven.game.core.Game;
import me.mxndarijn.weerwolven.game.core.GamePlayer;

import java.util.List;
import java.util.UUID;

public final class JesterInstantWinListener {
    public static AutoCloseable subscribe(Game game, GameEventBus bus) {
        return bus.subscribe(PlayerEliminatedEvent.class, Priority.NORMAL, evt -> {
            GamePlayer target = evt.target();
            if (target.getRole() == Roles.JESTER && evt.cause() == PlayerEliminatedEvent.Cause.VOTED_OUT) {
                UUID winner = target.getOptionalPlayerUUID().orElse(null);
                WinResult result = new WinResult(
                        target.getRole().getTeam(),
                        winner == null ? List.of() : List.of(winner),
                        WinConditionText.JESTER
                );
                game.getGameEventBus().post(new me.mxndarijn.weerwolven.game.bus.events.GameWonEvent(result));
            }
        });
    }
}