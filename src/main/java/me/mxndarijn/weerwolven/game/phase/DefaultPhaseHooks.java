// me.mxndarijn.weerwolven.game.DefaultPhaseHooks
package me.mxndarijn.weerwolven.game.phase;

import me.mxndarijn.weerwolven.game.bus.GameEventBus;
import me.mxndarijn.weerwolven.game.bus.events.AttackIncomingEvent;
import me.mxndarijn.weerwolven.game.core.Game;
import me.mxndarijn.weerwolven.game.core.GamePlayer;
import me.mxndarijn.weerwolven.game.runtime.EliminateQueue;
import me.mxndarijn.weerwolven.game.runtime.PendingKill;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class DefaultPhaseHooks implements PhaseHooks {

    @Override
    public int seatIndex(Game game, GamePlayer seat) {
        // Deterministic, stable order based on seat position
        return game.getGamePlayers().indexOf(seat);
    }

    @Override
    public void applyPreventsAndSaves(Game game, GameEventBus bus) {
        EliminateQueue queue = game.getEliminateQueue(); // you exposed getKillQueue() earlier
        if (queue.isEmpty()) return;

        // Take a snapshot of pending, then rebuild the queue with only allowed hits.
        var snapshot = new ArrayList<>(queue.view());
        queue.clear();

        for (PendingKill pk : snapshot) {
            GamePlayer target = pk.target();
            GamePlayer source = pk.source();

            // Skip already-dead or absent targets
            if (target == null || !target.isAlive()) continue;

            // --- PREVENTION HOOK via EventBus ---
            // Your AttackIncomingEvent should be Cancellable and optionally support redirection.
            // If your API differs, adjust this block accordingly.
            var evt = new AttackIncomingEvent(source, target, pk.cause());
            bus.post(evt);

            if (evt.isCancelled()) {
                // Blocked by Doctor/Angel/Bodyguard/Trap/etc.
                continue;
            }

            GamePlayer finalTarget = evt.getRedirectTarget() != null ? evt.getRedirectTarget() : target;

            // Re-queue the (possibly redirected) hit to be committed later.
            queue.propose(finalTarget, source, pk.cause());
        }
    }

    @Override
    public List<java.util.UUID> applyAllQueuedKills(Game game, GameEventBus bus) {
        var died = game.getEliminateQueue().commitAll(game, bus); // posts PlayerDiedEvent for each
        return toUuidList(died);
    }

    @Override
    public void runAftermath(Game game, GameEventBus bus, List<java.util.UUID> justDied) {
        // Typical deathrattles (Hunter/Wreker/Junior) and chain effects (Lovers) are implemented
        // as bus listeners that may propose more kills into the queue during PlayerDiedEvent handling.

        // Keep committing proposed kills until stable.
        int safety = 0;
        while (!game.getEliminateQueue().isEmpty() && safety++ < 100) {
            game.getEliminateQueue().commitAll(game, bus);
        }
        // If you want to detect runaway loops, log when safety is hit.
    }

    @Override
    public void cleanupStatuses(Game game) {
        for (GamePlayer gp : game.getGamePlayers()) {
            gp.getStatusStore().cleanupExpired(game);
        }
    }

    // --- helpers ---
    private static List<java.util.UUID> toUuidList(List<GamePlayer> players) {
        var out = new ArrayList<java.util.UUID>(players.size());
        for (GamePlayer gp : players) {
            Optional<java.util.UUID> id = gp.getOptionalPlayerUUID();
            id.ifPresent(out::add);
        }
        return out;
    }
}
