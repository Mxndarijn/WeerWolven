// me.mxndarijn.weerwolven.game.combat.KillQueue
package me.mxndarijn.weerwolven.game.runtime;

import me.mxndarijn.weerwolven.game.Game;
import me.mxndarijn.weerwolven.game.GamePlayer;
import me.mxndarijn.weerwolven.game.bus.GameEventBus;
import me.mxndarijn.weerwolven.game.bus.events.PlayerEliminatedEvent;

import java.util.ArrayList;
import java.util.List;

public final class KillQueue {
    private final List<PendingKill> pending = new ArrayList<>();

    public void clear() { pending.clear(); }
    public boolean isEmpty() { return pending.isEmpty(); }
    public List<PendingKill> view() { return List.copyOf(pending); }

    public void propose(GamePlayer target, GamePlayer source, PlayerEliminatedEvent.Cause cause) {
        if (target == null || !target.isAlive()) return;
        pending.add(new PendingKill(target, source, cause == null ? PlayerEliminatedEvent.Cause.OTHER : cause));
    }

    /** Apply all queued kills, mark players dead, and post events. */
    public List<GamePlayer> commitAll(Game game, GameEventBus bus) {
        var died = new ArrayList<GamePlayer>();
        for (var k : new ArrayList<>(pending)) {
            var t = k.target();
            if (!t.isAlive()) continue; // already dead in this wave
            t.setAlive(false);
            died.add(t);
            bus.post(new PlayerEliminatedEvent(t, k.source(), k.cause()));
        }
        pending.clear();
        return died;
    }
}
