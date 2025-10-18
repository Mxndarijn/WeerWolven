package me.mxndarijn.weerwolven.game.core;

import me.mxndarijn.weerwolven.game.bus.GameEventBus;
import me.mxndarijn.weerwolven.game.bus.events.GameWonEvent;
import me.mxndarijn.weerwolven.game.core.Game;
import me.mxndarijn.weerwolven.game.core.win.WinCondition;
import me.mxndarijn.weerwolven.game.core.win.WinResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class WinCheckService {
    private final Game game;
    private final GameEventBus bus;
    private final List<WinCondition> conditions = new ArrayList<>();

    public WinCheckService(Game game, GameEventBus bus, List<WinCondition> initial) {
        this.game = game;
        this.bus = bus;
        this.conditions.addAll(initial);
        this.conditions.sort(Comparator.comparingInt(WinCondition::priority));
    }

    public Optional<WinResult> evaluateNow() {
        for (WinCondition c : conditions) {
            Optional<WinResult> r = c.evaluate(game);
            if (r.isPresent()) {
                bus.post(new GameWonEvent(r.get()));
                return r;
            }
        }
        return Optional.empty();
    }
}