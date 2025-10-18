package me.mxndarijn.weerwolven.game.action;

import me.mxndarijn.weerwolven.data.ActionKind;
import me.mxndarijn.weerwolven.data.Timing;
import me.mxndarijn.weerwolven.game.core.GamePlayer;

import java.util.List;
import java.util.Map;


/**
 * Represents a declared action intent from one or more actors to be executed during a specific game phase.
 * <p>
 * ActionIntents are collected during orchestration, sorted by timing and initiative, and then resolved
 * by the {@link me.mxndarijn.weerwolven.game.phase.PhaseExecutor}. Each intent captures who performs
 * the action, what kind of action it is, when it should execute, and any action-specific parameters.
 *
 * @param actors      the list of {@link GamePlayer}s performing this action; typically a single player for
 *                   solo abilities (e.g., Seer inspect) or multiple players for team abilities (e.g., werewolf eliminate)
 * @param kind       the {@link ActionKind} categorizing this action (e.g., INSPECT, JAIL, PROTECT, ELIMINATE);
 *                   used to route the intent to the appropriate {@link me.mxndarijn.weerwolven.game.action.ActionHandler}
 * @param timing     the {@link Timing} indicating when this action should be resolved in the game flow
 *                   (NIGHT, DAWN, DAY, DUSK, or REACTION)
 * @param params     a flexible map holding action-specific parameters such as {@code "target"} (single {@link GamePlayer})
 *                   or {@code "targets"} (List of {@link GamePlayer}); keys and value types vary by action kind
 * @param initiative the execution priority within the same timing phase; lower values execute first,
 *                   allowing fine-grained ordering of actions (e.g., protect before eliminate)
 */
public record ActionIntent(
        List<GamePlayer> actors,
        ActionKind kind,
        Timing timing,
        Map<String,Object> params,
        int initiative
) {}
