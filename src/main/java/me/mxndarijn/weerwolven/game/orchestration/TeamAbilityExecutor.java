// me.mxndarijn.weerwolven.game.orchestration.TeamAbilityExecutor
package me.mxndarijn.weerwolven.game.orchestration;

import me.mxndarijn.weerwolven.game.action.ActionIntent;
import me.mxndarijn.weerwolven.game.core.Game;
import me.mxndarijn.weerwolven.game.core.GamePlayer;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Executes a team/aggregated ability (e.g., Wolves voting together). */
public interface TeamAbilityExecutor {
    CompletableFuture<List<ActionIntent>> executeTeam(Game game, List<GamePlayer> actors, long timeoutMs);
}
