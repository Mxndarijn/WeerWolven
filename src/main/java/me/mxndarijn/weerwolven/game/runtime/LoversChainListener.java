// me.mxndarijn.weerwolven.game.runtime.LoversChainListener
package me.mxndarijn.weerwolven.game.runtime;

import me.mxndarijn.weerwolven.game.Game;
import me.mxndarijn.weerwolven.game.StatusKey;
import me.mxndarijn.weerwolven.game.bus.AutoCloseableGroup;
import me.mxndarijn.weerwolven.game.bus.GameEventBus;
import me.mxndarijn.weerwolven.game.bus.Priority;
import me.mxndarijn.weerwolven.game.bus.events.PlayerEliminatedEvent;
import me.mxndarijn.weerwolven.game.status.LoverStatus;

/**
 *
 */
public final class LoversChainListener {
    private LoversChainListener() {}

    public static AutoCloseable subscribe(Game game, GameEventBus bus) {
        var group = new AutoCloseableGroup();
        group.add(bus.subscribe(PlayerEliminatedEvent.class, Priority.NORMAL, e -> {
            if (e.target() == null) return;

            var st = e.target().getStatusStore().getOne(StatusKey.LOVERS);
            if (st.isEmpty()) return;

            var partnerUuid = ((LoverStatus) st.get()).partner();
            if (partnerUuid == null) return;

            var partner = game.getGamePlayers().stream()
                    .filter(gp -> gp.getOptionalPlayerUUID().isPresent() && gp.getOptionalPlayerUUID().get().equals(partnerUuid))
                    .findFirst().orElse(null);
            if (partner == null || !partner.isAlive()) return;

            // Queue partner eliminated in aftermath
            game.getKillQueue().propose(partner, e.target(), PlayerEliminatedEvent.Cause.LOVERS_LINK);
        }));
        return group;
    }
}
