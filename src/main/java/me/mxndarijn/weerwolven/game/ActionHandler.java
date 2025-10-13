package me.mxndarijn.weerwolven.game;

import me.mxndarijn.weerwolven.data.ActionKind;
import me.mxndarijn.weerwolven.game.Game;
import me.mxndarijn.weerwolven.game.bus.GameEventBus;

import java.util.List;

public interface ActionHandler {
    ActionKind kind();
    ResolveMode mode();

    /** For SERIAL kinds (executor sorts intents already). */
    default void resolveSerial(List<ActionIntent> intents, Game game, GameEventBus bus) {}

    /** For TEAM_AGGREGATED kinds (whole batch at once). */
    default void resolveAggregated(List<ActionIntent> intents, Game game, GameEventBus bus) {}
}
