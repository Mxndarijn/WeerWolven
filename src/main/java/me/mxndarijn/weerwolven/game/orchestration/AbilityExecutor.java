package me.mxndarijn.weerwolven.game.orchestration;

import me.mxndarijn.weerwolven.game.action.ActionIntent;
import me.mxndarijn.weerwolven.game.core.Game;
import me.mxndarijn.weerwolven.game.core.GamePlayer;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/** executes an action, for example open menu or go outside */
public interface AbilityExecutor {
    CompletableFuture<List<ActionIntent>> execute(Game game, GamePlayer actor, long timeoutMs);
}
