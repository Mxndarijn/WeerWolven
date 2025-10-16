package me.mxndarijn.weerwolven.game.orchestration;

import me.mxndarijn.weerwolven.data.ActionKind;
import me.mxndarijn.weerwolven.game.action.ActionIntent;
import me.mxndarijn.weerwolven.game.core.Game;
import me.mxndarijn.weerwolven.game.core.GamePlayer;

import java.util.List;

/** Default fallbacks: do nothing when timeouts occur. */
public final class SimpleDefaultDecisionPolicy implements DefaultDecisionPolicy {
    @Override
    public List<ActionIntent> decideSolo(Game game, GamePlayer actor, ActionKind kind) {
        return List.of();
    }

    @Override
    public List<ActionIntent> decideTeam(Game game, List<GamePlayer> actors, ActionKind kind) {
        return List.of();
    }
}
