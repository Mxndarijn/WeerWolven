// me.mxndarijn.weerwolven.game.orchestration.DefaultDecisionPolicy
package me.mxndarijn.weerwolven.game.orchestration;

import me.mxndarijn.weerwolven.data.ActionKind;
import me.mxndarijn.weerwolven.game.action.ActionIntent;
import me.mxndarijn.weerwolven.game.core.Game;
import me.mxndarijn.weerwolven.game.core.GamePlayer;

import java.util.List;

/** What to do if a player/team times out, cancels, or otherwise fails to submit. */
public interface DefaultDecisionPolicy {

    /** Solo actors timed out. Return zero or more fallback intents (usually zero = skip). */
    List<ActionIntent> decideSolo(Game game, GamePlayer actor, ActionKind kind);

    /**
     * Team timed out. Return zero or more fallback intents.
     * For aggregated kinds (e.g., TEAM_KILL), returning an empty list means “no-kill”.
     */
    List<ActionIntent> decideTeam(Game game, List<GamePlayer> actors, ActionKind kind);
}
